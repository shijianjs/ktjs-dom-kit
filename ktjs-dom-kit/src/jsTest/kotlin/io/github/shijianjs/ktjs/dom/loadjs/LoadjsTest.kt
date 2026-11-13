package io.github.shijianjs.ktjs.dom.loadjs

import js.objects.unsafeJso
import js.reflect.unsafeCast
import kotlin.js.Promise
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import web.dom.document
import web.html.HTMLImageElement
import web.html.HTMLLinkElement
import web.html.HTMLScriptElement
import web.html.HTMLElement

/**
 * The `loadjs` wrapper, exercised against the real library in Chromium.
 *
 * Upstream's own suite (`test/tests.js`, mocha + chai) fetches the files under `assets/` from a
 * server. A browser test here has no files to fetch, so the same behaviours are driven through a
 * `data:` URL — a real resource the browser loads — and through a URL that can only fail; what
 * upstream asserts about the files it fetched is asserted here on the created elements, the callbacks
 * and the bundle registry.
 */
class LoadjsTest {

    @Test
    fun theModuleFunctionLoadsAScriptAndRunsItsCallback(): Promise<Unit> {
        val url = scriptUrl("document.title='ktjs-loadjs'")

        return loadAndReport(url).then { outcome ->
            assertTrue(outcome.succeeded, "the load reported a failure: ${outcome.failedPaths}")
            assertEquals("ktjs-loadjs", document.title, "the script that loaded did not run")
        }
    }

    @Test
    fun theListBranchLoadsEveryPathAndTheBundleFormRunsTheCallback(): Promise<Unit> {
        val first = scriptUrl("document.body.append(document.createElement('mark'))")
        val second = scriptUrl("document.body.append(document.createElement('mark'))")

        return loadAndReport(arrayOf(first, second), bundleId = BUNDLE_LIST).then { outcome ->
            assertTrue(outcome.succeeded, "the load reported a failure: ${outcome.failedPaths}")
            assertEquals(2, body.querySelectorAll("mark").length, "not every path ran")
            body.querySelectorAll("mark").forEach { it.remove() }

            // The bundle is registered by then, so `ready` answers without a second load.
            var ready = false
            loadjs.ready(BUNDLE_LIST) { ready = true }
            assertTrue(ready, "the bundle was not satisfied when the callback ran")
        }
    }

    @Test
    fun aPathThatCannotLoadIsReportedWithItsPath(): Promise<Unit> {
        val missing = unloadablePath()

        return loadAndReport(arrayOf(missing, unloadablePath("second"))).then { outcome ->
            assertFalse(outcome.succeeded, "a path that cannot load must not report success")
            assertEquals(2, outcome.failedPaths.size, "both paths failed, so both are reported: ${outcome.failedPaths}")
            assertTrue(missing in outcome.failedPaths, "the failing path is reported as written: ${outcome.failedPaths}")
        }
    }

    @Test
    fun numRetriesRepeatsTheAttemptBeforeReportingTheFailure(): Promise<Unit> {
        var attempts = 0

        return loadAndReport(
            unloadablePath(),
            bundleId = BUNDLE_RETRIES,
            configure = {
                numRetries = 1.0
                // `before` runs once per attempt, so it counts them.
                before = { _, _ -> attempts++; null }
            },
        ).then { outcome ->
            assertFalse(outcome.succeeded, "a path that cannot load must not report success")
            assertEquals(2, attempts, "numRetries = 1 means one retry, so two attempts")
        }
    }

    @Test
    fun beforeSeesTheElementAndTheAsyncOptionReachesIt() {
        val url = scriptUrl("document.title='ktjs-async'")
        var seen: HTMLElement? = null

        loadjs(
            url,
            unsafeJso<LoadOptions> {
                async = false
                before = { _, element ->
                    seen = element
                    element.setAttribute("data-ktjs-probe", "")
                    true
                }
            },
        )

        val script = assertNotNull(seen, "before was not called").unsafeCast<HTMLScriptElement>()
        assertEquals(url, script.getAttribute("src"), "the element points at the path as written")
        assertEquals(false, script.async, "async = false did not reach the <script>")
        assertSame(head, script.parentElement, "returning true inserts the element into <head>")
        assertEquals("", script.getAttribute("data-ktjs-probe"), "what before set on the element survived")
        script.remove()
    }

    @Test
    fun beforeReturningFalseKeepsTheElementOutOfTheDocument() {
        val url = "/ktjs/never-fetched.js"

        loadjs(
            url,
            unsafeJso<LoadOptions> {
                before = { _, _ -> false }
                // Nothing is inserted, so no load event can arrive; the callbacks exist to prove it.
                success = { error("a load that was never inserted cannot succeed") }
            },
        )

        assertEquals(null, head.querySelector("script[src='$url']"), "the element was inserted anyway")
    }

    @Test
    fun thePathPrefixesDecideWhichElementIsCreated() {
        val created = mutableListOf<HTMLElement>()

        loadjs(
            arrayOf("css!/styles.custom", "img!/picture.custom", "module!/module.js"),
            unsafeJso<LoadOptions> { before = { _, element -> created.add(element); false } },
        )

        assertEquals(3, created.size, "one element per path")
        created[0].unsafeCast<HTMLLinkElement>().let {
            assertEquals("stylesheet", it.getAttribute("rel"))
            assertEquals("/styles.custom", it.getAttribute("href"), "the css! prefix is stripped from the URL")
        }
        created[1].unsafeCast<HTMLImageElement>().let {
            assertEquals("/picture.custom", it.getAttribute("src"), "the img! prefix is stripped from the URL")
        }
        created[2].unsafeCast<HTMLScriptElement>().let {
            assertEquals("module", it.getAttribute("type"), "module! loads the script as a module")
            assertEquals("/module.js", it.getAttribute("src"))
        }
        assertEquals(0, head.querySelectorAll("link[href='/styles.custom'], img, script[src='/module.js']").length)
    }

    @Test
    fun aNomodulePathIsSkippedInABrowserThatSupportsModules() {
        var created = 0
        var finished = false

        loadjs(
            "nomodule!/legacy.js",
            unsafeJso<LoadOptions> {
                before = { _, _ -> created++; true }
                success = { finished = true }
            },
        )

        // Chromium has module support, so upstream answers immediately and never touches the DOM.
        assertTrue(finished, "a skipped path still counts as satisfied")
        assertEquals(0, created, "no element for a path this browser is not supposed to load")
    }

    @Test
    fun theBundleIdFormRegistersTheBundleAndResetForgetsIt() {
        loadjs.reset()

        loadjs("/ktjs/never-fetched.js", BUNDLE_DEFINED, unsafeJso<LoadOptions> { before = { _, _ -> false } })

        assertTrue(loadjs.isDefined(BUNDLE_DEFINED), "handing a bundle id to loadjs defines it")
        assertFalse(loadjs.isDefined("ktjs-loadjs-undefined"), "an unknown id is not defined")

        loadjs.reset()
        assertFalse(loadjs.isDefined(BUNDLE_DEFINED), "reset forgets every bundle")
    }

    @Test
    fun doneSatisfiesEachBundleAndReadyWantsAllOfThem() {
        var ran = false

        loadjs.ready(arrayOf(BUNDLE_ONE, BUNDLE_TWO)) { ran = true }
        loadjs.done(BUNDLE_ONE)
        assertFalse(ran, "the callback must wait for the second bundle")

        loadjs.done(BUNDLE_TWO)
        assertTrue(ran, "done satisfies the bundle the callback was waiting for")
    }

    @Test
    fun readyReturnsTheModuleSoCallsChain() {
        assertSame(loadjs, loadjs.ready(BUNDLE_ONE) {}, "a single bundle id chains")
        assertSame(loadjs, loadjs.ready(arrayOf(BUNDLE_ONE, BUNDLE_TWO)) {}, "a list chains")
        assertSame(loadjs, loadjs.ready(BUNDLE_ONE, unsafeJso<ReadyOptions> {}), "the options form chains")
    }

    @Test
    fun readyNamesTheBundlesThatNeverLoaded(): Promise<Unit> {
        val reported = Promise<Array<String>> { resolve, _ ->
            loadjs.ready(
                arrayOf(BUNDLE_DONE, BUNDLE_FAILING),
                unsafeJso<ReadyOptions> {
                    success = { error("one bundle cannot load, so this must report an error") }
                    error = { depsNotFound -> resolve(depsNotFound) }
                },
            )
            loadjs.done(BUNDLE_DONE)
            loadjs(unloadablePath(), BUNDLE_FAILING)
        }

        return reported.then { depsNotFound -> assertEquals(listOf(BUNDLE_FAILING), depsNotFound.toList()) }
    }

    @Test
    fun thePromiseOverloadResolvesOnceTheFilesLoad(): Promise<Unit> {
        val url = scriptUrl("document.title='ktjs-promise'")

        return loadjs(url, promiseOptions {})
            .then { assertEquals("ktjs-promise", document.title, "the promise resolved before the script ran") }
    }

    @Test
    fun thePromiseOverloadRejectsWithThePathsThatFailed(): Promise<Unit> {
        val missing = unloadablePath()

        return loadjs(missing, BUNDLE_PROMISE, promiseOptions { numRetries = 1.0 }).then(
            onFulfilled = { error("a path that cannot load must reject the promise") },
            onRejected = { rejection ->
                assertEquals(listOf(missing), unsafeCast<Array<String>>(rejection).toList())
            },
        )
    }

    /**
     * Runs one load and reports what upstream said about it, whichever callback it used.
     *
     * The `error` and `success` fields of one [LoadOptions] object both resolve the same promise, so a
     * wrong callback fails an assertion instead of hanging the suite.
     */
    private fun loadAndReport(
        paths: String,
        bundleId: String? = null,
        configure: LoadOptions.() -> Unit = {},
    ): Promise<Outcome> = report(configure) { options ->
        if (bundleId == null) loadjs(paths, options) else loadjs(paths, bundleId, options)
    }

    /** The list branch of the same helper. */
    private fun loadAndReport(
        paths: Array<String>,
        bundleId: String? = null,
        configure: LoadOptions.() -> Unit = {},
    ): Promise<Outcome> = report(configure) { options ->
        if (bundleId == null) loadjs(paths, options) else loadjs(paths, bundleId, options)
    }

    private fun report(configure: LoadOptions.() -> Unit, call: (LoadOptions) -> Unit): Promise<Outcome> =
        Promise { resolve, _ ->
            val options = unsafeJso<LoadOptions> {
                success = { resolve(Outcome(succeeded = true)) }
                error = { pathsNotFound -> resolve(Outcome(succeeded = false, failedPaths = pathsNotFound.toList())) }
            }
            options.configure()
            call(options)
        }

    /** What upstream reported for one load. */
    private class Outcome(val succeeded: Boolean, val failedPaths: List<String> = emptyList())

    /**
     * A `data:` URL holding a real script: something the browser genuinely fetches and runs, without a
     * server behind it. The body must not contain `?` or `#` — upstream cuts the URL at those.
     */
    private fun scriptUrl(body: String): String = "data:text/javascript,$body"

    /**
     * A URL that can only fail: the browser does not know the scheme, so the request never leaves the
     * page. Upstream's own suite gets a failing load by asking its server for a file that is not there.
     */
    private fun unloadablePath(name: String = "missing.js"): String = "ktjs-no-such-scheme://$name"

    private val head: HTMLElement get() = document.head

    private val body: HTMLElement get() = document.body

    private companion object {
        const val BUNDLE_LIST = "ktjs-loadjs-list"
        const val BUNDLE_RETRIES = "ktjs-loadjs-retries"
        const val BUNDLE_DEFINED = "ktjs-loadjs-defined"
        const val BUNDLE_ONE = "ktjs-loadjs-one"
        const val BUNDLE_TWO = "ktjs-loadjs-two"
        const val BUNDLE_DONE = "ktjs-loadjs-done"
        const val BUNDLE_FAILING = "ktjs-loadjs-failing"
        const val BUNDLE_PROMISE = "ktjs-loadjs-promise"
    }
}
