package io.github.shijianjs.ktjs.dom.attribute.types

import io.github.shijianjs.ktjs.dom.attribute.AttributeCodec

/**
 * Attribute limited to a fixed vocabulary, matched against [token] ignoring case.
 *
 * This is the codec behind `literalAttribute(...)`: wrapper libraries model upstream literals as
 * `sealed external interface` values, and this is how one of those comes back out of an attribute
 * string. Anything unknown decodes to `null`, so [AttributeCodec.absent] — the **first** value, since
 * Kotlin/JS has no way to invent one — is what an unknown or missing attribute reads as.
 *
 * ```kotlin
 * val slots = listOf("header", "content", "footer")
 * val codec = LiteralAttributeCodec(slots) { it }
 * codec.encode("content")   // "content"
 * codec.decode("CONTENT")   // "content"
 * ```
 *
 * An empty [values] list is not a supported vocabulary: it has no `absent` value to answer with, so
 * it is rejected at construction instead of failing on the first read.
 */
class LiteralAttributeCodec<T>(
    /** Every value this attribute accepts, in declaration order; the first one is [absent]. */
    val values: List<T>,
    /** How a value spells itself in the attribute text. */
    val token: (T) -> String,
) : AttributeCodec<T> {

    init {
        require(values.isNotEmpty()) { "LiteralAttributeCodec needs at least one value to answer with" }
    }

    override fun encode(value: T): String = token(value)

    override fun decode(value: String): T? {
        val text = value.trim()
        if (text.isEmpty()) return null
        return values.firstOrNull { token(it).equals(text, ignoreCase = true) }
    }

    override val absent: T = values.first()

    override fun toString(): String = "LiteralAttributeCodec(${values.map(token)})"
}
