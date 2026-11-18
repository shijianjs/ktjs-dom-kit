package io.github.shijianjs.ktjs.dom.element

import web.dom.ParentNode
import web.html.HTMLElement

/**
 * Appends this element to the parent in context and hands it back, so a subtree reads as
 * `+tag { … }` inside the block of the element that contains it:
 *
 * ```kotlin
 * val card = HtmlTagName.div {
 *     +HtmlTagName.h2 { textContent = "Title" }
 *     +HtmlTagName.p { textContent = "Body" }
 *     val note = +HtmlTagName.small { textContent = "…" }
 * }
 * ```
 *
 * The parent comes from the enclosing receiver, so `+` exists only where a [ParentNode] is in scope
 * — an element's block, most commonly. Anywhere else (`document.body`, a node held in a field, an
 * element that already exists) use `parent.appendChild(node)`: there is nothing to create, so the
 * two-part spelling is the honest one.
 *
 * Returning the node is what makes the operator useful in both positions: `+span { … }` on its own
 * appends and drops the value, `val x = +span { … }` keeps the child for later — to change its text
 * on the next tick, for instance — without a second `querySelector`.
 *
 * Previous siblings are kept in insertion order, exactly like repeated `appendChild` calls; nothing
 * is cleared first, so appending to a container that already has content adds to it.
 */
context(parent: ParentNode)
operator fun <T : HTMLElement> T.unaryPlus(): T {
    parent.appendChild(this)
    return this
}
