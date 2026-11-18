package io.github.shijianjs.ktjs.dom.css.dsl

import web.cssom.ClassName
import web.dom.TagName

/**
 * The simple selectors a compound is made of, plus the two pieces that have no type of their own
 * (`*`, `&`).
 *
 * Each factory does one thing: it renders one node of a selector. Composition is the other file's
 * job ([compound], [selectorList], the combinators), so the two never drift into two half-DSLs.
 */

/** `vaadin-button` — a type selector. The typed `TagName` constants are the intended argument. */
fun tag(tagName: TagName<*>): CssSelector = CssSelector("$tagName")

/** `vaadin-button` — the same selector when the name is not available as a `TagName` constant. */
fun tag(name: String): CssSelector = CssSelector(name)

/** `.card` — a class selector; pass the constant the sheet and the DOM both use. */
fun className(className: ClassName): CssSelector = CssSelector(".$className")

/** `.card` — the same selector for a name that is not a constant yet. */
fun className(name: String): CssSelector = CssSelector(".$name")

/** `#main` — an id selector. */
fun id(value: String): CssSelector = CssSelector("#$value")

/**
 * `[part="label"]` — an attribute selector.
 *
 * [value] is the only optional part: without it the selector is the bare `[name]` presence test.
 * [operator] matches the six CSS comparison forms; [quote] chooses `[name="value"]` over
 * `[name=value]`, and [caseFlag] appends ` i` / ` s`. Only `i` and `s` exist, which is why they are an
 * enum rather than a string.
 */
fun attributeSelector(
    name: String,
    value: String? = null,
    operator: AttributeOperator = AttributeOperator.equals,
    quote: Boolean = true,
    caseFlag: AttributeCase? = null,
): CssSelector {
    if (value == null) return CssSelector("[$name]")
    val rendered = if (quote) "\"${escapeQuoted(value)}\"" else value
    val flag = caseFlag?.let { " ${it.token}" }.orEmpty()
    return CssSelector("[$name${operator.token}$rendered$flag]")
}

/**
 * `:hover` / `::before` — a pseudo-class or pseudo-element.
 *
 * [name] is written without the colons. Two forms take an argument: a string for what CSS does not
 * type (a `:nth-child` formula, a `::part(name)` identifier) and a [CssSelector] for what it does —
 * `:not(.is-hidden)`, `:is(.a, .b)`, `:host([collapsed])`.
 */
fun pseudo(
    name: String,
    element: Boolean = false,
): CssSelector = CssSelector(if (element) "::$name" else ":$name")

fun pseudo(
    name: String,
    argument: String,
    element: Boolean = false,
): CssSelector = CssSelector((if (element) "::" else ":") + name + "(" + argument + ")")

fun pseudo(
    name: String,
    argument: CssSelector,
    element: Boolean = false,
): CssSelector = pseudo(name, "$argument", element)

/**
 * A `Pseudo` (a variant already attached to its parent rule, like `Pseudos.hover`) as a standalone
 * simple selector: the leading `&` is dropped, so `Pseudos.hover.asCssSelector()` is `:hover` and can
 * be composed into a selector of its own.
 */
fun Pseudo.asCssSelector(): CssSelector {
    val text = "$this"
    return CssSelector(if (text.startsWith("&")) text.substring(1) else text)
}

/** The selectors that are a single token and have no factory of their own. */
object Selectors {
    /** `*` — every element. */
    val universal: CssSelector = CssSelector("*")

    /** `&` — the nesting selector, for building rule keys that stay attached to their parent. */
    val nesting: CssSelector = CssSelector("&")

    /** `:host` — the shadow host of a custom element. */
    val host: CssSelector = CssSelector(":host")

    /** `:root` — the document root. */
    val root: CssSelector = CssSelector(":root")

    /** `:scope` — the element a query was called on. */
    val scope: CssSelector = CssSelector(":scope")
}

/** The six attribute comparison operators; `token` is what CSS spells, escaped where it starts with `$`. */
enum class AttributeOperator(internal val token: String) {
    /** `=` — exactly equal. */
    equals("="),

    /** `~=` — one of the space-separated words. */
    includes("~="),

    /** `|=` — equal, or starts with the value plus `-`. */
    dashMatch("|="),

    /** `^=` — starts with. */
    prefix("^="),

    /** `$=` — ends with. */
    suffix("\$="),

    /** `*=` — contains. */
    substring("*="),
}

/** ` i` / ` s` — how the attribute value is compared (ASCII case-insensitive or case-sensitive). */
enum class AttributeCase(internal val token: String) {
    /** `i` — ASCII case-insensitive. */
    insensitive("i"),

    /** `s` — case-sensitive. */
    sensitive("s"),
}

/** Backslash and double quote are the two characters that end a quoted attribute value early. */
private fun escapeQuoted(value: String): String = value
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
