package io.github.shijianjs.ktjs.dom.attribute.types

import io.github.shijianjs.ktjs.dom.attribute.AttributeCodec

/**
 * Plain string value: the attribute is read and written verbatim.
 *
 * A missing attribute decodes to `""`, so a `String` property is never null. Use `String?` as the
 * property type when absence has to be distinguishable — that needs its own codec, since the value
 * would have to survive `encode(null)`, which the delegate reads as "remove the attribute".
 */
object StringAttributeCodec : AttributeCodec<String> {
    override fun encode(value: String): String = value

    override fun decode(value: String): String = value

    override val absent: String = ""
}
