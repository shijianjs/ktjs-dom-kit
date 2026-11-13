package io.github.shijianjs.ktjs.dom.attribute

/**
 * Converts between a delegated property's Kotlin value and the attribute's string form.
 *
 * The built-ins are `StringAttributeCodec`, `BooleanAttributeCodec`, `IntAttributeCodec`,
 * `DoubleAttributeCodec` and `enumAttributeCodec`; everything else (JSON, a timestamp, a parsed
 * record) is an implementation of this interface. No serialization library is involved.
 */
interface AttributeCodec<T> {

    /** The attribute value for [value]; `null` means "remove the attribute". */
    fun encode(value: T): String?

    /**
     * [value] parsed back, or `null` when it is not something this codec can read.
     *
     * A `null` here is treated exactly like a missing attribute: the delegate falls back to its
     * `default`, or to [absent] when it has none. Decoding never throws, so a typo in the markup
     * cannot break the element.
     */
    fun decode(value: String): T?

    /** The value a delegate reports while the attribute is absent and it has no `default`. */
    val absent: T
}
