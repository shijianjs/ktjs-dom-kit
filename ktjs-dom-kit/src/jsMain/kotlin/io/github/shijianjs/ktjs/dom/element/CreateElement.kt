package io.github.shijianjs.ktjs.dom.element

import web.dom.Document
import web.dom.TagName
import web.dom.document
import web.html.HTMLElement

/**
 * Creates an element of this tag and applies [block] to it.
 *
 * Wraps the typed `Document.createElement(TagName<T>)` — the tag name decides the element type, so
 * the callback receives `T` without a cast:
 *
 * ```kotlin
 * val link = TagName<HTMLAnchorElement>("a").createElement {
 *     href = "/docs/intro"
 *     textContent = "Get started"
 * }
 * ```
 *
 * [dom] defaults to the page's document; pass another one when building a detached document.
 */
fun <T : HTMLElement> TagName<T>.createElement(
    dom: Document = document,
    block: T.() -> Unit = {},
): T = dom.createElement(this).apply(block)

/**
 * [createElement] spelled as a call on the tag itself, for the places where the tag is the subject
 * of the sentence rather than a factory being invoked:
 *
 * ```kotlin
 * val card = HtmlTagName.div {
 *     classList.add("card")
 *     textContent = "Body"
 * }
 * ```
 *
 * Same element, same block, same default document — only the spelling differs. `createElement` stays
 * the explicit form for the cases this one cannot express: passing [Document] or naming the call
 * `createElement` in a chain that already reads as one.
 */
operator fun <T : HTMLElement> TagName<T>.invoke(
    block: T.() -> Unit = {},
): T = createElement(block = block)
