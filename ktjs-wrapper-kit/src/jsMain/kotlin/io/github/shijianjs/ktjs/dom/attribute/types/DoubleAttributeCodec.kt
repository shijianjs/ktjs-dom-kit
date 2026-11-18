package io.github.shijianjs.ktjs.dom.attribute.types

import io.github.shijianjs.ktjs.dom.attribute.AttributeCodec

/** Floating point number; anything `toDoubleOrNull` rejects falls back to the default (or `0.0`). */
object DoubleAttributeCodec : AttributeCodec<Double> {
    override fun encode(value: Double): String = value.toString()

    override fun decode(value: String): Double? = value.trim().toDoubleOrNull()

    override val absent: Double = 0.0
}
