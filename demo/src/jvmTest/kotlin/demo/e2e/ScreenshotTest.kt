package demo.e2e

import com.microsoft.playwright.Browser
import com.microsoft.playwright.Page
import com.microsoft.playwright.junit.UsePlaywright
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

/**
 * Paints the site in both themes and writes the PNGs to `build/e2e-results`, then asserts the visual
 * facts a build cannot catch:
 *
 * - the light theme is actually light (a sheet that hard-codes a dark colour fails this);
 * - sidebar group headings are plain labels, not the browser's default bordered `button`;
 * - the counter's increment control is a Lumo button, not the native white `button`;
 * - the page is one screen: sidebar and content column side by side, top bar above the content.
 *
 * Everything here goes through Playwright locators, which pierce shadow roots; `document.querySelector`
 * cannot see into the shell's or the page's shadow tree.
 */
@UsePlaywright(DemoOptions::class)
class ScreenshotTest(private val browser: Browser) {
    private val results: Path = Path.of("build", "e2e-results")

    private fun open(path: String): Page {
        val page = browser.newPage(Browser.NewPageOptions().setViewportSize(1600, 900))
        page.navigate(DemoServer.baseUrl + path)
        page.waitForSelector("ktjs-demo-shell .shell__nav")
        page.setDefaultTimeout(7_000.0)
        return page
    }

    private fun shoot(page: Page, name: String): Path {
        Files.createDirectories(results)
        val file = results.resolve("$name.png").toAbsolutePath()
        page.screenshot(Page.ScreenshotOptions().setPath(file))
        return file
    }

    /** The site is one screen: nothing is wider than the viewport, whatever the language. */
    private fun assertNoHorizontalOverflow(page: Page) {
        val scrollWidth = (page.evaluate("document.scrollingElement.scrollWidth") as Number).toInt()
        val innerWidth = (page.evaluate("innerWidth") as Number).toInt()
        assertEquals(innerWidth, scrollWidth, "the page must not scroll horizontally")
    }

    private fun luminance(page: Page, selector: String): Double =
        (page.locator(selector).first().evaluate(
            """
            (node) => {
              const color = getComputedStyle(node).backgroundColor;
              const parts = color.match(/\d+/g).map(Number);
              return (0.2126 * parts[0] + 0.7152 * parts[1] + 0.0722 * parts[2]) / 255;
            }
            """.trimIndent(),
        ) as Number).toDouble()

    @Test
    fun `light theme paints a light page`() {
        val page = open("")
        val light = luminance(page, "ktjs-demo-shell")
        assertTrue(light > 0.7, "content background luminance was $light in the light theme")
        assertNoHorizontalOverflow(page)
        shoot(page, "light-overview")
        page.close()
    }

    @Test
    fun `dark theme repaints the same page`() {
        val page = open("")
        page.locator("ktjs-demo-shell .shell__actions vaadin-button:text-is('Dark')").click()
        page.waitForTimeout(400.0)
        val dark = luminance(page, "ktjs-demo-shell")
        assertTrue(dark < 0.2, "content background luminance was $dark in the dark theme")
        assertNoHorizontalOverflow(page)
        shoot(page, "dark-overview")
        page.close()
    }

    @Test
    fun `chinese switch repaints every string in place`() {
        val page = open("")
        page.locator("ktjs-demo-shell .shell__actions vaadin-button:text-is('中文')").click()
        page.waitForSelector("ktjs-demo-page .page__title:text-is('ktjs-dom-kit 概览')")
        assertEquals(
            "开始",
            page.locator("ktjs-demo-shell .shell__group-title").first().textContent(),
            "group headings switch language too",
        )
        assertNoHorizontalOverflow(page)
        shoot(page, "zh-overview")
        page.close()
    }

    @Test
    fun `sidebar headings are labels and the counter uses a Lumo button`() {
        val page = open("#/custom-elements")

        assertEquals(
            0,
            page.locator("ktjs-demo-shell button.shell__group-title").count(),
            "sidebar group headings must not be buttons",
        )
        assertEquals(
            "div",
            page.locator("ktjs-demo-shell .shell__group-title").first()
                .evaluate("node => node.tagName.toLowerCase()") as String,
        )
        assertEquals(
            "vaadin-button",
            page.locator("demo-counter vaadin-button").first()
                .evaluate("node => node.tagName.toLowerCase()") as String,
            "the counter must use a Lumo button, not a native one",
        )

        shoot(page, "light-custom-elements")
        page.locator("ktjs-demo-shell .shell__actions vaadin-button:text-is('Dark')").click()
        page.waitForTimeout(400.0)
        shoot(page, "dark-custom-elements")
        page.close()
    }
}
