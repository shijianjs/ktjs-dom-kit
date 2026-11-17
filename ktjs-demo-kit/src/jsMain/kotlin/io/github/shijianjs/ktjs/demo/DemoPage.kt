package io.github.shijianjs.ktjs.demo

import io.github.shijianjs.ktjs.dom.element.createElement
import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementAdapter
import io.github.shijianjs.ktjs.dom.webcomponent.attachStyledShadow
import io.github.shijianjs.ktjs.dom.webcomponent.defineIfMissing
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

    /** The block a page appends its sections to. */
    val body: HTMLElement = HtmlTagName.div.createElement { classList.add(PAGE_BODY) }

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
