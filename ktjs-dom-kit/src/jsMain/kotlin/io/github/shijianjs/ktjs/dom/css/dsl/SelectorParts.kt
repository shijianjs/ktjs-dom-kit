package io.github.shijianjs.ktjs.dom.css.dsl

import web.cssom.ClassName
import web.cssom.HtmlAttributes
import web.cssom.Ident
import web.cssom.Selector
import web.dom.TagName

/**
 * The simple selectors a compound is made of, plus the two pieces that have no type of their own
 * (`*`, `&`).
 *
 * Each factory does one thing: it renders one node of a selector. Composition is the other file's
 * job ([compound], [selectorList], the combinators), so the two never drift into two half-DSLs.
 */

/** `vaadin-button` — a type selector. The name has to be a [TagName], not a string. */
fun tag(tagName: TagName<*>): Selector = Selector("$tagName")

/** `.card` — a class selector; pass the constant the sheet and the DOM both use. */
fun className(className: ClassName): Selector = Selector(".$className")

/** `#main` — an id selector. */
fun id(value: String): Selector = Selector("#$value")

/**
 * `[disabled]` — an attribute selector for a name the generated catalogue knows
 * (`web.cssom.HtmlAttributes`, 210 entries: `class`, `part`, `type`, `disabled`, …).
 *
 * The catalogue value already carries the brackets (`HtmlAttributes.type` is `"[type]"`), so this
 * form is the bare presence test and nothing is re-derived.
 */
fun attributeSelector(name: HtmlAttributes): Selector = Selector("$name")

/**
 * `[type="text"]` — the same catalogue entry, with a value.
 *
 * The brackets are stripped and handed to the string-based form below, which owns quoting,
 * [AttributeOperator] and the case flag — one implementation of the bracket grammar, not two.
 */
fun attributeSelector(
    name: HtmlAttributes,
    value: String,
    operator: AttributeOperator = AttributeOperator.equals,
    quote: Boolean = true,
    caseFlag: AttributeCase? = null,
): Selector = attributeSelector(
    name = "$name".removeSurrounding("[", "]"),
    value = value,
    operator = operator,
    quote = quote,
    caseFlag = caseFlag,
)

/**
 * `[part="label"]` — an attribute selector for a name the catalogue does not carry.
 *
 * That is the whole point of this overload: ARIA (`aria-current`, `aria-expanded`), `data-*` and any
 * namespace of a project's own are not HTML attributes, so there is no generated constant to pass. Every
 * name that *is* an HTML attribute should go through [HtmlAttributes] instead.
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
): Selector {
    if (value == null) return Selector("[$name]")
    val rendered = if (quote) "\"${escapeQuoted(value)}\"" else value
    val flag = caseFlag?.let { " ${it.token}" }.orEmpty()
    return Selector("[$name${operator.token}$rendered$flag]")
}

/**
 * `:hover` / `::before` as a selector part — the variant without the `&`, because a part is not
 * attached to anything yet.
 *
 * The argument is a [Pseudo] and nothing else: class-versus-element is decided by
 * `pseudoClass` / `pseudoElement` (and by the constants in [Pseudos]), never by a flag at the call
 * site — CSS fixes the kind per name.
 *
 * Two forms take an argument: an [Ident] for the bare token CSS does not type — the `label` of
 * `::part(label)`, the `2n+1` of `:nth-child(2n+1)` — and a [Selector] for what it does —
 * `:not(.is-hidden)`, `:is(.a, .b)`, `:host([collapsed])`.
 */
fun pseudo(pseudo: Pseudo): Selector = Selector("$pseudo")

fun pseudo(
    pseudo: Pseudo,
    argument: Ident,
): Selector = Selector("$pseudo($argument)")

fun pseudo(
    pseudo: Pseudo,
    argument: Selector,
): Selector = Selector("$pseudo($argument)")

/** The selectors that are a single token and have no factory of their own. */
object Selectors {
    /** `*` — every element. */
    val universal: Selector = Selector("*")

    /** `&` — the nesting selector, for building rule keys that stay attached to their parent. */
    val nesting: Selector = Selector("&")
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
