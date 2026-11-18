package io.github.shijianjs.ktjs.dom.attribute.types

import io.github.shijianjs.ktjs.dom.attribute.AttributeCodec

/**
 * HTML boolean attribute: presence means `true`, whatever the value says.
 *
 * `<x disabled>` and `<x disabled="false">` are both `true` — this is the semantics the HTML parser
 * gives `checked` / `hidden` / `disabled`, not a Kotlin design choice. `true` writes an empty string
 * (so the attribute shows up bare), `false` removes it.
 */
object BooleanAttributeCodec : AttributeCodec<Boolean> {
    override fun encode(value: Boolean): String? = if (value) "" else null

    override fun decode(value: String): Boolean = true

    override val absent: Boolean = false
}
