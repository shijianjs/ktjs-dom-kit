package io.github.shijianjs.ktjs.dom.loadjs

import io.github.shijianjs.ktjs.dom.element.invoke
import kotlin.js.js
import web.dom.document
import web.events.ERROR
import web.events.Event
import web.events.EventHandler
import web.events.LOAD
import web.events.addEventListener
import web.html.HTMLScriptElement
import web.html.HTMLElement
import web.html.HtmlTagName
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * One bundle: the file it loads, the element that file arrives in, and the bookkeeping around that
 * load — how it ended, and who waits for it.
 *
 * Everything that has to do with *one* url lives here; only the page-wide part — the id-to-bundle
 * table and the factory — stays in [Loadjs]. One of these is the whole state of a bundle, which is
 * why `ready` / `done` are things a bundle does instead of functions on a registry: a bundle outlives
 * the call that started it, so a late `ready` has to ask the object that kept the outcome.
 *
 * A file that belongs to no bundle is this same object with no id: it loads, it answers, and nothing
 * else can reach it.
 */
class LoadFile internal constructor(
    /** The id this bundle is filed under; `null` for a one-shot load nothing can wait on. */
    internal val bundleId: String?,
) {

    /** The path of the current load, as written — kind prefix and query included. */
    private lateinit var path: String

    /** Extra attempts a failed load gets. */
    private var numRetries = 0

    /** The insertion hook. */
    private var before: BeforeLoad? = null

    /**
     * Which element this path asks for — decided once, from the path, on the first attempt.
     *
     * The regexes are asked in one place: the query and fragment are cut first (so `foo.css?v=1` is
     * still CSS), a `css!` / `img!` prefix forces a kind, and `module!` / `nomodule!` resolve to the
     * half of the pair this browser can use — or to [Kind.Skipped] when neither applies.
     */
    private val kind: Kind by lazy {
        when {
            CSS_PATH.containsMatchIn(pathname) -> Kind.Stylesheet
            IMAGE_PATH.containsMatchIn(pathname) -> Kind.Image
            pathname.startsWith(MODULE_PREFIX) -> if (supportsModules) Kind.Module else Kind.Skipped
            pathname.startsWith(NOMODULE_PREFIX) -> if (supportsModules) Kind.Skipped else Kind.Script
            else -> Kind.Script
        }
    }

    /** The element of the current attempt: made by [createElement], settled by [awaitSettled]. */
    private var element: HTMLElement? = null

    /** How the load ended: unset while it has not, then the success or the failure. */
    private var outcome: Result<Unit>? = null

    /** Calls in [ready] that are still waiting for this bundle. */
    private val waiting = mutableListOf<Continuation<Unit>>()

    /**
     * Whether a load has been started for this bundle — `Loadjs.isDefined` answers from it.
     * `ready` / `done` alone only mention a bundle, they do not load one.
     */
    var isDefined = false
        private set

    /** The path without its query and fragment: the kind is decided by this one. */
    private val pathname: String get() = path.replace(QUERY_OR_FRAGMENT, "")

    /** What the element gets as its URL: the path with the `css!` / `img!` / `module!` prefix gone. */
    private val url: String get() = path.replace(LOAD_KIND_PREFIX, "")

    /**
     * Loads [path] into `<head>` and returns once the browser has it, or throws [LoadFailedException]
     * if it never arrived — after [numRetries] extra attempts.
     *
     * A path this browser is not meant to load (`module!` without module support, `nomodule!` with it)
     * counts as loaded and returns without an element. A failed attempt is retried with a fresh element
     * — the failed ones stay in the document, as they do upstream.
     *
     * A bundle loads once: a second call on a bundle that already loaded throws
     * [BundleAlreadyDefinedException], and the id is free again after `Loadjs.reset()`.
     */
    suspend fun load(path: String, numRetries: Int = 0, before: BeforeLoad? = null) {
        bundleId?.let {
            if (isDefined) throw BundleAlreadyDefinedException(it)
            isDefined = true
        }

        this.path = path
        this.numRetries = numRetries
        this.before = before

        try {
            loadAttempts()
        } catch (failure: LoadFailedException) {
            finish(Result.failure(failure))
            throw failure
        }
        finish(Result.success(Unit))
    }

    /**
     * Waits for this bundle and returns once it has arrived.
     *
     * A finished bundle answers at once from [outcome] — failure included, so a late `ready` still
     * throws. One that has not finished registers its continuation here, and the load (or [done])
     * resumes it. A `ready` made before the load even starts is resumed by that load.
     *
     * @throws LoadFailedException if the load this bundle waited for failed.
     */
    suspend fun ready() {
        outcome?.let {
            it.getOrThrow()
            return
        }
        suspendCoroutine { continuation -> waiting += continuation }
    }

    /**
     * Satisfies this bundle by hand, as if its file had arrived: a [ready] waiting on it returns now,
     * and later ones return at once.
     *
     * For a bundle whose loading someone else does — a framework's own loader, an inline script — so
     * the rest of the page can wait on it like any other.
     */
    fun done() {
        finish(Result.success(Unit))
    }

    /** Records how the load ended and hands it to whoever waits — the only place [outcome] is set. */
    private fun finish(outcome: Result<Unit>) {
        this.outcome = outcome

        // The list is copied before resuming: the waiting calls run their own code, which may come
        // back to this bundle.
        val waiters = waiting.toList()
        waiting.clear()
        waiters.forEach { waiter ->
            outcome.fold({ waiter.resume(Unit) }, { waiter.resumeWithException(it) })
        }
    }

    /** The retry loop: one element per attempt, and a skip counts as a load. */
    private suspend fun loadAttempts() {
        repeat(numRetries + 1) {
            element = createElement() ?: return
            if (awaitSettled()) return
        }
        throw LoadFailedException(path)
    }

    /**
     * Makes the element of the current attempt, or `null` when this browser is the wrong one.
     *
     * The tag is the factory (`HtmlTagName.link { … }`, `dom.element`'s DSL), so each block sees its
     * own element type and no cast is needed.
     */
    private fun createElement(): HTMLElement? = when (kind) {
        Kind.Skipped -> null

        Kind.Stylesheet -> HtmlTagName.link {
            rel = "stylesheet"
            href = url
        }

        Kind.Image -> HtmlTagName.img { src = url }

        Kind.Script -> HtmlTagName.script { src = url }

        Kind.Module -> HtmlTagName.script {
            src = url
            type = "module"
        }
    }

    /**
     * Inserts [element] and suspends until the browser reports it: `true` for `load`, `false` for
     * `error`.
     *
     * `before` returning `false` means the caller inserts the element itself, so no event ever
     * arrives and this never returns — the caller owns that element and its events from then on, as
     * upstream's README describes.
     *
     * Nothing sets `async`: a script the loader creates is a dynamic script, which the browser
     * schedules on its own terms whatever the attribute says, so upstream's `async` knob promised
     * something the loader could not deliver and is gone.
     */
    private suspend fun awaitSettled(): Boolean {
        val element = checkNotNull(element) { "no element was made for this attempt" }

        return suspendCoroutine { continuation ->
            // `Event.LOAD` / `Event.ERROR`: the typed constants, not `EventType("load")` strings. The
            // listener comes from `web.events.addEventListener` (the member overload is internal) and
            // takes a handler before `options`, so it is passed positionally rather than as a lambda.
            element.addEventListener(Event.LOAD, EventHandler { continuation.resume(true) })
            element.addEventListener(Event.ERROR, EventHandler { continuation.resume(false) })

            // `!== false` upstream: only an explicit `false` opts out, not `null`/`undefined`.
            if (before?.invoke(path, element) != false) document.head.appendChild(element)
        }
    }

    /** The kind of element a path asks for, and whether this browser should load it at all. */
    private enum class Kind {
        /** `<link rel="stylesheet">` */
        Stylesheet,

        /** `<img>` */
        Image,

        /** `<script>` */
        Script,

        /** `<script type="module">` */
        Module,

        /** The half of a `module!` / `nomodule!` pair this browser is not supposed to run. */
        Skipped,
    }

    companion object {

        /**
         * The query and fragment are cut before the kind is decided, and stay on the URL. Upstream's
         * stripper also cuts at `|`, a stray character in its character class; `?` and `#` are the
         * characters that actually end a path.
         */
        private val QUERY_OR_FRAGMENT = Regex("[?#].*$")

        /** Upstream's forced kinds, which also override the extension. */
        private val LOAD_KIND_PREFIX = Regex("^(?:css|img|module|nomodule)!")

        /** `css!foo` or any `*.css` — upstream forces CSS with the prefix, everything else infers it. */
        private val CSS_PATH = Regex("^css!|\\.css$")

        /** `img!foo` or any extension the browser decodes as an image. */
        private val IMAGE_PATH = Regex("^img!|\\.(?:png|gif|jpg|svg|webp)$")

        private const val MODULE_PREFIX = "module!"

        private const val NOMODULE_PREFIX = "nomodule!"

        /**
         * `'noModule' in document.createElement('script')` — property *existence*, which no typed API
         * expresses (`script.noModule` is `false` on a browser that supports it). Probed once, the
         * first time a `module!` / `nomodule!` path is loaded, so the module still loads where there
         * is no `document` (Node, Docusaurus's prerender pass) as long as no such path is used.
         */
        private val supportsModules: Boolean by lazy {
            js("'noModule' in document.createElement('script')")
        }
    }
}
