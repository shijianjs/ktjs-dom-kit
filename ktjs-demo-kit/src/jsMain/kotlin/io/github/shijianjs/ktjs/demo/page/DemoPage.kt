package io.github.shijianjs.ktjs.demo.page

import io.github.shijianjs.ktjs.demo.i18n.Lang
import io.github.shijianjs.ktjs.demo.nav.Page
import io.github.shijianjs.ktjs.demo.style.RESET
import io.github.shijianjs.ktjs.dom.element.invoke
import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementAdapter
import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementReferenceAdapter
import io.github.shijianjs.ktjs.dom.webcomponent.attachStyledShadow
import io.github.shijianjs.ktjs.vaadin.themablemixin.addTheme
import io.github.shijianjs.ktjs.vaadin.verticallayout.VerticalLayoutVariants
import io.github.shijianjs.ktjs.vaadin.verticallayout.VerticalLayoutElement
import io.github.shijianjs.ktjs.vaadin.verticallayout.createVerticalLayout
import js.array.jsArrayOf
import web.components.ShadowRoot
import web.cssom.CSSStyleSheet
import web.dom.Node
import web.dom.document
import web.html.HTMLHeadingElement
import web.html.HTMLParagraphElement
import web.html.HTMLElement
import web.html.HtmlTagName

/**
 * The frame of one page — title, summary, the links row and the [body] the page fills with sections.
 *
 * **A page extends this class and registers its own tag.** Everything that produces DOM in this demo
 * is a custom element, pages included: a page is a component like a counter or a styling box, it owns
 * its nodes and its text, and the router mounts it by tag instead of calling a function that returns
 * markup. A function returning a page is the one shape this architecture does not allow.
 */
open class DemoPage : CustomElementAdapter(), Page {
    private val titleElement: HTMLHeadingElement = HtmlTagName.h1 { classList.add(PAGE_TITLE) }

    private val summaryElement: HTMLParagraphElement = HtmlTagName.p { classList.add(PAGE_SUMMARY) }

    private val linksElement: HTMLElement = HtmlTagName.div { classList.add(PAGE_LINKS) }

    /**
     * The block a page appends its sections to.
     *
     * It is a `vaadin-vertical-layout` with `theme="spacing"`, not a plain `div`: a page appends several
     * section blocks, each of which is itself a vertical layout, and boxes that stack without a gap end
     * up touching (a card's bottom edge right against the next section's title). The gap belongs to the
     * container, so a page does not have to put a margin on every section it creates.
     */
    val body: VerticalLayoutElement = createVerticalLayout { addTheme(VerticalLayoutVariants.spacing) }.apply {
        // The page is the column: its body spans the frame instead of hugging its widest section.
        style.setProperty("width", "100%")
    }

    /** The layout mounts the page itself; a page is its own root node. */
    override val node: HTMLElement get() = this

    /** The frame's shadow root: the only style scope this page has. */
    private val frame: ShadowRoot = attachStyledShadow(RESET, PAGE_SHEET)

    /** Sheets a page added on top of the frame's own, kept so the adopted list can be rebuilt. */
    private val extraSheets = mutableListOf<CSSStyleSheet>()

    init {
        frame.append(titleElement, summaryElement, linksElement, body)
    }

    /** A page rewrites its own copy; the frame carries none of its own. */
    open override fun applyTranslation(lang: Lang) {}

    fun setTitle(text: String) {
        titleElement.textContent = text
    }

    fun setSummary(text: String) {
        summaryElement.textContent = text
    }

    /**
     * Adopts one more sheet into the page's shadow tree.
     *
     * A page that needs a rule the blocks cannot express — a `::part()` selector, a screen-reader-only
     * helper — declares it in its own file and hands it to the frame here. The rule then lives next to
     * the page that uses it instead of in a site-wide stylesheet, and the frame stays the only owner of
     * the shadow tree.
     */
    fun addSheet(sheet: CSSStyleSheet) {
        extraSheets += sheet
        frame.adoptedStyleSheets = jsArrayOf(RESET, PAGE_SHEET, *extraSheets.toTypedArray())
    }

    /** Adds one entry to the link row; the frame owns the layout and colour of that row. */
    fun addLink(node: Node) {
        if (node is HTMLElement) node.classList.add(PAGE_LINK)
        linksElement.append(node)
    }

    /**
     * Re-hosts one linked document stylesheet inside this page's shadow tree.
     *
     * A wrapper around a library that ships its own CSS (`index.html` links `lf/index.css`, a
     * calendar theme, a grid theme) has a problem the library cannot solve: document styles never
     * cross a shadow boundary, so the canvas renders unstyled — usually with no error at all. This
     * copies the rule text of the linked sheet into a sheet the page adopts, which is the only way
     * to keep both the `<link>` (that is how the file is shipped) and the shadow tree.
     *
     * Returns whether a sheet matched, so a page can say so in its status line instead of failing
     * silently.
     */
    fun addLinkedSheet(hrefFragment: String): Boolean {
        val list = document.styleSheets
        for (index in 0 until list.length) {
            val sheet = list.item(index) ?: continue
            val href = sheet.href ?: continue
            if (!href.contains(hrefFragment)) continue
            val text = StringBuilder()
            val rules = sheet.cssRules
            for (ruleIndex in 0 until rules.length) {
                val rule = rules.item(ruleIndex) ?: continue
                // Two things a copied rule loses. First, `:root` never matches inside a shadow tree,
                // so a theme that defines its tokens there (`--fc-classic-*`, `--pswp-*`) contributes
                // nothing and every `var(...)` in the rest of the sheet resolves to nothing — the
                // library renders uncoloured and borderless, with no error. Re-point it at `:host`.
                // Second, the sheet's base URL: `url(../fonts/x.woff)` would resolve against the page.
                text.append(absolutizeUrls(hostScoped(rule.cssText), href))
            }
            val copy = CSSStyleSheet()
            copy.replaceSync(text.toString())
            addSheet(copy)
            return true
        }
        return false
    }

    companion object : CustomElementReferenceAdapter<DemoPage>(DemoPage::class.js, "ktjs-demo-page") {
        /** `new URL(value, base).href`: the browser's own resolver, so a relative path stays correct. */
        private val resolveUrl: (String, String) -> String = js("(value, base) => new URL(value, base).href")

        /**
         * Re-points `:root` at `:host` in one rule.
         *
         * A library's theme declares its design tokens on `:root`; inside a shadow tree that selector
         * matches nothing, so the tokens never exist and every `var(--token)` in the rest of the sheet
         * is invalid at computed-value time — the widget renders uncoloured, without borders, and
         * without an error. `:host` is the same element as far as the page is concerned, and the tokens
         * inherit down the tree.
         *
         * `:root` only ever appears as a selector here, so a plain replacement is exact.
         */
        private fun hostScoped(css: String): String =
            if (css.contains(":root")) css.replace(":root", ":host") else css

        /**
         * Rewrites every relative `url(...)` in one rule to an absolute one, resolved against [base].
         *
         * Only values the browser would resolve relative to the stylesheet are rewritten: `data:` URIs,
         * fragment references and absolute URLs stay as they are.
         */
        private fun absolutizeUrls(css: String, base: String): String {
            if (!css.contains("url(")) return css
            val out = StringBuilder(css.length + 64)
            var index = 0
            while (index < css.length) {
                val start = css.indexOf("url(", index)
                if (start < 0) {
                    out.append(css, index, css.length)
                    break
                }
                val end = css.indexOf(')', start)
                if (end < 0) {
                    out.append(css, index, css.length)
                    break
                }
                out.append(css, index, start)
                val raw = css.substring(start + 4, end).trim().trim('"', '\'')
                val absolute = when {
                    raw.startsWith("data:") || raw.startsWith("#") || raw.contains("://") -> raw
                    else -> resolveUrl(raw, base)
                }
                out.append("url(\"").append(absolute).append("\")")
                index = end + 1
            }
            return out.toString()
        }
    }
}
