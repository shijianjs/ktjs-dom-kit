package io.github.shijianjs.ktjs.dom.loadjs

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope

/**
 * Loads scripts, stylesheets and images into `<head>`, and answers for the bundles they belong to.
 *
 * This is [`loadjs`](https://github.com/kubetail-org/loadjs) translated to Kotlin rather than bound to
 * the npm package: the upstream source is ~320 lines of stable ES5 that has not changed in years, so
 * owning it costs less than a dependency plus the UMD function-object export to wrap. Upstream's
 * callback API is not carried over either: a call is one file and it suspends, and a list of files
 * is just the single call run concurrently.
 *
 * The object is the page-wide half only: the id-to-bundle table and the factory that hands a bundle
 * out. Everything about *one* url — the path, the element, the attempts, how the load ended and who
 * waits for it — lives in the [LoadFile] that [bundle] returns, so `ready` / `done` are things a
 * bundle does:
 *
 * ```kotlin
 * val assets = Loadjs.bundle("assets")
 * launch { assets.load("/assets/app.css") }
 * launch { assets.ready() }                      // returns once the bundle has arrived
 *
 * Loadjs("/assets/app.js")                       // shorthand: one file, no bundle
 * ```
 *
 * A path is a URL; the extension, or the prefix in front of it, decides which element is created:
 *
 * | Path | Element |
 * | --- | --- |
 * | `foo.js`, and anything else | `<script>` |
 * | `foo.css`, `css!foo.custom` | `<link rel="stylesheet">` |
 * | `foo.png` / `.jpg` / `.gif` / `.svg` / `.webp`, `img!foo.custom` | `<img>` |
 * | `module!foo.js` | `<script type="module">`; skipped in a browser without module support |
 * | `nomodule!foo.js` | plain `<script>`; skipped in a browser that *has* module support |
 *
 * A skipped path counts as loaded, which is how one bundle serves both kinds of browser. Elements go
 * into `<head>` unless the `before` hook returns `false`; nothing is removed from the document
 * afterwards.
 *
 * The registry belongs to the page, as upstream's module-level caches did, so this is a singleton
 * rather than something to instantiate and pass around: [bundle] hands out the same object for the
 * same id, a bundle outlives the call that started it, and the coroutine that waits does not have to
 * be the one that loads. Loading is DOM work (`document.createElement`, `<head>`), so this is for the
 * browser — pulling the package into Node (Docusaurus's prerender pass, for instance) fails at the
 * first `document`. Compared with upstream it drops what only older browsers needed: the IE9–11
 * empty-stylesheet check (`hideFocus` / `rel="preload"`) and the `onbeforeload` hook, which no current
 * browser fires.
 *
 * Both forms of the call are `suspend`, and the module depends on `kotlinx-coroutines-core` (`api`),
 * so a page writing `launch { … }` has the library already.
 */
object Loadjs {

    /** Every bundle the page knows. The state is in the [LoadFile]; this is only the way to find it. */
    private val bundles = mutableMapOf<String, LoadFile>()

    /**
     * The bundle filed under [bundleId], created when that id is first mentioned — the way a page
     * reaches its bundles, and the only factory.
     *
     * Same id, same object: this does not throw when a bundle already exists. The
     * [BundleAlreadyDefinedException] is about loading one *twice* — a [LoadFile.load] on a bundle that
     * has already loaded throws it.
     *
     * ```kotlin
     * val assets = Loadjs.bundle("assets")
     * launch { assets.load("/assets/app.js") }    // defines the bundle
     * launch { assets.ready() }                   // waits for it, from anywhere
     * ```
     */
    fun bundle(bundleId: String): LoadFile = bundles.getOrPut(bundleId) { LoadFile(bundleId) }

    /**
     * Loads [path] and returns once the browser has it — the shorthand for [bundle] plus
     * [LoadFile.load], for the case where nothing has to wait on a bundle id.
     *
     * With a [bundleId] the load files its result under that bundle; without one it is a throwaway
     * file nothing else can reach.
     */
    suspend operator fun invoke(
        path: String,
        bundleId: String? = null,
        numRetries: Int = 0,
        before: BeforeLoad? = null,
    ) {
        (bundleId?.let(::bundle) ?: LoadFile(bundleId = null)).load(path, numRetries, before)
    }

    /**
     * Loads every path in [paths] at the same time and returns once they have all arrived, or throws
     * [LoadsFailedException] naming the ones that did not.
     *
     * This is structured concurrency, not bookkeeping: each path is one [async] of the single-path
     * [invoke], so they are in flight together and the call waits for all of them. Files that did
     * arrive stay in the document — nothing is unloaded because a sibling failed.
     *
     * ```kotlin
     * Loadjs(listOf("/assets/app.js", "/assets/app.css", "/assets/logo.png"))
     * ```
     *
     * A bundle id names one load, so this form takes none; several files waited on as a group is
     * `coroutineScope { … }` around the single calls, which is exactly what this does.
     */
    suspend operator fun invoke(
        paths: List<String>,
        numRetries: Int = 0,
        before: BeforeLoad? = null,
    ) {
        val failed = supervisorScope {
            paths.map { path -> async { path to arrived(path, numRetries, before) } }.awaitAll()
        }.filterNot { (_, arrived) -> arrived }
            .map { (path, _) -> path }

        if (failed.isNotEmpty()) throw LoadsFailedException(failed)
    }

    /** Loads one path and answers whether it arrived, so the list form can report every failure. */
    private suspend fun arrived(path: String, numRetries: Int, before: BeforeLoad?): Boolean =
        try {
            invoke(path, numRetries = numRetries, before = before)
            true
        } catch (_: LoadFailedException) {
            false
        }

    /** Whether that id has been handed to a load since [reset]. */
    fun isDefined(bundleId: String): Boolean = bundles[bundleId]?.isDefined == true

    /**
     * Forgets every bundle, its outcome and its waiters.
     *
     * For tests, and for a page that reloads its own scripts in place. A `ready` that is still waiting
     * belongs to the state being thrown away and is dropped, not resumed: the coroutine that made it
     * never returns.
     */
    fun reset() {
        bundles.clear()
    }
}
