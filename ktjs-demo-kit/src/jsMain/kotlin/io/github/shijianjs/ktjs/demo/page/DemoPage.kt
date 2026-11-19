package io.github.shijianjs.ktjs.demo.page

import io.github.shijianjs.ktjs.demo.style.RESET
import io.github.shijianjs.ktjs.dom.element.invoke
import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementAdapter
import io.github.shijianjs.ktjs.dom.webcomponent.attachStyledShadow
import io.github.shijianjs.ktjs.dom.webcomponent.defineIfMissing
import io.github.shijianjs.ktjs.vaadin.themablemixin.addTheme
import io.github.shijianjs.ktjs.vaadin.verticallayout.VerticalLayoutVariants
import io.github.shijianjs.ktjs.vaadin.verticallayout.VerticalLayoutElement
import io.github.shijianjs.ktjs.vaadin.verticallayout.createVerticalLayout
import web.components.CustomElementReference
import js.array.jsArrayOf
import web.components.ShadowRoot
import web.cssom.CSSStyleSheet
import web.dom.Node
import web.dom.TagName
import web.dom.document
import web.html.HTMLHeadingElement
import web.html.HTMLParagraphElement
import web.html.HTMLElement
import web.html.HtmlTagName

/**
 * `<ktjs-demo-page>`: the frame one page is built inside — title, summary, the links row and the body
 * slot the page fills with sections.
 *
 * It is one element for **every** page of the site, not one class per page: the pages differ in
 * content, not in their frame, and a site with ninety pages should not carry ninety tags. The body
 * lives in this element's shadow tree, so the page's own sheet styles it and nothing leaks to the
 * host page.
 *
 * This is a container, not a base class: a page creates it, fills [body] and translates its own
 * strings. Nothing extends it.
 */
class DemoPage : CustomElementAdapter() {
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

    /** The frame's shadow root: the only style scope this page has. */
    private val frame: ShadowRoot = attachStyledShadow(RESET, PAGE_SHEET)

    /** Sheets a page added on top of the frame's own, kept so the adopted list can be rebuilt. */
    private val extraSheets = mutableListOf<CSSStyleSheet>()

    init {
        frame.append(titleElement, summaryElement, linksElement, body)
    }

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
                text.append(rule.cssText)
            }
            val copy = CSSStyleSheet()
            copy.replaceSync(text.toString())
            addSheet(copy)
            return true
        }
        return false
    }

    companion object : CustomElementReference<DemoPage>(DemoPage::class.js) {
        val NAME: TagName<DemoPage> = TagName("ktjs-demo-page")

        /** Defines the tag once; every page creation goes through [newPage]. */
        fun define() {
            defineIfMissing(NAME, this)
        }
    }
}

/** Creates an empty page frame; [DemoPage.define] has to have run. */
fun newPage(): DemoPage {
    DemoPage.define()
    return DemoPage.NAME()
}
