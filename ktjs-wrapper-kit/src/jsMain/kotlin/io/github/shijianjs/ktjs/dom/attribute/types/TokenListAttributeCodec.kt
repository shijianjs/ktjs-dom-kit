package io.github.shijianjs.ktjs.dom.attribute.types

import io.github.shijianjs.ktjs.dom.attribute.AttributeCodec

/**
 * Space-separated token list, the `classList` shape applied to an attribute.
 *
 * `theme="primary  small"` and `theme="small primary"` are the same set; the codec normalises to
 * single spaces, drops blanks and duplicates, and keeps the order in which tokens appear. Writing an
 * empty list removes the attribute, so "no tokens" and "absent" are the same state — exactly like an
 * empty `class`.
 *
 * ```kotlin
 * val Theme = Attribute("theme", TokenListAttributeCodec)
 * button.writeAttribute(Theme, listOf("primary", "small"))   // theme="primary small"
 * button.readAttribute(Theme)                                // ["primary", "small"]
 * ```
 *
 * Pair it with `element.tokens(Theme)` when `add` / `remove` / `toggle` / `contains` read better
 * than handing over a whole list.
 */
object TokenListAttributeCodec : AttributeCodec<List<String>> {

    override fun encode(value: List<String>): String? =
        value.flatMap(::tokenize).distinct().joinToString(" ").ifEmpty { null }

    override fun decode(value: String): List<String> = tokenize(value).distinct()

    override val absent: List<String> = emptyList()
}

/** Splits [value] on any whitespace and drops empty pieces; shared with `TokenList`. */
internal fun tokenize(value: String): List<String> = value.split(WHITESPACE).filter { it.isNotEmpty() }

private val WHITESPACE = Regex("\\s+")
