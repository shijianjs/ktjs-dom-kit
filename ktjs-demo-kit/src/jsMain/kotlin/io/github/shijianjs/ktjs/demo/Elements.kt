package io.github.shijianjs.ktjs.demo

import web.html.HTMLElement
import web.html.Hidden
import web.html.`false`
import web.html.`true`

/**
 * Element helpers shared by the shell and the pages.
 *
 * Only the two things the platform makes awkward: Vaadin's `theme` is a space-joined attribute
 * (`removeAttribute` when there are no variants), and the native `hidden` property is the `Hidden`
 * enumeration rather than a `Boolean`. Creating nodes needs no helper — `TagName<T>.createElement { }`
 * already returns the typed element with the builder's receiver, so `textContent` / `classList` /
 * `append` are written straight on it.
 */

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
