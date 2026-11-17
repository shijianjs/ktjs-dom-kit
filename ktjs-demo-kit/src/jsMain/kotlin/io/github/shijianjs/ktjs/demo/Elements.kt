package io.github.shijianjs.ktjs.demo

import io.github.shijianjs.ktjs.dom.element.createElement
import web.cssom.ClassName
import web.dom.Node
import web.dom.TagName
import web.html.HTMLElement
import web.html.Hidden
import web.html.`false`
import web.html.`true`

/**
 * Element helpers shared by the shell and the pages.
 *
 * They are generic: they create a node from a [TagName], set a class or a Vaadin `theme`, or hide a
 * node. Anything that knows what a page looks like (paragraphs, sections, cards, readouts) lives with
 * the page in [io.github.shijianjs.ktjs.demo.page].
 */

/** Creates an element of [tag], optionally with [className], and appends [children]. */
fun <T : HTMLElement> element(tag: TagName<T>, className: ClassName? = null, vararg children: Node): T =
    tag.createElement {
        if (className != null) classList.add(className)
        if (children.isNotEmpty()) append(*children)
    }

/** Adds [className] and returns the element, so calls can chain. */
fun <T : HTMLElement> T.withClass(className: ClassName): T {
    classList.add(className)
    return this
}

/** Sets Vaadin's `theme` attribute from [variants]; no variants removes it. */
fun <T : HTMLElement> T.withTheme(vararg variants: String): T {
    if (variants.isEmpty()) {
        removeAttribute("theme")
    } else {
        setAttribute("theme", variants.joinToString(" "))
    }
    return this
}

/** Shows or hides the element with the native `hidden` property. */
fun HTMLElement.setHidden(hidden: Boolean) {
    this.hidden = if (hidden) Hidden.Companion.`true` else Hidden.Companion.`false`
}
