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
        page.waitForSelector("ktjs-demo-shell vaadin-app-layout")
        page.setDefaultTimeout(7_000.0)
        // Iconify fetches each icon from the API on first use; screenshots and size assertions are
        // meaningless while the elements are still `loading`.
        waitForIcons(page)
        return page
    }

    /**
     * Waits until every iconify element has rendered. `document.querySelectorAll` cannot see into
     * the shell's shadow tree, so the check goes through a Playwright locator, which does.
     */
    private fun waitForIcons(page: Page) {
        val deadline = System.currentTimeMillis() + 10_000
        while (true) {
            val states = page.locator("ktjs-demo-shell iconify-icon").evaluateAll(
                "nodes => nodes.map((node) => node.status)",
            ) as List<*>
            if (states.isNotEmpty() && states.none { it != "rendered" }) return
            if (System.currentTimeMillis() > deadline) {
                throw AssertionError("icons never finished loading: $states")
            }
            Thread.sleep(100)
        }
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

    /**
     * The window itself never scrolls: the app layout owns the height, the content column and the
     * drawer scroll inside it. This is what keeps a second, always-visible scrollbar out of the
     * navigation — the bug the old sticky sidebar produced on every browser.
     */
    private fun assertOneScreen(page: Page) {
        val metrics = page.evaluate(
            """
            () => {
              const scrolling = document.scrollingElement;
              return { pageScroll: scrolling.scrollHeight - scrolling.clientHeight, inner: innerHeight };
            }
            """.trimIndent(),
        ) as Map<*, *>
        val pageScroll = (metrics["pageScroll"] as Number).toInt()
        assertTrue(pageScroll <= 1, "the window must not scroll; overflow was $pageScroll px")
    }

    /** A navigation column that fits its items shows no scrollbar, not an empty track. */
    private fun assertDrawerHasNoScrollbar(page: Page) {
        val state = page.locator("ktjs-demo-shell vaadin-app-layout").first().evaluate(
            """
            (layout) => {
              const drawer = layout.shadowRoot.querySelector('[part="drawer"]');
              return {
                overflow: drawer.scrollHeight - drawer.clientHeight,
                overflowY: getComputedStyle(drawer).overflowY,
              };
            }
            """.trimIndent(),
        ) as Map<*, *>
        assertTrue((state["overflow"] as Number).toInt() <= 1, "the drawer scrolled by ${state["overflow"]} px")
        assertEquals("auto", state["overflowY"], "the drawer must scroll on demand, not always")
    }

    @Test
    fun `light theme paints a light page`() {
        val page = open("")
        val light = luminance(page, "ktjs-demo-shell")
        assertTrue(light > 0.7, "content background luminance was $light in the light theme")
        assertNoHorizontalOverflow(page)
        assertOneScreen(page)
        assertDrawerHasNoScrollbar(page)
        // The side nav paints the item carrying its `current` state attribute; exactly one must carry it.
        assertEquals(
            1,
            page.locator("ktjs-demo-shell vaadin-side-nav vaadin-side-nav-item[current]").count(),
            "exactly one navigation item is current",
        )
        // The title lives in the navbar, the way the app-layout examples put it there, not in the drawer.
        assertEquals("ktjs-dom-kit", page.locator("ktjs-demo-shell [slot='navbar'] .shell__title").textContent())
        assertEquals(0, page.locator("ktjs-demo-shell [slot='drawer'] .shell__title").count())
        // Every page row carries its Iconify icon in the prefix slot; groups carry one too.
        assertEquals(
            page.locator("ktjs-demo-shell [slot='drawer'] vaadin-side-nav-item[data-route]").count(),
            page.locator(
                "ktjs-demo-shell [slot='drawer'] vaadin-side-nav-item[data-route] iconify-icon[slot='prefix']",
            ).count(),
            "every page row has an icon",
        )
        assertTrue(
            page.locator("ktjs-demo-shell [slot='drawer'] iconify-icon[slot='prefix']").count() >=
                page.locator("ktjs-demo-shell [slot='drawer'] vaadin-side-nav-item").count(),
        )
        // The wrapper is only decoration if the icon never renders: `iconify-icon.status` tells.
        val icon = page.locator("ktjs-demo-shell [slot='drawer'] iconify-icon").first().evaluate(
            """
            (node) => {
              const rect = node.getBoundingClientRect();
              return {
                status: node.status,
                icon: node.icon,
                width: Math.round(rect.width),
                height: Math.round(rect.height),
              };
            }
            """.trimIndent(),
        ) as Map<*, *>
        assertTrue(
            icon["status"] == "rendered",
            "the sidebar icon did not render: $icon",
        )
        assertTrue((icon["width"] as Number).toInt() > 0 && (icon["height"] as Number).toInt() > 0, "icon has no box: $icon")
        shoot(page, "light-overview")
        page.close()
    }

    @Test
    fun `dark theme repaints the same page`() {
        val page = open("")
        page.locator("ktjs-demo-shell [slot='navbar'] vaadin-button:text-is('Dark')").click()
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
        page.locator("ktjs-demo-shell [slot='navbar'] vaadin-button:text-is('中文')").click()
        page.waitForSelector("ktjs-demo-page .page__title:text-is('ktjs-dom-kit 概览')")
        assertEquals(
            "开始",
            page.locator("ktjs-demo-shell vaadin-side-nav vaadin-side-nav-item").first()
                .evaluate("node => node.querySelector('span').textContent") as String,
            "group headings switch language too",
        )
        assertNoHorizontalOverflow(page)
        shoot(page, "zh-overview")
        page.close()
    }

    @Test
    fun `sidebar headings are labels and the counter uses a Lumo button`() {
        val page = open("#/custom-elements")

        // Groups are parents of the one side-nav menu, so a heading is a nav item rather than a bespoke
        // div or a browser-default button.
        assertEquals(
            "vaadin-side-nav-item",
            page.locator("ktjs-demo-shell vaadin-side-nav vaadin-side-nav-item").first()
                .evaluate("node => node.tagName.toLowerCase()") as String,
        )
        assertEquals(
            "vaadin-button",
            page.locator("demo-counter vaadin-button").first()
                .evaluate("node => node.tagName.toLowerCase()") as String,
            "the counter must use a Lumo button, not a native one",
        )

        shoot(page, "light-custom-elements")
        page.locator("ktjs-demo-shell [slot='navbar'] vaadin-button:text-is('Dark')").click()
        page.waitForTimeout(400.0)
        shoot(page, "dark-custom-elements")
        page.close()
    }
}
