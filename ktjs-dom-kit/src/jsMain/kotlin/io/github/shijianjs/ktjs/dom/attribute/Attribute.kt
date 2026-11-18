package io.github.shijianjs.ktjs.dom.attribute

import io.github.shijianjs.ktjs.dom.attribute.types.BooleanAttributeCodec
import io.github.shijianjs.ktjs.dom.attribute.types.DoubleAttributeCodec
import io.github.shijianjs.ktjs.dom.attribute.types.IntAttributeCodec
import io.github.shijianjs.ktjs.dom.attribute.types.StringAttributeCodec
import web.html.HTMLElement

/**
 * One attribute **of one element**: a name, the [AttributeCodec] that turns its text into a value,
 * and the element it belongs to.
 *
 * This is the wrapper path. `attr { }` is a *member* of a component you own — it registers itself,
 * reports changes and holds a default — while a wrapper element (`createButton()` → `ButtonElement`)
 * is an `external` class: you cannot add members to it, and Kotlin does not delegate extension
 * properties. So the operation lives on an object instead, and the extension property only hands that
 * object out:
 *
 * ```kotlin
 * val ButtonElement.labelText: Attribute<String> get() = attribute("aria-label")
 *
 * button.labelText.value = "Close"        // aria-label="Close"
 * button.labelText.value                  // "Close"
 * button.labelText.clear()                // attribute removed
 * ```
 *
 * The object is stateless: every read goes through `getAttribute` and every write through
 * `setAttribute`. There is no cached copy that can drift, so a value written by another component,
 * devtools or a raw `setAttribute` is visible on the next read; and creating one costs nothing, which
 * is why the extension property can build one per access.
 *
 * What is deliberately *not* here: registration, observers, `default` and change callbacks. Those
 * belong to the element's owner (`attr`), not to the attribute. "Missing" reads as [AttributeCodec.absent].
 */
class Attribute<T>(
    /** The element whose attribute this object reads and writes. */
    val element: HTMLElement,
    /** Attribute name as it appears in markup and CSS selectors, e.g. `aria-label`. */
    val name: String,
    /** Converts between the attribute text and [T]. */
    val codec: AttributeCodec<T>,
) {

    /** The current value; assigning encodes it (a `null` encoding removes the attribute). */
    var value: T
        get() = element.getAttribute(name)?.let(codec::decode) ?: codec.absent
        set(value) {
            val encoded = codec.encode(value)
            if (encoded == null) {
                element.removeAttribute(name)
            } else {
                element.setAttribute(name, encoded)
            }
        }

    /** The attribute text exactly as the browser stores it, `null` when the attribute is absent. */
    val raw: String? get() = element.getAttribute(name)

    /** Whether the element carries the attribute at all. */
    val present: Boolean get() = element.hasAttribute(name)

    /** Removes the attribute. */
    fun clear() {
        element.removeAttribute(name)
    }
}

/** This element's `String` attribute [name]; a missing attribute reads as `""`. */
fun HTMLElement.attribute(name: String): Attribute<String> = Attribute(this, name, StringAttributeCodec)

/** This element's `Boolean` attribute [name]; presence semantics, like `hidden`. */
fun HTMLElement.booleanAttribute(name: String): Attribute<Boolean> = Attribute(this, name, BooleanAttributeCodec)

/** This element's `Int` attribute [name]; unreadable text falls back to `0`. */
fun HTMLElement.intAttribute(name: String): Attribute<Int> = Attribute(this, name, IntAttributeCodec)

/** This element's `Double` attribute [name]; unreadable text falls back to `0.0`. */
fun HTMLElement.doubleAttribute(name: String): Attribute<Double> = Attribute(this, name, DoubleAttributeCodec)

/** This element's attribute [name] read through an arbitrary [codec] (an enum, a timestamp, JSON …). */
fun <T> HTMLElement.attribute(name: String, codec: AttributeCodec<T>): Attribute<T> = Attribute(this, name, codec)

/** This element's space-separated token attribute [name]; see [TokenListAttribute]. */
fun HTMLElement.tokens(name: String): TokenListAttribute = TokenListAttribute(this, name)
