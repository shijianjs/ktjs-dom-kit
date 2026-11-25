package io.github.shijianjs.ktjs.dom.attribute

import io.github.shijianjs.ktjs.dom.attribute.types.BooleanAttributeCodec
import web.html.HTMLElement

/**
 * ARIA attributes a wrapper has to write on an element it does not own.
 *
 * They are **not** boolean attributes: ARIA is a text vocabulary — `aria-hidden="true"` and
 * `aria-hidden="false"` are both *present* and mean opposite things — so
 * [BooleanAttributeCodec] (presence semantics, the `hidden` rule) would turn `false` into "on".
 * The codec here always writes the spelled value, and an absent attribute decodes to the ARIA
 * default.
 */

/**
 * `aria-hidden`: whether an assistive reader should skip this element.
 *
 * ```kotlin
 * icon.ariaHiddenAttr.value = true        // aria-hidden="true"
 * icon.ariaHiddenAttr.value = false       // aria-hidden="false"
 * icon.ariaHiddenAttr.clear()             // attribute removed (the ARIA default)
 * ```
 *
 * Decoding is case-insensitive and anything that is not `"true"` reads as `false`: browsers treat
 * an invalid value as "not hidden". [Attribute.valueOrNull] still distinguishes absent from
 * `aria-hidden="false"`.
 */
val HTMLElement.ariaHiddenAttr: Attribute<Boolean>
    get() = attribute("aria-hidden", AriaHiddenAttributeCodec)

/** The codec behind [ariaHiddenAttr]; exposed so a wrapper can declare its own aliases. */
object AriaHiddenAttributeCodec : AttributeCodec<Boolean> {
    override fun encode(value: Boolean): String = value.toString()

    override fun decode(value: String): Boolean = value.equals("true", ignoreCase = true)

    override val absent: Boolean = false
}
