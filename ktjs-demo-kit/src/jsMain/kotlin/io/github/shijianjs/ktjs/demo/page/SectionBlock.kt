package io.github.shijianjs.ktjs.demo.page

import io.github.shijianjs.ktjs.dom.element.invoke
import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementAdapter
import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementReferenceAdapter
import io.github.shijianjs.ktjs.vaadin.themablemixin.addTheme
import io.github.shijianjs.ktjs.vaadin.verticallayout.VerticalLayoutElement
import io.github.shijianjs.ktjs.vaadin.verticallayout.VerticalLayoutVariants
import io.github.shijianjs.ktjs.vaadin.verticallayout.createVerticalLayout
import web.html.HTMLElement
import web.html.HtmlTagName

/**
 * One titled section, **as an element**: heading, optional hint, and the [body] its examples go into.
 *
 * The element owns its three nodes, so nothing is assembled outside a class. Two naming rules show up
 * here, and both are the reason the fields are what they are:
 *
 * - The heading node is [heading], not `title`. `HTMLElement.title` is the browser's own tooltip
 *   property (a `String`), so a node called `title` cannot be declared on an element at all — a name
 *   is cheaper to rename than an architecture is to bend around it.
 * - A field a page binds through `Binder` is one word here on purpose (`hint`); anything that would
 *   trip over a built-in `HTMLElement` member gets a second word instead.
 *
 * It stays in the light DOM: its children are the page's own cards and grids, and they have to keep
 * receiving the page's sheet, which a shadow root would cut off. [show] appends the nodes instead of
 * the constructor doing it, because a custom element's constructor may not add children to itself.
 */
class SectionBlock : CustomElementAdapter() {
    /** The node a page binds its title text to. */
    val heading: HTMLElement = HtmlTagName.h2().apply { classList.add(SECTION_TITLE) }

    /** The node a page binds its hint text to; empty text hides it. */
    val hint: HTMLElement = HtmlTagName.p().apply { classList.add(SECTION_HINT) }

    /** The vertical group the examples go into; it keeps the component's gap between them. */
    val body: VerticalLayoutElement = createVerticalLayout { addTheme(VerticalLayoutVariants.spacing) }

    /** The section nodes themselves are this element; a page appends it to its body. */
    val root: HTMLElement get() = this

    /** Fills the section: the width, the three nodes and their text. */
    fun show(heading: String = "", hintText: String = "") {
        classList.add(SECTION)
        style.setProperty("width", "100%")
        // A vertical layout sizes its children to their content and aligns them to the start, so a
        // section would otherwise be as wide as its widest child — a grid or a row would then pick its
        // column count from that width instead of the page's. Sections always span the page.
        body.style.setProperty("width", "100%")
        append(this.heading, this.hint, body)
        setText(heading, hintText)
    }

    /** Writes the heading and the hint; an empty [hintText] hides the hint line. */
    fun setText(heading: String, hintText: String = "") {
        this.heading.textContent = heading
        this.hint.textContent = hintText
        this.hint.toggleAttribute("hidden", hintText.isEmpty())
    }

    companion object : CustomElementReferenceAdapter<SectionBlock>(SectionBlock::class.js, "ktjs-demo-section")
}
