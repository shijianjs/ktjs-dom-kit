package io.github.shijianjs.ktjs.dom.attribute.types

import io.github.shijianjs.ktjs.dom.attribute.AttributeCodec

/** Whole number; anything `toIntOrNull` rejects falls back to the delegate's `default` (or `0`). */
object IntAttributeCodec : AttributeCodec<Int> {
    override fun encode(value: Int): String = value.toString()

    override fun decode(value: String): Int? = value.trim().toIntOrNull()

    override val absent: Int = 0
}
