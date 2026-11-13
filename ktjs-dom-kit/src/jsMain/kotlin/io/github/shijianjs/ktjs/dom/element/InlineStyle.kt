package io.github.shijianjs.ktjs.dom.element

import io.github.shijianjs.ktjs.dom.webcomponent.attachStyledShadow
import csstype.Properties
import js.reflect.unsafeCast
import web.html.HTMLElement

/**
 * This element's inline style, typed.
 *
 * `HTMLElement.style` comes from lib.dom, where every property is a plain `String`. [Properties] is
 * kotlin-csstype's view of the very same `CSSStyleDeclaration` object, so this is a cast to a richer
 * interface over the same data — it lets a component write its own box and colours with real types
 * (`Display.flex`, `1.5.rem`, `Color(…)`, `Padding(top, horizontal, bottom)`) instead of hand-built
 * strings:
 *
 * ```kotlin
 * card.inlineStyle.apply {
 *     display = Display.flex
 *     padding = Padding(1.5.rem)
 *     borderRadius = 1.rem
 * }
 * ```
 *
 * Style belongs here, on the node that owns it. A stylesheet is for what inline styles cannot say
 * (`:hover`, `::before`, media queries) — see [attachStyledShadow] — and adoption is for what
 * several components really share — the same call with more sheets.
 */
val HTMLElement.inlineStyle: Properties
    get() = unsafeCast(style)
