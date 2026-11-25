package io.github.shijianjs.ktjs.dom.css.dsl

import web.cssom.Selector

/*
 * A selector built from typed parts, not from a pasted string.
 *
 * The value is `web.cssom.Selector`, cssom's own opaque selector — the type `csstype.RuleBuilder`
 * keys a rule with — so a built selector is a rule key as it stands, with no conversion at the
 * boundary. The pieces are the node kinds a selector is made of (`postcss-selector-parser` names them
 * the same way): tag, universal, class, id, attribute, pseudo, and the nesting selector `&`;
 * combinators join them.
 *
 * ```kotlin
 * // .row.is-selected > vaadin-button[disabled]::part(label)
 * private val ROW = ClassName("row")
 * private val SELECTED = ClassName("is-selected")
 * private val BUTTON: TagName<HTMLButtonElement> = TagName("vaadin-button")
 *
 * compound(className(ROW), className(SELECTED))
 *     .child(
 *         compound(
 *             tag(BUTTON),
 *             attributeSelector(HtmlAttributes.disabled),
 *             pseudo(pseudoElement("part"), ident("label")),
 *         ),
 *     )
 * ```
 *
 * Every part is a value, so a selector can be a field, passed to a function, or stored in a catalog.
 * The one thing this engine does not do is **parse**: there is no factory for a full selector that
 * came from somewhere else — that would be the string this API exists to replace. `Selector(…)` takes
 * whatever text it is given, so a single piece cssom cannot type (an `:nth-child` formula, a
 * `::part` name) still goes through it at the point where it is spelled.
 *
 * Spacing follows CSS convention (`"a > b"`, `"a + b"`); the values themselves are never trimmed or
 * reformatted, so what you build is what stylis receives.
 */

/**
 * Glues simple selectors into one **compound** selector: no combinator, all conditions on the same
 * element. `compound(className(ROW), className(SELECTED))` is `.row.is-selected`.
 *
 * An empty list is a bug at the call site, not a selector, so it is reported as `""` plus nothing —
 * stylis drops an empty key, which shows up as a rule that never applies.
 */
fun compound(vararg parts: Selector): Selector = Selector(parts.joinToString(""))

/** A selector list: one rule, several selectors, joined by commas — `.a, .b`. */
fun selectorList(vararg selectors: Selector): Selector = Selector(selectors.joinToString(","))

/** Descendant combinator: `left right`. */
infix fun Selector.descendant(right: Selector): Selector = Selector("$this $right")

/** Child combinator: `left > right`. */
infix fun Selector.child(right: Selector): Selector = Selector("$this > $right")

/** Adjacent sibling combinator: `left + right`. */
infix fun Selector.nextSibling(right: Selector): Selector = Selector("$this + $right")

/** General sibling combinator: `left ~ right`. */
infix fun Selector.subsequentSibling(right: Selector): Selector = Selector("$this ~ $right")

/** Column combinator: `left || right`, for table columns selected as a grid column. */
infix fun Selector.column(right: Selector): Selector = Selector("$this || $right")
