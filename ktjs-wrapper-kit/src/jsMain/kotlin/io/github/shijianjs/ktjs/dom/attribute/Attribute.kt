package io.github.shijianjs.ktjs.dom.attribute

import io.github.shijianjs.ktjs.dom.attribute.types.BooleanAttributeCodec
import io.github.shijianjs.ktjs.dom.attribute.types.DoubleAttributeCodec
import io.github.shijianjs.ktjs.dom.attribute.types.IntAttributeCodec
import io.github.shijianjs.ktjs.dom.attribute.types.LiteralAttributeCodec
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
 * What is deliberately *not* here: registration, `default` and change callbacks that belong to an
 * element's owner. Those are `attr`'s job. A wrapper gets the same reactivity from [observe] instead
 * of a delegate, and the absent-versus-empty distinction from [valueOrNull] / [setOrClear].
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
            write(codec.encode(value))
        }

    /**
     * The current value, or `null` when the attribute is absent — **or** when its text is not
     * something [codec] can read.
     *
     * Wrapper attributes are mostly optional, and absent is not the same as empty:
     * `aria-label=""` and no `aria-label` are different states. [value] cannot express that (it
     * answers [AttributeCodec.absent]), so the distinction lives here; pair it with [setOrClear].
     */
    val valueOrNull: T? get() = element.getAttribute(name)?.let(codec::decode)

    /** The attribute text exactly as the browser stores it, `null` when the attribute is absent. */
    val raw: String? get() = element.getAttribute(name)

    /** Whether the element carries the attribute at all. */
    val present: Boolean get() = element.hasAttribute(name)

    /**
     * Sets [value], or removes the attribute when it is `null`.
     *
     * The nullable counterpart of the [value] setter: `setOrClear(null)` is [clear], anything else is
     * an ordinary write.
     */
    fun setOrClear(value: T?) {
        if (value == null) clear() else this.value = value
    }

    /**
     * Calls [onChange] with the new value whenever this attribute really changes, including writes
     * from markup, devtools or another component — the reactivity `attr` gives the element's owner,
     * without owning the element.
     *
     * Only real changes are reported: writing the same decoded value again, or a write that decodes
     * to the same value (`"3"` → `"03"` on an `Int` attribute), does not fire. A removal reports
     * [AttributeCodec.absent].
     *
     * The callback gets the new value only. The old one and the raw text belong to `AttributeChange`,
     * which is part of the *authoring* side of the kit (the `attr` delegate), not of the wrapper
     * layer — a wrapper reacts to what the element is now. Use [observeAttribute] when the raw text
     * matters.
     *
     * The returned watch holds a live `MutationObserver`: disconnect it when it is no longer needed
     * (normally in the owning component's `disconnectedCallback`).
     */
    fun observe(onChange: (T) -> Unit): AttributeWatch =
        observeRawAttribute(element, name) { raw ->
            onChange(raw?.let(codec::decode) ?: codec.absent)
        }

    /** Removes the attribute. */
    fun clear() {
        element.removeAttribute(name)
    }

    private fun write(encoded: String?) {
        if (encoded == null) element.removeAttribute(name) else element.setAttribute(name, encoded)
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

/**
 * This element's attribute [name], limited to [values] — the typed form of a literal attribute like
 * `slot="navbar"` or `orientation="horizontal"`.
 *
 * [token] is how each value spells itself in the markup; wrapper libraries pass the table they
 * generate for their own vocabulary (`literalAttribute("theme", ButtonVariants.all, { it.name })`),
 * which is what turns a typo into a compile error instead of a silently ignored string. Decoding is
 * case-insensitive, and unknown text reads as the first value — see [LiteralAttributeCodec.absent].
 */
fun <T> HTMLElement.literalAttribute(name: String, values: List<T>, token: (T) -> String): Attribute<T> =
    Attribute(this, name, LiteralAttributeCodec(values, token))

/** This element's space-separated token attribute [name]; see [TokenListAttribute]. */
fun HTMLElement.tokenListAttribute(name: String): TokenListAttribute = TokenListAttribute(this, name)
