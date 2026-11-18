package io.github.shijianjs.ktjs.dom.attribute

import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementAdapter

/**
 * One change of one attribute, as an `attr` delegate saw it.
 *
 * An object rather than just the new value, so the name, the raw strings and a removal are available
 * without changing the callback's signature later. Only real changes are reported: writing the same
 * decoded value again does nothing. On the **first** report (from
 * [CustomElementAdapter.applyInitialAttributes] or the first setter call) there is no previous
 * report, so `oldValue == newValue` and `oldRaw == newRaw`. [newRaw] being `null` tells a removal
 * apart from a value that happens to decode to the default.
 */
class AttributeChange<T> internal constructor(
    /** Attribute name, e.g. `max-count`. */
    val name: String,
    /** Decoded value before the change; equal to [newValue] on the first report. */
    val oldValue: T,
    /** Decoded value after the change. */
    val newValue: T,
    /** Raw attribute value before the change, `null` when the attribute was absent. */
    val oldRaw: String?,
    /** Raw attribute value after the change, `null` when the attribute was removed. */
    val newRaw: String?,
) {
    /** `true` when the attribute was removed rather than written. */
    val isRemoval: Boolean get() = newRaw == null

    override fun toString(): String = "AttributeChange($name: $oldRaw -> $newRaw, $oldValue -> $newValue)"
}
