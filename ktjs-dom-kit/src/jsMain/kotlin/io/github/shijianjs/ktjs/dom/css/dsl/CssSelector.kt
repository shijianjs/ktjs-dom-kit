package io.github.shijianjs.ktjs.dom.css.dsl

import csstype.RuleBuilder
import js.reflect.unsafeCast
import web.cssom.Selector

/**
 * A selector built from typed parts, not from a pasted string.
 *
 * Shaped like `web.cssom.Selector` and [Pseudo] — an opaque value with constructor-like factories,
 * rendered to CSS text once, when it becomes a rule key. The pieces are the node kinds a selector is
 * made of (`postcss-selector-parser` names them the same way): tag, universal, class, id, attribute,
 * pseudo, and the nesting selector `&`; combinators join them.
 *
 * ```kotlin
 * // .row.is-selected > vaadin-button::part(label)
 * compound(className(ROW), className(SELECTED))
 *     .child(compound(tag("vaadin-button"), pseudo("part", "label", element = true)))
 * ```
 *
 * Every part is a value, so a selector can be a field, passed to a function, or stored in a catalog.
 * The one thing this engine does not do is **parse**: there is no `CssSelector("…")` for a full
 * selector that came from somewhere else — that would be the string this API exists to replace. The
 * factory takes whatever text it is given, so a single piece cssom cannot type (an `:nth-child`
 * formula, a `::part` name) still goes through it at the point where it is spelled.
 *
 * Spacing follows CSS convention (`"a > b"`, `"a + b"`); the values themselves are never trimmed or
 * reformatted, so what you build is what stylis receives.
 */
external interface CssSelector

inline fun CssSelector(value: String): CssSelector = unsafeCast(value)

/** The `web.cssom.Selector` a rule key needs; the conversion point of this whole file. */
fun CssSelector.asSelector(): Selector = Selector("$this")

/**
 * Glues simple selectors into one **compound** selector: no combinator, all conditions on the same
 * element. `compound(className(ROW), className(SELECTED))` is `.row.is-selected`.
 *
 * An empty list is a bug at the call site, not a selector, so it is reported as `""` plus nothing —
 * stylis drops an empty key, which shows up as a rule that never applies.
 */
fun compound(vararg parts: CssSelector): CssSelector = CssSelector(parts.joinToString(""))

/** A selector list: one rule, several selectors, joined by commas — `.a, .b`. */
fun selectorList(vararg selectors: CssSelector): CssSelector = CssSelector(selectors.joinToString(","))

/** Descendant combinator: `left right`. */
infix fun CssSelector.descendant(right: CssSelector): CssSelector = CssSelector("$this $right")

/** Child combinator: `left > right`. */
infix fun CssSelector.child(right: CssSelector): CssSelector = CssSelector("$this > $right")

/** Adjacent sibling combinator: `left + right`. */
infix fun CssSelector.nextSibling(right: CssSelector): CssSelector = CssSelector("$this + $right")

/** General sibling combinator: `left ~ right`. */
infix fun CssSelector.subsequentSibling(right: CssSelector): CssSelector = CssSelector("$this ~ $right")

/** Column combinator: `left || right`, for table columns selected as a grid column. */
infix fun CssSelector.column(right: CssSelector): CssSelector = CssSelector("$this || $right")

/**
 * A rule key, written the way the pseudo helpers are: the context parameter picks up the enclosing
 * `RuleBuilder`, so a built selector is invoked directly in `cssSheet { … }`.
 *
 * ```kotlin
 * cssSheet {
 *     (className(ROW) child className(CELL)) { padding = Padding(8.px) }
 * }
 * ```
 *
 * Requires Kotlin 2.2+ context parameters (no compiler flag in 2.4). Import `…css.dsl.invoke` at
 * call sites, as with any operator extension.
 */
context(builder: RuleBuilder<T>)
inline operator fun <T : Any> CssSelector.invoke(
    block: T.() -> Unit,
) {
    builder.apply {
        this@invoke.asSelector()(block)
    }
}
