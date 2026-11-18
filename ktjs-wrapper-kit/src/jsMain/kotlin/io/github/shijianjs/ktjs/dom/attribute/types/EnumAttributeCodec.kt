package io.github.shijianjs.ktjs.dom.attribute.types

import io.github.shijianjs.ktjs.dom.attribute.AttributeCodec

/**
 * Enum attribute, matched against [label] ignoring case.
 *
 * Kotlin/JS cannot enumerate an enum through reflection, so the values are passed in:
 * `attr(codec = enumAttributeCodec(ActionKind.entries), default = ActionKind.Primary)`. [label]
 * defaults to the lower-cased constant name (`Primary` → `primary`); the constant name itself is
 * accepted when decoding as well. [AttributeCodec.absent] is the first value, which only matters
 * when the delegate has no `default`.
 */
fun <E : Enum<E>> enumAttributeCodec(
    values: List<E>,
    label: (E) -> String = { it.name.lowercase() },
): AttributeCodec<E> = object : AttributeCodec<E> {
    override fun encode(value: E): String = label(value)

    override fun decode(value: String): E? {
        val text = value.trim()
        return values.firstOrNull { label(it).equals(text, ignoreCase = true) }
            ?: values.firstOrNull { it.name.equals(text, ignoreCase = true) }
    }

    override val absent: E = values.first()
}
