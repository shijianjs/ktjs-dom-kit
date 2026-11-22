package io.github.shijianjs.ktjs.dom.loadjs

import js.reflect.unsafeCast
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.js.Promise
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import web.dom.document
import web.html.HTMLImageElement
import web.html.HTMLLinkElement
import web.html.HTMLScriptElement
import web.html.HTMLElement

/**
 * The loader against a real browser: element kinds, insertion, retries and the bundle registry.
 *
 * Upstream's suite (`test/tests.js`, mocha + chai) fetches files under `assets/` from a server. A
 * browser test here has no server, so the same behaviours run through `data:` URLs — real resources
 * the browser loads and runs — and through a URL that can only fail. The cases map onto upstream's
 * groups: JavaScript loading, the `before` / `numRetries` options, the `css!` / `img!` / `module!` /
 * `nomodule!` paths, and the API tests (`ready` / `done` / `reset` / `isDefined`).
 *
 * `Loadjs` suspends, so each case is a `running { … }` body — a `runTest` coroutine handed to the test
 * DSL as the promise it expects. A load whose element `before` keeps out of the document never returns
 * and runs in `backgroundScope`, which the end of the test cancels. The registry is the page's, as
 * upstream's caches were, so every test starts from `Loadjs.reset()`.
 */
class LoadjsTest {

    @BeforeTest
    fun startFromAnEmptyRegistry() {
        Loadjs.reset()
    }

    /* ---- loading a file ---- */

    @Test
    fun aSinglePathLoadsItsScript(): Promise<Unit> = running {
        Loadjs(scriptUrl("document.title='ktjs-single'"))

        assertEquals("ktjs-single", document.title, "the call returned before the script ran")
    }

    @Test
    fun aPathThatCannotLoadThrowsItsPath(): Promise<Unit> = running {
        val missing = unloadablePath()

        val failure = assertIs<LoadFailedException>(failureOf { Loadjs(missing) })

        assertEquals(missing, failure.path)
    }

    @Test
    fun aFailedPathIsReportedWithItsPrefix(): Promise<Unit> = running {
        val failing = "css!" + unloadablePath("styles.css")

        val failure = assertIs<LoadFailedException>(failureOf { Loadjs(failing) })

        assertEquals(failing, failure.path, "the path is reported as written, prefix included")
    }

    @Test
    fun beforeSeesTheElementTheLoaderInserts(): Promise<Unit> = running {
        val url = scriptUrl("document.title='ktjs-before'")
        var seen: HTMLElement? = null

        Loadjs(
            url,
            before = { _, element ->
                seen = element
                element.setAttribute("data-ktjs-probe", "")
                true
            },
        )

        val script = assertNotNull(seen, "before was not called").unsafeCast<HTMLScriptElement>()
        assertEquals(url, script.getAttribute("src"), "the element points at the path as written")
        assertTrue(head.contains(script), "returning true leaves the insertion to the loader")
        assertEquals("", script.getAttribute("data-ktjs-probe"), "what before set on the element survived")
        script.remove()
    }

    @Test
    fun beforeReturningFalseKeepsTheElementOutOfTheDocument(): Promise<Unit> = running {
        val url = "/ktjs/never-fetched.js"

        // Nothing is inserted, so no event can arrive and this call never returns.
        startAndLeaveRunning { Loadjs(url, before = { _, _ -> false }) }

        assertNull(head.querySelector("script[src='$url']"), "the element was inserted anyway")
    }

    @Test
    fun beforeReturningNullStillInsertsTheElement(): Promise<Unit> = running {
        var created: HTMLElement? = null

        startAndLeaveRunning {
            Loadjs(
                scriptUrl("void 0"),
                // `null`, not `false`: only an explicit `false` opts out of the insertion.
                before = { _, element -> created = element; null },
            )
        }

        assertTrue(head.contains(assertNotNull(created)), "returning null inserts the element")
        created?.remove()
    }

    /* ---- several files at once ---- */

    @Test
    fun aListOfPathsLoadsAllOfThem(): Promise<Unit> = running {
        val first = scriptUrl("document.head.setAttribute('data-ktjs-first','1')")
        val second = scriptUrl("document.head.setAttribute('data-ktjs-second','1')")

        Loadjs(listOf(first, second))

        assertEquals("1", head.getAttribute("data-ktjs-first"), "the first script did not run")
        assertEquals("1", head.getAttribute("data-ktjs-second"), "the second script did not run")
        head.removeAttribute("data-ktjs-first")
        head.removeAttribute("data-ktjs-second")
    }

    @Test
    fun aListOfPathsReportsTheOnesThatDidNotArrive(): Promise<Unit> = running {
        val arrived = scriptUrl("document.head.setAttribute('data-ktjs-arrived','1')")
        val missing = unloadablePath("missing.js")
        val alsoMissing = "css!" + unloadablePath("missing.css")

        val failure = assertIs<LoadsFailedException>(
            failureOf { Loadjs(listOf(arrived, missing, alsoMissing)) },
        )

        assertEquals(listOf(missing, alsoMissing), failure.paths, "the failures keep the order they were asked in")
        assertEquals("1", head.getAttribute("data-ktjs-arrived"), "a file that arrived stays loaded")
        head.removeAttribute("data-ktjs-arrived")
    }

    @Test
    fun anEmptyListReturnsAtOnce(): Promise<Unit> = running {
        Loadjs(emptyList<String>())
    }

    /* ---- which element a path asks for ---- */

    @Test
    fun theExtensionDecidesTheKindEvenWithAQueryOrFragment(): Promise<Unit> = running {
        val created = mutableListOf<HTMLElement>()

        listOf("/ktjs/styles.css?x=1", "/ktjs/picture.png#frag", "/ktjs/app.js?v=2").forEach { path ->
            startAndLeaveRunning { Loadjs(path, before = { _, element -> created.add(element); false }) }
        }

        assertEquals(3, created.size, "one element per path")
        created[0].unsafeCast<HTMLLinkElement>().let {
            assertEquals("stylesheet", it.getAttribute("rel"))
            assertEquals("/ktjs/styles.css?x=1", it.getAttribute("href"), "the query stays on the URL")
        }
        created[1].unsafeCast<HTMLImageElement>().let {
            assertEquals("/ktjs/picture.png#frag", it.getAttribute("src"), "the fragment stays on the URL")
        }
        created[2].unsafeCast<HTMLScriptElement>().let { script ->
            assertEquals("/ktjs/app.js?v=2", script.getAttribute("src"))
            // The loader adds neither `async` nor `defer`: a script it creates is a dynamic script,
            // and the browser schedules it on its own terms whatever those attributes say.
            assertFalse(script.hasAttribute("async"), "the loader does not set an attribute it cannot honour")
        }
    }

    @Test
    fun thePrefixesOverrideTheExtensionAndAreStrippedFromTheUrl(): Promise<Unit> = running {
        val created = mutableListOf<HTMLElement>()

        listOf("css!/ktjs/styles.custom", "img!/ktjs/picture.custom", "module!/ktjs/module.js").forEach { path ->
            startAndLeaveRunning { Loadjs(path, before = { _, element -> created.add(element); false }) }
        }

        assertEquals(3, created.size, "one element per path")
        created[0].unsafeCast<HTMLLinkElement>().let {
            assertEquals("stylesheet", it.getAttribute("rel"))
            assertEquals("/ktjs/styles.custom", it.getAttribute("href"), "the css! prefix is stripped")
        }
        created[1].unsafeCast<HTMLImageElement>().let {
            assertEquals("/ktjs/picture.custom", it.getAttribute("src"), "the img! prefix is stripped")
        }
        created[2].unsafeCast<HTMLScriptElement>().let {
            assertEquals("module", it.getAttribute("type"), "module! loads the script as a module")
            assertEquals("/ktjs/module.js", it.getAttribute("src"))
        }
    }

    @Test
    fun aNomodulePathIsSkippedWhereModulesExist(): Promise<Unit> = running {
        var created = 0

        // Chromium has module support, so the pair's other half applies: no element, and the call
        // returns as if the file had loaded.
        Loadjs("nomodule!/ktjs/legacy.js", before = { _, _ -> created++; true })

        assertEquals(0, created, "no element for a path this browser is not supposed to load")
    }

    @Test
    fun aStylesheetLoadsWithTheCssPrefix(): Promise<Unit> = running {
        var link: HTMLLinkElement? = null

        Loadjs(
            "css!data:text/css,:root{--ktjs-loadjs:1}",
            before = { _, element -> link = element.unsafeCast<HTMLLinkElement>(); true },
        )

        assertNotNull(assertNotNull(link).sheet, "the browser did not parse the stylesheet")
        link.remove()
    }

    @Test
    fun anImageLoadsWithTheImagePrefix(): Promise<Unit> = running {
        var image: HTMLImageElement? = null

        Loadjs("img!$PIXEL_GIF", before = { _, element -> image = element.unsafeCast<HTMLImageElement>(); true })

        assertTrue(assertNotNull(image).naturalWidth > 0, "the browser did not decode the image")
        image.remove()
    }

    /* ---- retries ---- */

    @Test
    fun numRetriesRepeatsTheAttemptBeforeFailing(): Promise<Unit> = running {
        var attempts = 0
        val missing = unloadablePath()

        val failure = assertIs<LoadFailedException>(
            failureOf { Loadjs(missing, numRetries = 2, before = { _, _ -> attempts++; null }) },
        )

        assertEquals(missing, failure.path)
        assertEquals(3, attempts, "numRetries = 2 means two retries, so three attempts")
    }

    /* ---- the bundle registry ---- */

    @Test
    fun bundleHandsOutOneObjectPerId() {
        val bundle = Loadjs.bundle(BUNDLE_ONE)

        assertSame(bundle, Loadjs.bundle(BUNDLE_ONE), "the same id must return the same bundle")
        assertFalse(bundle.isDefined, "asking for a bundle does not define it")
    }

    @Test
    fun aBundleIdCannotBeHandedToTheLoaderTwice(): Promise<Unit> = running {
        // A failing load still defines the id before it throws.
        failureOf { Loadjs(unloadablePath(), bundleId = BUNDLE_DEFINED) }

        val thrown = failureOf { Loadjs(unloadablePath("second.js"), bundleId = BUNDLE_DEFINED) }

        assertIs<BundleAlreadyDefinedException>(thrown)
    }

    @Test
    fun isDefinedAnswersForWhatTheLoaderSaw(): Promise<Unit> = running {
        assertFalse(Loadjs.isDefined(BUNDLE_DEFINED), "nothing has been handed over yet")

        failureOf { Loadjs(unloadablePath(), bundleId = BUNDLE_DEFINED) }

        assertTrue(Loadjs.isDefined(BUNDLE_DEFINED), "handing a bundle id to the loader defines it")
        assertFalse(Loadjs.isDefined("ktjs-undefined"), "an unknown id is not defined")
    }

    @Test
    fun readyReturnsAtOnceForABundleDoneEarlier(): Promise<Unit> = running {
        Loadjs.bundle(BUNDLE_DONE).done()

        Loadjs.bundle(BUNDLE_DONE).ready() // answers from the cached result instead of suspending
    }

    @Test
    fun readyWaitsUntilDonePublishesTheBundle(): Promise<Unit> = running {
        var resumed = false

        startAndLeaveRunning { Loadjs.bundle(BUNDLE_ONE).ready(); resumed = true }
        assertFalse(resumed, "ready returned before the bundle was done")

        Loadjs.bundle(BUNDLE_ONE).done()
        testScheduler.runCurrent()

        assertTrue(resumed, "done did not resume the waiting call")
    }

    @Test
    fun readyMadeBeforeTheLoadIsResumedByIt(): Promise<Unit> = running {
        var resumed = false

        // `ready` on an id nothing has loaded yet: the entry exists, its load has not started.
        startAndLeaveRunning { Loadjs.bundle(BUNDLE_ONE).ready(); resumed = true }
        assertFalse(resumed)

        Loadjs(scriptUrl("void 0"), bundleId = BUNDLE_ONE)
        testScheduler.runCurrent() // the waiter was resumed onto the test dispatcher

        assertTrue(resumed, "the load did not finish the bundle an earlier ready was waiting on")
    }

    @Test
    fun readyReportsTheFailureItsLoadHad(): Promise<Unit> = running {
        val missing = unloadablePath("bundle.js")
        failureOf { Loadjs(missing, bundleId = BUNDLE_FAILING) }

        val failure = assertIs<LoadFailedException>(failureOf { Loadjs.bundle(BUNDLE_FAILING).ready() })

        assertEquals(missing, failure.path, "ready reports the load's own failure")
    }

    @Test
    fun resetForgetsTheIdsAndTheirResults(): Promise<Unit> = running {
        failureOf { Loadjs(unloadablePath(), bundleId = BUNDLE_DEFINED) }
        assertTrue(Loadjs.isDefined(BUNDLE_DEFINED))

        Loadjs.reset()

        assertFalse(Loadjs.isDefined(BUNDLE_DEFINED), "reset forgets every id")

        // The id is free again — this does not throw `BundleAlreadyDefinedException` — and the fresh
        // failure is its own, so the cached one went with it.
        val again = unloadablePath("again.js")
        val failure = assertIs<LoadFailedException>(failureOf { Loadjs(again, bundleId = BUNDLE_DEFINED) })
        assertEquals(again, failure.path)
    }

    /* ---- helpers ---- */

    /**
     * A case as a coroutine: `runTest` gives the case virtual time and a `backgroundScope`, and on
     * Kotlin/JS it returns coroutines-test's own promise type — the same object the test DSL takes
     * under its stdlib name, so the cast happens here once instead of in every case.
     */
    private fun running(block: suspend TestScope.() -> Unit): Promise<Unit> =
        runTest(testBody = block).unsafeCast<Promise<Unit>>()

    /**
     * Starts a load and leaves it running: for the calls whose element `before` keeps out of the
     * document, which therefore never return. `backgroundScope` is cancelled when the test ends, so a
     * suspended call is not a leak.
     */
    private fun TestScope.startAndLeaveRunning(block: suspend () -> Unit) {
        backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { block() }
    }

    /** What a suspending call threw, so a `runTest` body can assert on a failure without a helper. */
    private suspend fun failureOf(block: suspend () -> Unit): Throwable {
        var thrown: Throwable? = null
        try {
            block()
        } catch (failure: Throwable) {
            thrown = failure
        }
        return thrown ?: error("the call was expected to fail and returned instead")
    }

    /**
     * A `data:` URL holding a real script: something the browser genuinely fetches and runs, without a
     * server behind it. The body must not contain `?` or `#` — the loader cuts the path at those.
     */
    private fun scriptUrl(body: String): String = "data:text/javascript,$body"

    /**
     * A URL that can only fail: the browser does not know the scheme, so the request never leaves the
     * page. Upstream's own suite gets a failing load by asking its server for a file that is not there.
     */
    private fun unloadablePath(name: String = "missing.js"): String = "ktjs-no-such-scheme://$name"

    private val head: HTMLElement get() = document.head

    private companion object {
        const val BUNDLE_DEFINED = "ktjs-loadjs-defined"
        const val BUNDLE_ONE = "ktjs-loadjs-one"
        const val BUNDLE_DONE = "ktjs-loadjs-done"
        const val BUNDLE_FAILING = "ktjs-loadjs-failing"

        /** A 1×1 transparent GIF; `img!` makes the browser fetch and decode it. */
        const val PIXEL_GIF =
            "data:image/gif;base64,R0lGODlhAQABAIAAAP///wAAACH5BAAAAAAALAAAAAABAAEAAAICRAEAOw=="
    }
}
