package io.github.shijianjs.ktjs.dom.element

import web.dom.Document
import web.dom.TagName
import web.dom.document
import web.html.HTMLElement

/**
 * Creates an element of this tag and applies [apply] to it.
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
 * [dom] defaults to the page's document; pass another one when building inside a detached document.
 */
fun <T : HTMLElement> TagName<T>.createElement(
    dom: Document = document,
    apply: T.() -> Unit = {},
): T = dom.createElement(this).also(apply)
