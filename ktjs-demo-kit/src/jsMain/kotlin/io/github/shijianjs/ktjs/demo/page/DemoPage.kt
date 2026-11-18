package io.github.shijianjs.ktjs.demo.page

import io.github.shijianjs.ktjs.demo.RESET
import io.github.shijianjs.ktjs.dom.element.createElement
import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementAdapter
import io.github.shijianjs.ktjs.dom.webcomponent.attachStyledShadow
import io.github.shijianjs.ktjs.dom.webcomponent.defineIfMissing
import io.github.shijianjs.ktjs.vaadin.themablemixin.addTheme
import io.github.shijianjs.ktjs.vaadin.theme.VerticalLayoutVariants
import io.github.shijianjs.ktjs.vaadin.verticallayout.VerticalLayoutElement
import io.github.shijianjs.ktjs.vaadin.verticallayout.createVerticalLayout
import web.components.CustomElementReference
import web.dom.Node
import web.dom.TagName
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
    private val titleElement: HTMLHeadingElement = HtmlTagName.h1.createElement { classList.add(PAGE_TITLE) }

    private val summaryElement: HTMLParagraphElement = HtmlTagName.p.createElement { classList.add(PAGE_SUMMARY) }

    private val linksElement: HTMLElement = HtmlTagName.div.createElement { classList.add(PAGE_LINKS) }

    /**
     * The block a page appends its sections to.
     *
     * It is a `vaadin-vertical-layout` with `theme="spacing"`, not a plain `div`: a page appends several
     * section blocks, each of which is itself a vertical layout, and boxes that stack without a gap end
     * up touching (a card's bottom edge right against the next section's title). The gap belongs to the
     * container, so a page does not have to put a margin on every section it creates.
     */
    val body: VerticalLayoutElement = createVerticalLayout { addTheme(VerticalLayoutVariants.spacing) }

    init {
        attachStyledShadow(RESET, PAGE_SHEET).append(titleElement, summaryElement, linksElement, body)
    }

    fun setTitle(text: String) {
        titleElement.textContent = text
    }

    fun setSummary(text: String) {
        summaryElement.textContent = text
    }

    /** Adds one entry to the link row; the frame owns the layout and colour of that row. */
    fun addLink(node: Node) {
        if (node is HTMLElement) node.classList.add(PAGE_LINK)
        linksElement.append(node)
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
    return DemoPage.NAME.createElement()
}
