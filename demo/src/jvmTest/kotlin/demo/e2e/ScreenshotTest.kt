package demo.e2e

import com.microsoft.playwright.Browser
import com.microsoft.playwright.Locator
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
 * cannot see into the layout's or the page's shadow tree.
 */
@UsePlaywright(DemoOptions::class)
class ScreenshotTest(private val browser: Browser) {
    private val results: Path = Path.of("build", "e2e-results")

    private fun open(path: String): Page {
        val page = browser.newPage(Browser.NewPageOptions().setViewportSize(1600, 900))
        page.navigate(DemoServer.baseUrl + path)
        page.waitForSelector("ktjs-demo-layout vaadin-app-layout")
        // The layout builds its menus in mount(), one task after the element itself upgrades;
        // measuring or clicking before they exist makes a test depend on timing, not on behaviour.
        page.locator("ktjs-demo-layout vaadin-menu-bar-item[data-menu='theme']").first().waitFor()
        page.setDefaultTimeout(7_000.0)
        // Iconify fetches each icon from the API on first use; screenshots and size assertions are
        // meaningless while the elements are still `loading`.
        waitForIcons(page)
        return page
    }

    /**
     * Waits until every iconify element has rendered. `document.querySelectorAll` cannot see into
     * the layout's shadow tree, so the check goes through a Playwright locator, which does.
     */
    private fun waitForIcons(page: Page) {
        val deadline = System.currentTimeMillis() + 10_000
        while (true) {
            val states = page.locator("ktjs-demo-layout iconify-icon").evaluateAll(
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

    /**
     * Waits until [locator]'s text contains [text].
     *
     * The pages repaint from `MutationObserver` callbacks, which the browser delivers on a microtask;
     * reading the text once right after a click would measure timing instead of behaviour.
     */
    private fun waitForText(locator: Locator, text: String, timeoutMs: Long = 5_000) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (true) {
            val current = locator.textContent() ?: ""
            if (current.contains(text)) return
            if (System.currentTimeMillis() > deadline) {
                throw AssertionError("'$text' never appeared; the section says: $current")
            }
            Thread.sleep(100)
        }
    }

    private fun openMenu(page: Page, menu: String) {
        page.locator("ktjs-demo-layout vaadin-menu-bar-item[data-menu='$menu']")
            .first()
            .click()
        // The entries are teleported into the menu's overlay; waiting for one keeps the next
        // step independent of the opening animation.
        page.locator("vaadin-menu-bar-item[data-action]:visible").first().waitFor()
    }

    private fun pick(page: Page, action: String) {
        openMenu(page, action.substringBefore(':'))
        page.locator("vaadin-menu-bar-item[data-action='$action']").first().click()
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

    /**
     * A navigation card that fits its items shows no scrollbar, not an empty track.
     *
     * The card is what scrolls now, not the column around it: the column holds the gutter and the card
     * fills it, so it can never overflow — measuring it would assert nothing.
     */
    private fun assertDrawerHasNoScrollbar(page: Page) {
        val state = page.locator("ktjs-demo-layout .layout__drawer").first().evaluate(
            """
            (card) => {
              return {
                overflow: card.scrollHeight - card.clientHeight,
                overflowY: getComputedStyle(card).overflowY,
              };
            }
            """.trimIndent(),
        ) as Map<*, *>
        assertTrue((state["overflow"] as Number).toInt() <= 1, "the drawer card scrolled by ${state["overflow"]} px")
        assertEquals("auto", state["overflowY"], "the drawer card must scroll on demand, not always")
    }

    /**
     * The shell is three cards on a canvas, and this is the geometry that says so: one white surface
     * per region, the same radius on all three, the same gutter to the window edges and the same gap
     * between neighbours — and a navbar band that is still 56px tall, the height a Vaadin app layout
     * gives it, so the cards cost the page no more than the gutter on each side.
     *
     * The numbers are the sheet's own ([8] gutter, [8] gap, 16px radius), restated on purpose: a
     * screenshot shows that the layout *looks* right but not that a card is 8px from an edge on one
     * side and 16px on the other, and that is the mistake a redesign makes.
     */
    private fun assertShellIsCards(page: Page) {
        val state = page.locator("ktjs-demo-layout").first().evaluate(
            """
            (layout) => {
              const app = layout.shadowRoot.querySelector('vaadin-app-layout');
              const measure = (selector) => {
                const node = layout.shadowRoot.querySelector(selector);
                const box = node.getBoundingClientRect();
                const style = getComputedStyle(node);
                return {
                  x: Math.round(box.left),
                  y: Math.round(box.top),
                  w: Math.round(box.width),
                  h: Math.round(box.height),
                  right: Math.round(innerWidth - box.right),
                  bottom: Math.round(innerHeight - box.bottom),
                  background: style.backgroundColor,
                  radius: style.borderTopLeftRadius,
                  border: style.borderTopWidth,
                  overflowY: style.overflowY,
                };
              };
              const band = app.shadowRoot.querySelector('[part~="navbar"]');
              const content = layout.shadowRoot.querySelector('.layout__content');
              return {
                inner: [innerWidth, innerHeight],
                canvas: getComputedStyle(layout).backgroundColor,
                band: Math.round(band.getBoundingClientRect().height),
                topbar: measure('.layout__topbar'),
                drawer: measure('.layout__drawer'),
                content: measure('.layout__content'),
                page: content.firstElementChild ? {
                  overflowY: getComputedStyle(content.firstElementChild).overflowY,
                  insetLeft: Math.round(
                    content.firstElementChild.getBoundingClientRect().left - content.getBoundingClientRect().left,
                  ),
                  insetRight: Math.round(
                    content.getBoundingClientRect().right - content.firstElementChild.getBoundingClientRect().right,
                  ),
                } : null,
              };
            }
            """.trimIndent(),
        ) as Map<*, *>

        @Suppress("UNCHECKED_CAST")
        val navbar = state["topbar"] as Map<*, *>

        @Suppress("UNCHECKED_CAST")
        val drawerCard = state["drawer"] as Map<*, *>

        @Suppress("UNCHECKED_CAST")
        val contentCard = state["content"] as Map<*, *>

        // One surface design, three times: white, the card radius, no hairline, and never a sliver.
        listOf("the navbar" to navbar, "the drawer" to drawerCard, "the content column" to contentCard)
            .forEach { (name, card) ->
                assertEquals("rgb(255, 255, 255)", card["background"], "$name is a white card")
                assertEquals("16px", card["radius"], "$name shares the card radius")
                assertEquals("0px", card["border"], "$name has no hairline, only its shadow")
                assertTrue((card["w"] as Number).toInt() > 200, "$name is wide enough to be a card")
                assertTrue((card["h"] as Number).toInt() >= 40, "$name is tall enough to be a card")
            }

        // The canvas is a step below the cards, or the cards are not floating on anything.
        assertTrue(
            state["canvas"] != navbar["background"],
            "the page canvas must differ from the card surface, both were ${state["canvas"]}",
        )

        // The band keeps Vaadin's 56px: the cards are inset *inside* it, not added around it.
        assertEquals(56, (state["band"] as Number).toInt(), "the navbar band grew past its 56px")

        // The window's edges: the bar is one gutter in from all three it touches, and each column is one
        // gutter in from the sides it has to itself.
        assertEquals(8, (navbar["x"] as Number).toInt(), "the navbar is one gutter from the left")
        assertEquals(8, (navbar["right"] as Number).toInt(), "the navbar is one gutter from the right")
        assertEquals(8, (navbar["y"] as Number).toInt(), "the navbar is one gutter from the top")
        assertEquals(8, (drawerCard["x"] as Number).toInt(), "the drawer is one gutter from the left")
        assertEquals(8, (drawerCard["bottom"] as Number).toInt(), "the drawer is one gutter from the bottom")
        assertEquals(8, (contentCard["right"] as Number).toInt(), "the content column is one gutter from the right")
        assertEquals(8, (contentCard["bottom"] as Number).toInt(), "the content column is one gutter from the bottom")

        // And the gaps: the bar does not touch the row under it, and the two columns do not touch each
        // other. The content column's left padding is that gap, so the two boxes are what has to show it.
        val underTheBar =
            (drawerCard["y"] as Number).toInt() - ((navbar["y"] as Number).toInt() + (navbar["h"] as Number).toInt())
        assertEquals(8, underTheBar, "the gap under the navbar was ${underTheBar}px, not one gap")
        val betweenTheColumns =
            (contentCard["x"] as Number).toInt() - ((drawerCard["x"] as Number).toInt() + (drawerCard["w"] as Number).toInt())
        assertEquals(8, betweenTheColumns, "the gap between the columns was ${betweenTheColumns}px, not one gap")

        // The page scrolls inside the card's padding instead of the card scrolling itself: a scrollbar
        // painted on the card's own edge lies across its rounded corner.
        val innerWidthValue = (state["inner"] as List<*>)[0] as Number
        val innerHeightValue = (state["inner"] as List<*>)[1] as Number
        @Suppress("UNCHECKED_CAST")
        val inner = state["page"] as Map<*, *>
        assertEquals("visible", contentCard["overflowY"], "the card itself must not scroll")
        assertEquals("auto", inner["overflowY"], "the page inside must scroll")
        // The page is inside the card's 16px padding on both sides, so its scrollbar cannot reach the arc.
        assertEquals(16, (inner["insetLeft"] as Number).toInt(), "the page starts at the card's padding")
        assertEquals(16, (inner["insetRight"] as Number).toInt(), "the page ends at the card's padding")
        // The vertical budget: the band plus one gutter is all the chrome the page pays for.
        assertEquals(
            (innerHeightValue.toInt() - 56 - 8),
            (contentCard["h"] as Number).toInt(),
            "the content card lost height beyond the navbar and the gutter",
        )
        assertTrue((innerWidthValue.toInt() - (contentCard["w"] as Number).toInt()) > 0, "the card fits the window")
    }

    @Test
    fun `light theme paints a light page`() {
        val page = open("")
        val light = luminance(page, "ktjs-demo-layout")
        assertTrue(light > 0.7, "content background luminance was $light in the light theme")
        assertNoHorizontalOverflow(page)
        assertOneScreen(page)
        assertDrawerHasNoScrollbar(page)
        assertShellIsCards(page)
        // The side nav paints the item carrying its `current` state attribute; exactly one must carry it.
        assertEquals(
            1,
            page.locator("ktjs-demo-layout vaadin-side-nav vaadin-side-nav-item[current]").count(),
            "exactly one navigation item is current",
        )
        // The title lives in the navbar, the way the app-layout examples put it there, not in the drawer.
        assertEquals("ktjs-dom-kit", page.locator("ktjs-demo-layout [slot='navbar'] .layout__title").textContent())
        assertEquals(0, page.locator("ktjs-demo-layout [slot='drawer'] .layout__title").count())
        // Every top-level row carries its Iconify icon in the prefix slot, and so does a group row; a
        // group's children deliberately carry none — the row above them already names the section.
        val drawer = "ktjs-demo-layout [slot='drawer']"
        val topLevelRows = "$drawer vaadin-side-nav-item[data-route]:not([slot])"
        val childRows = "$drawer vaadin-side-nav-item[data-route][slot='children']"
        assertTrue(page.locator(topLevelRows).count() > 0, "the drawer has top-level page rows")
        assertTrue(page.locator(childRows).count() > 0, "the drawer has page rows under a group")
        assertEquals(
            page.locator(topLevelRows).count(),
            page.locator("$topLevelRows iconify-icon[slot='prefix']").count(),
            "every top-level row has an icon",
        )
        assertEquals(
            0,
            page.locator("$childRows iconify-icon").count(),
            "a group's children carry no icon of their own",
        )
        // The wrapper is only decoration if the icon never renders: `iconify-icon.status` tells.
        val icon = page.locator("ktjs-demo-layout [slot='drawer'] iconify-icon").first().evaluate(
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
        pick(page, "theme:dark")
        page.waitForTimeout(400.0)
        val dark = luminance(page, "ktjs-demo-layout")
        assertTrue(dark < 0.2, "content background luminance was $dark in the dark theme")
        assertNoHorizontalOverflow(page)
        shoot(page, "dark-overview")
        page.close()
    }

    @Test
    fun `chinese switch repaints every string in place`() {
        val page = open("")
        pick(page, "lang:zh-Hans")
        page.waitForSelector("demo-overview-page .page__title:text-is('ktjs-dom-kit 概览')")
        // The menu closes with an animation; a screenshot taken mid-close shows a ghost overlay.
        page.keyboard().press("Escape")
        page.waitForTimeout(500.0)
        // The drawer is rewritten in place, not rebuilt: the row a one-page group lifted to the top
        // level and a row under a group both read Chinese now. The only remaining group row is "DOM",
        // whose label is the same word in both languages, so the rows are what the switch shows.
        assertEquals(
            "概览",
            page.locator("ktjs-demo-layout vaadin-side-nav vaadin-side-nav-item[data-route='overview'] span")
                .first().textContent(),
            "a lifted page row switches language",
        )
        assertEquals(
            "元素创建",
            page.locator("ktjs-demo-layout vaadin-side-nav vaadin-side-nav-item[data-route='elements'] span")
                .first().textContent(),
            "a group's child row switches language too",
        )
        assertNoHorizontalOverflow(page)
        shoot(page, "zh-overview")
        page.close()
    }

    @Test
    fun `the attributes page shows the wrapper section in both themes`() {
        val page = open("#/attributes")
        // Three sections drove the attribute layer before the wrapper capabilities arrived; the
        // fourth is the typed one (optional, literal vocabulary, subscription).
        assertEquals(
            4,
            page.locator("demo-attribute-page .section").count(),
            "the page carries four sections",
        )
        assertEquals(
            "vaadin-button",
            page.locator("demo-attribute-page .section").last().locator("vaadin-button").first()
                .evaluate("node => node.tagName.toLowerCase()") as String,
            "the target is a wrapper element, not a bespoke one",
        )
        assertNoHorizontalOverflow(page)
        shoot(page, "light-attributes")
        pick(page, "theme:dark")
        page.keyboard().press("Escape")
        page.waitForTimeout(500.0)
        shoot(page, "dark-attributes")
        page.close()
    }

    /**
     * The typed section is the wrapper capability in one screen: an absent attribute reads as `null`
     * (not `""`), a literal attribute answers from its table, and the subscription repaints — which
     * only happens when the browser's `MutationObserver` really delivered a change.
     */
    @Test
    fun `the typed section distinguishes null, vocabulary and subscriptions`() {
        val page = open("#/attributes")
        val section = page.locator("demo-attribute-page .section").last()
        val initial = section.textContent()
        assertTrue(initial.contains("aria-label = null"), "an absent attribute must read as null: $initial")
        assertTrue(initial.contains("data-density = compact"), "the first vocabulary entry is the absent value: $initial")
        assertTrue(initial.contains("observed = 0"), "nothing was written yet: $initial")

        // Buttons of the typed section: 0 is the target, 1 sets the label, 2 clears it, 3 sets the density.
        section.locator("vaadin-button").nth(1).click()
        waitForText(section, "aria-label = \"Close\"")
        waitForText(section, "observed = 1")

        section.locator("vaadin-button").nth(3).click()
        waitForText(section, "data-density = comfortable")

        section.locator("vaadin-button").nth(2).click()
        waitForText(section, "aria-label = null")
        waitForText(section, "observed = 2")

        page.close()
    }

    @Test
    fun `the styling page shows the built-selector section`() {
        val page = open("#/stylesheets")
        assertEquals(
            3,
            page.locator("demo-styling-page .section").count(),
            "adopted sheets, the three-step ladder, and the built selector",
        )
        assertNoHorizontalOverflow(page)
        shoot(page, "light-styling")
        page.close()
    }

    @Test
    fun `sidebar headings are labels and the counter uses a Lumo button`() {
        val page = open("#/custom-elements")

        // Groups are parents of the one side-nav menu, so a heading is a nav item rather than a bespoke
        // div or a browser-default button.
        assertEquals(
            "vaadin-side-nav-item",
            page.locator("ktjs-demo-layout vaadin-side-nav vaadin-side-nav-item").first()
                .evaluate("node => node.tagName.toLowerCase()") as String,
        )
        assertEquals(
            "vaadin-button",
            page.locator("demo-counter vaadin-button").first()
                .evaluate("node => node.tagName.toLowerCase()") as String,
            "the counter must use a Lumo button, not a native one",
        )

        shoot(page, "light-custom-elements")
        pick(page, "theme:dark")
        page.keyboard().press("Escape")
        page.waitForTimeout(500.0)
        shoot(page, "dark-custom-elements")
        page.close()
    }
}
