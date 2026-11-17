package demo.e2e

import com.microsoft.playwright.Browser
import com.microsoft.playwright.Page
import com.microsoft.playwright.junit.UsePlaywright
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The acceptance rules every demo site of this account has to meet, asserted in a real browser:
 *
 * - one screen, not a stack of sections: sidebar with groups, top bar with a breadcrumb, an action
 *   row and a content area that swaps one page at a time;
 * - `#/<slug>` deep links, navigation highlighting and `document.title` following the route;
 * - a language switch that rewrites text in place and keeps page state;
 * - a light/dark switch that flips the document theme;
 * - no stylesheet of ours on `document`, no inline styling beyond the host element, no console errors.
 */
@UsePlaywright(DemoOptions::class)
class DemoPageTest(private val browser: Browser) {
    private val consoleErrors: MutableList<String> = mutableListOf()

    private fun open(path: String): Page {
        val page = browser.newPage(Browser.NewPageOptions().setViewportSize(1280, 800))
        page.onPageError { consoleErrors += it }
        page.onConsoleMessage { message ->
            if (message.type() == "error") consoleErrors += message.text()
        }
        page.navigate(DemoServer.baseUrl + path.trimStart('#').let { if (path.startsWith("#")) "#$it" else it })
        page.waitForSelector("ktjs-demo-shell .shell__nav")
        page.setDefaultTimeout(7_000.0)
        return page
    }

    private fun assertNoErrors() {
        assertEquals(emptyList<String>(), consoleErrors)
    }

    @Test
    fun `shell upgrades and keeps our sheets inside shadow roots`() {
        val page = open("")

        assertEquals(0, (page.evaluate("document.adoptedStyleSheets.length") as Number).toInt())
        assertTrue(page.evaluate("customElements.get('ktjs-demo-shell') !== undefined") as Boolean)
        assertTrue(page.evaluate("customElements.get('ktjs-demo-page') !== undefined") as Boolean)
        assertEquals(
            1,
            (page.evaluate("document.querySelectorAll('[style]').length") as Number).toInt(),
            "only the host element carries a style attribute",
        )
        assertNoErrors()
    }

    @Test
    fun `deep link renders the page, the breadcrumb and the document title`() {
        val page = open("#/custom-elements")

        assertTrue(page.title().contains("Custom elements"), "title was: ${page.title()}")
        val breadcrumb = page.locator("ktjs-demo-shell .shell__breadcrumb").textContent().orEmpty()
        assertTrue(breadcrumb.contains("Custom elements"), "breadcrumb was: $breadcrumb")
        assertTrue(page.locator("ktjs-demo-page .page__title").textContent().orEmpty().isNotEmpty())
        assertEquals("#/custom-elements", page.evaluate("location.hash") as String)
        assertNoErrors()
    }

    @Test
    fun `navigation swaps exactly one page`() {
        val page = open("")

        assertTrue(page.locator("ktjs-demo-shell .shell__nav vaadin-side-nav-item").count() >= 4)
        page.locator("[data-route='stylesheets']").first().click()
        page.waitForSelector("ktjs-demo-page .page__title:text-is('Stylesheets')")
        assertEquals(1, page.locator("ktjs-demo-page").count(), "the content area hosts one page")
        assertNoErrors()
    }

    @Test
    fun `language switch rewrites text and keeps page state`() {
        val page = open("#/custom-elements")

        val counter = page.locator("demo-counter").first()
        counter.locator("button").click()
        counter.locator("button").click()
        assertEquals("2", counter.locator(".counter__value").textContent())

        page.locator("ktjs-demo-shell .shell__actions vaadin-button:text-is('中文')").click()
        page.waitForSelector("ktjs-demo-page .page__title:text-is('自定义元素')")
        assertEquals(
            "2",
            page.locator("demo-counter").first().locator(".counter__value").textContent(),
            "the page cache kept the counter state across the language switch",
        )
        assertNoErrors()
    }

    @Test
    fun `theme switch flips the document theme`() {
        val page = open("")

        page.locator("ktjs-demo-shell .shell__actions vaadin-button:text-is('Dark')").click()
        assertEquals(
            "dark",
            page.evaluate("document.documentElement.getAttribute('theme')") as String,
        )

        page.locator("ktjs-demo-shell .shell__actions vaadin-button:text-is('Light')").click()
        assertEquals(
            null,
            page.evaluate("document.documentElement.getAttribute('theme')") as String?,
        )
        assertNoErrors()
    }
}
