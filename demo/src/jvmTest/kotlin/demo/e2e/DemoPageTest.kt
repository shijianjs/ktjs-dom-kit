package demo.e2e

import com.microsoft.playwright.Browser
import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.junit.UsePlaywright
import com.microsoft.playwright.options.WaitForSelectorState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The acceptance rules every demo site of this account has to meet, asserted in a real browser:
 *
 * - one screen, not a stack of sections: sidebar with groups, top bar with a breadcrumb, the link /
 *   language / theme menus in the action row, and a content area that swaps one page at a time;
 * - `#/<slug>` deep links, navigation highlighting and `document.title` following the route;
 * - a language switch that rewrites text in place and keeps page state;
 * - a theme menu that flips the document between light, dark and the browser's own preference;
 * - no stylesheet of ours on `document`, no inline styling in the page body, no console errors.
 *
 * The menus are driven through their own elements — a button opens the menu, and an entry is picked by
 * the `data-action` it carries — so a test never depends on which label happens to be translated.
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
        page.waitForSelector("ktjs-demo-shell vaadin-app-layout")
        // The shell builds its menus in mount(), one task after the element itself upgrades;
        // measuring or clicking before they exist makes a test depend on timing, not on behaviour.
        page.locator("ktjs-demo-shell vaadin-menu-bar-item[data-menu='theme']").first().waitFor()
        waitForIconsRendered(page)
        page.setDefaultTimeout(7_000.0)
        return page
    }

    private fun assertNoErrors() {
        assertEquals(emptyList<String>(), consoleErrors)
    }

    /**
     * Waits until every icon of the shell has rendered.
     *
     * Iconify loads asynchronously and an icon that has not rendered yet has no width, so measuring the
     * navbar before that gives a narrower action row than the one the user sees. The locator has to be
     * used instead of `querySelectorAll`: menu-bar buttons live in the menu's own shadow tree, which a
     * plain document query cannot see into.
     */
    private fun waitForIconsRendered(page: Page) {
        val icons = page.locator("ktjs-demo-shell iconify-icon")
        val deadline = System.currentTimeMillis() + 7_000
        while (System.currentTimeMillis() < deadline) {
            val states = icons.evaluateAll("icons => icons.map((icon) => icon.status)") as List<*>
            if (states.isNotEmpty() && states.all { it == "rendered" }) return
            Thread.sleep(50)
        }
        error("some icons never rendered: ${icons.evaluateAll("icons => icons.map((icon) => icon.status)")}")
    }

    /** Opens the menu bar of [menu] (`docs`, `language` or `theme`) by clicking its button. */
    private fun openMenu(page: Page, menu: String) {
        page.locator("ktjs-demo-shell vaadin-menu-bar-item[data-menu='$menu']")
            .first()
            .click()
        // The entries are teleported into the menu's overlay; waiting for one keeps the next
        // step independent of the opening animation.
        page.locator("vaadin-menu-bar-item[data-action]:visible").first().waitFor()
    }

    /** Picks the entry carrying [action], e.g. `theme:dark`, from the menu named by its prefix. */
    private fun pick(page: Page, action: String) {
        openMenu(page, action.substringBefore(':'))
        page.locator("vaadin-menu-bar-item[data-action='$action']").first().click()
        // Clicking only starts the overlay's closing animation. While it is still on screen the action
        // row measures ~18px narrower (one icon slot), so geometry read straight after the click is
        // an in-flight value rather than the layout the user ends up looking at.
        page.locator("vaadin-menu-bar-item[data-action]").first().waitFor(
            Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN),
        )
    }

    /**
     * Geometry of the right-hand side of the navbar: the action row's box and where the breadcrumbs
     * start. Choosing a menu entry must not move either — the buttons are fixed nouns and only the
     * icon and the check mark change.
     */
    private fun actionGeometry(page: Page): List<Double> =
        (page.locator("ktjs-demo-shell [slot='navbar']").first().evaluate(
            """
            (navbar) => {
              const actions = navbar.querySelector('.shell__actions').getBoundingClientRect();
              const crumbs = navbar.querySelector('vaadin-breadcrumbs').getBoundingClientRect();
              return [actions.width, actions.x, crumbs.x];
            }
            """.trimIndent(),
        ) as List<*>).map { (it as Number).toDouble() }

    @Test
    fun `the title sits in the navbar, not in the drawer`() {
        val page = open("")

        assertEquals("ktjs-dom-kit", page.locator("ktjs-demo-shell [slot='navbar'] .shell__title").textContent())
        assertEquals(0, page.locator("ktjs-demo-shell [slot='drawer'] .shell__title").count())
        assertNoErrors()
    }

    @Test
    fun `the navbar carries a logo immediately left of the title`() {
        val page = open("")

        val logo = page.locator("ktjs-demo-shell [slot='navbar'] .shell__logo")
        assertEquals(1, logo.count(), "the nav bar has one site mark")
        // The mark is the repository logo (the same file the README and the docs site show), not an
        // Iconify glyph: if the build ever stops copying it, this fails instead of drawing a broken box.
        assertEquals("IMG", logo.evaluate("el => el.tagName"))
        assertEquals("img/ktjs-dom-kit-logo.png", logo.getAttribute("src"))
        assertTrue((logo.evaluate("el => el.naturalWidth") as Number).toInt() > 0, "the mark was loaded")
        val logoBox = logo.boundingBox()!!
        val titleBox = page.locator("ktjs-demo-shell [slot='navbar'] .shell__title").boundingBox()!!
        assertTrue(logoBox.x + logoBox.width <= titleBox.x + 1.0, "the mark sits left of the title")
        assertTrue(logoBox.height <= 30.0, "the mark stays navbar-sized, got ${logoBox.height}px")
        assertNoErrors()
    }

    /**
     * The action row is the last thing in the bar, so it needs an inset of its own: without it the
     * theme menu sits flush against the window edge (and the scrollbar).
     */
    @Test
    fun `the action row keeps a margin from the window edge`() {
        val page = open("")

        val actions = page.locator("ktjs-demo-shell [slot='navbar'] .shell__actions").boundingBox()!!
        val windowWidth = (page.evaluate("() => window.innerWidth") as Number).toDouble()
        val gap = windowWidth - (actions.x + actions.width)
        assertTrue(gap >= 8.0, "the last menu is ${gap}px from the window edge, expected at least 8px")
        assertNoErrors()
    }

    /**
     * The navbar is one line: the toggle, the mark, the title, its version tag and the breadcrumbs all
     * share the same vertical centre, and so do the two halves of the bar.
     */
    @Test
    fun `navbar children share one vertical centre`() {
        val page = open("")

        val centres = page.locator("ktjs-demo-shell [slot='navbar']").first().evaluate(
            """
            (navbar) => {
              const centre = (node) => {
                const rect = node.getBoundingClientRect();
                return (rect.top + rect.bottom) / 2;
              };
              const box = (node) => {
                const rect = node.getBoundingClientRect();
                return [node.tagName.toLowerCase(), Math.round(rect.top), Math.round(rect.height)];
              };
              const start = navbar.querySelector('.shell__topbar-start');
              return {
                bar: centre(navbar),
                halves: [...navbar.children].map(centre),
                start: [...start.children].map(centre),
                startBox: [...start.children].map(box),
                startHeight: start.getBoundingClientRect().height,
              };
            }
            """.trimIndent(),
        ) as Map<*, *>

        fun spread(values: List<Double>): Double = values.max() - values.min()

        val bar = (centres["bar"] as Number).toDouble()
        val halves = (centres["halves"] as List<*>).map { (it as Number).toDouble() }
        val start = (centres["start"] as List<*>).map { (it as Number).toDouble() }
        val boxes = centres["startBox"]
        assertTrue(spread(halves + bar) <= 1.0, "navbar halves were off-centre: $halves vs $bar")
        assertTrue(
            spread(start + bar) <= 1.0,
            "toggle, mark, title, version and breadcrumbs were off-centre: $start; navbar=$bar, " +
                "boxes=$boxes, startHeight=${centres["startHeight"]}",
        )
        assertNoErrors()
    }

    /**
     * The tab icon is the site logo — the same PNG the docs navbar and the README use, shrunk by the
     * `scaleLogo` build step. A `<link>` that points at a file the build never copies would leave the
     * browser showing its default globe, so the test also fetches it and checks the bytes arrive.
     */
    /**
     * The shell raises Lumo's corner radii one step, and the three steps stay distinct: `s` is the
     * stock 0.25em, `m` (and the base it derives from) is 0.5em, `l` is 1em. `m` is declared explicitly
     * because a few components redeclare it on their own `:host`; inheriting only the base left those
     * corners at the old value. The assertion reads the values a real component ends up with, not the
     * tokens alone: 14px body size ⇒ 3.5 / 7 / 14.
     */
    @Test
    fun `the shell rounds one step larger than Lumo's defaults`() {
        val page = open("#/elements")

        val tokens = page.locator("ktjs-demo-shell").first().evaluate(
            """
            (shell) => {
              const style = getComputedStyle(shell);
              return ['--lumo-border-radius-s', '--lumo-border-radius', '--lumo-border-radius-m',
                      '--lumo-border-radius-l'].map((name) => style.getPropertyValue(name).trim());
            }
            """.trimIndent(),
        ) as List<*>
        assertEquals(listOf("0.25em", "0.5em", "0.5em", "1em"), tokens)

        val cardRadius = page.locator("vaadin-card").first()
            .evaluate("node => getComputedStyle(node).borderRadius") as String
        val buttonRadius = page.locator("vaadin-button").first()
            .evaluate("node => getComputedStyle(node).borderRadius") as String
        assertEquals("7px", cardRadius, "a card did not take the doubled middle radius")
        assertEquals("7px", buttonRadius, "a button did not take the doubled middle radius")

        // `s` must stay visibly smaller than `m`, otherwise the three steps collapse into two values.
        val widths = listOf("s", "", "l").map { step ->
            val name = if (step.isEmpty()) "--lumo-border-radius" else "--lumo-border-radius-$step"
            page.locator("ktjs-demo-shell").first()
                .evaluate(
                    "(shell, name) => parseFloat(getComputedStyle(shell).getPropertyValue(name)) * 14",
                    name,
                ) as Number
        }
        assertTrue(
            widths[0].toDouble() < widths[1].toDouble() && widths[1].toDouble() < widths[2].toDouble(),
            "the radius steps are not ordered: $widths",
        )
        assertNoErrors()
    }

    @Test
    fun `the browser tab shows the site logo`() {
        val page = open("")

        val href = page.evaluate("document.querySelector('link[rel=\"icon\"]')?.href") as String?
        assertTrue(
            href != null && href.endsWith("/img/ktjs-dom-kit-logo.png"),
            "the tab icon does not point at the logo: $href",
        )
        val bytes = page.evaluate(
            """
            async () => {
              const response = await fetch(document.querySelector('link[rel="icon"]').href);
              return response.ok ? (await response.blob()).size : -1;
            }
            """.trimIndent(),
        ) as Number
        assertTrue(bytes.toInt() > 0, "the tab icon did not load: $bytes bytes")
        assertNoErrors()
    }

    /**
     * A dropdown entry is teleported into the menu's overlay and hidden again when the menu closes.
     * `iconify-icon` throws its own `<svg>` away on every "not visible" report while leaving
     * `status === "rendered"` behind, so without `noobserver` the entries come back blank on the
     * second open: the element is there, its shadow tree is empty.
     */
    @Test
    fun `a menu keeps the icons of its entries when reopened`() {
        val page = open("")

        fun shadowIconCounts(): List<Int> =
            (page.locator("vaadin-menu-bar-item[data-action]:visible").evaluateAll(
                "items => items.map(item => item.querySelector('iconify-icon')?.shadowRoot?.childElementCount ?? -1)",
            ) as List<*>).map { (it as Number).toInt() }

        openMenu(page, "lang")
        assertEquals(listOf(1, 1), shadowIconCounts(), "the first open lost an entry icon")

        page.keyboard().press("Escape")
        page.locator("vaadin-menu-bar-item[data-action]").first().waitFor(
            Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN),
        )
        openMenu(page, "lang")
        assertEquals(listOf(1, 1), shadowIconCounts(), "the second open lost an entry icon")
        assertNoErrors()
    }

    @Test
    fun `choosing a theme does not resize or move the action row`() {
        val page = open("")

        val before = actionGeometry(page)
        pick(page, "theme:dark")
        assertEquals("dark", page.evaluate("document.documentElement.getAttribute('theme')") as String)
        assertEquals(before, actionGeometry(page), "the theme menu resized or shifted the navbar")

        pick(page, "theme:light")
        assertEquals(null, page.evaluate("document.documentElement.getAttribute('theme')") as String?)
        assertEquals(before, actionGeometry(page))

        pick(page, "theme:system")
        assertEquals("light-dark", page.evaluate("document.documentElement.getAttribute('theme')") as String)
        assertEquals(before, actionGeometry(page))
        assertNoErrors()
    }

    /** The background the whole site sits on; `body` itself is transparent under Lumo. */
    private fun shellBackground(page: Page): String =
        page.locator("ktjs-demo-shell").first()
            .evaluate("node => getComputedStyle(node).backgroundColor") as String

    /**
     * Following the browser is a real option, not a label: with `theme="light-dark"` Lumo resolves
     * `prefers-color-scheme` itself, so emulating a dark browser repaints the whole site without any
     * JavaScript of ours running.
     */
    @Test
    fun `the follow-browser theme tracks prefers-color-scheme`() {
        val page = open("")
        pick(page, "theme:system")

        page.emulateMedia(Page.EmulateMediaOptions().setColorScheme(com.microsoft.playwright.options.ColorScheme.DARK))
        val dark = luminance(shellBackground(page))
        assertTrue(dark < 0.2, "a dark browser did not darken the site: $dark")

        page.emulateMedia(Page.EmulateMediaOptions().setColorScheme(com.microsoft.playwright.options.ColorScheme.LIGHT))
        val light = luminance(shellBackground(page))
        assertTrue(light > 0.7, "a light browser did not lighten the site: $light")
        assertNoErrors()
    }

    @Test
    fun `shell upgrades and keeps our sheets inside shadow roots`() {
        val page = open("")

        assertEquals(0, (page.evaluate("document.adoptedStyleSheets.length") as Number).toInt())
        assertTrue(page.evaluate("customElements.get('ktjs-demo-shell') !== undefined") as Boolean)
        assertTrue(page.evaluate("customElements.get('ktjs-demo-page') !== undefined") as Boolean)
        assertEquals(
            0,
            (page.evaluate("document.querySelectorAll('ktjs-demo-page [style]').length") as Number).toInt(),
            "page content is styled by sheets, not by inline declarations",
        )
        assertNoErrors()
    }

    @Test
    fun `the action row offers links, languages and themes as menus`() {
        val page = open("")

        assertEquals(3, page.locator("ktjs-demo-shell [slot='navbar'] vaadin-menu-bar").count())

        // The entries of a menu are materialised the first time it opens, and stay in the overlay
        // afterwards; the action prefix tells which menu they belong to.
        openMenu(page, "docs")
        assertEquals(3, page.locator("vaadin-menu-bar-item[data-action^='docs:']").count())
        page.keyboard().press("Escape")

        openMenu(page, "lang")
        assertEquals(2, page.locator("vaadin-menu-bar-item[data-action^='lang:']").count())
        page.keyboard().press("Escape")

        openMenu(page, "theme")
        assertEquals(3, page.locator("vaadin-menu-bar-item[data-action^='theme:']").count())
        assertNoErrors()
    }

    @Test
    fun `deep link renders the page, the breadcrumb and the document title`() {
        val page = open("#/custom-elements")

        assertTrue(page.title().contains("Custom elements"), "title was: ${page.title()}")
        val breadcrumb = page.locator("ktjs-demo-shell vaadin-breadcrumbs").textContent().orEmpty()
        assertTrue(breadcrumb.contains("Custom elements"), "breadcrumb was: $breadcrumb")
        assertTrue(page.locator("ktjs-demo-page .page__title").textContent().orEmpty().isNotEmpty())
        assertEquals("#/custom-elements", page.evaluate("location.hash") as String)
        assertNoErrors()
    }

    @Test
    fun `navigation swaps exactly one page`() {
        val page = open("")

        assertTrue(page.locator("ktjs-demo-shell [slot='drawer'] vaadin-side-nav-item").count() >= 4)
        page.locator("[data-route='stylesheets']").first().click()
        page.waitForSelector("ktjs-demo-page .page__title:text-is('Stylesheets')")
        assertEquals(1, page.locator("ktjs-demo-page").count(), "the content area hosts one page")
        assertNoErrors()
    }

    @Test
    fun `language switch rewrites text and keeps page state`() {
        val page = open("#/custom-elements")

        val counter = page.locator("demo-counter").first()
        counter.locator("vaadin-button").click()
        counter.locator("vaadin-button").click()
        assertEquals("2", counter.locator(".counter__value").textContent())

        pick(page, "lang:zh-Hans")
        page.waitForSelector("ktjs-demo-page .page__title:text-is('自定义元素')")
        assertEquals(
            "2",
            page.locator("demo-counter").first().locator(".counter__value").textContent(),
            "the page cache kept the counter state across the language switch",
        )
        assertNoErrors()
    }

    @Test
    fun `a wrapper element is driven by an Attribute and its token list`() {
        val page = open("#/attributes")
        // Third section: the wrapper element and its `theme` token list. (The fourth section is the
        // typed one, so the readout is located inside the section rather than taken as `.last()`.)
        val readout = page.locator("ktjs-demo-page .section").nth(2).locator(".readout").last()

        page.locator("vaadin-button:text-is('toggle primary')").click()
        val added = readout.textContent().orEmpty()
        assertTrue(added.contains("[primary]"), "readout was: $added")

        // toggling the only token removes the attribute, so "no tokens" and "absent" are one state
        page.locator("vaadin-button:text-is('toggle primary')").click()
        val removed = readout.textContent().orEmpty()
        assertTrue(removed.contains("theme = []"), "readout was: $removed")
        assertTrue(removed.contains("attribute = missing"), "readout was: $removed")

        // a messy raw write is normalised on the next read instead of being stored as-is
        page.locator("vaadin-button:text-is('write messy text')").click()
        val messy = readout.textContent().orEmpty()
        assertTrue(messy.contains("[primary, small]"), "readout was: $messy")
        assertNoErrors()
    }
}

/** Relative luminance of an `rgb(…)`/`rgba(…)` colour; used to tell a light page from a dark one. */
private fun luminance(color: String): Double {
    val parts = color.removePrefix("rgb").removePrefix("rgba").trim('(', ')', ' ').split(',')
    fun channel(value: String): Double {
        val number = value.trim().toDouble()
        val normalized = if (value.contains('%')) number / 100.0 else number / 255.0
        return if (normalized <= 0.03928) normalized / 12.92 else Math.pow((normalized + 0.055) / 1.055, 2.4)
    }
    return 0.2126 * channel(parts[0]) + 0.7152 * channel(parts[1]) + 0.0722 * channel(parts[2])
}
