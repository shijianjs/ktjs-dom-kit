package io.github.shijianjs.ktjs.dom.attribute

import web.html.HTMLElement

/**
 * One attribute, described without any element: a name and the [AttributeCodec] that turns its text
 * into a value.
 *
 * This is the other half of [attr]: `attr` is a *member* of your own component — it registers itself,
 * reports changes and owns a default — while an [Attribute] is a plain definition you can apply to
 * **any** element, including the ones that come out of a wrapper repository
 * (`createButton()` → `ButtonElement`). Nothing is registered, no listener is started and no callback
 * runs: the functions below only encode, write, read and decode, so a definition costs nothing per
 * element and can be shared by every type that carries the same attribute.
 *
 * ```kotlin
 * object Theme : Attribute<List<String>>("theme", TokenListAttributeCodec)
 *
 * val button = createButton()
 * button.writeAttribute(Theme, listOf("primary", "small"))
 * assertEquals(listOf("primary", "small"), button.readAttribute(Theme))
 * ```
 *
 * The functions are the public surface rather than a `by` delegate: a wrapper element is an
 * `external` class, so the only property you *could* declare on it is an extension property — and
 * Kotlin does not delegate those. If a call site really wants a name, write it there:
 *
 * ```kotlin
 * val ButtonElement.labelText: String
 *     get() = readAttribute(Label)
 *     set(value) = writeAttribute(Label, value)
 * ```
 */
class Attribute<T>(
    /** Attribute name as it appears in markup and CSS selectors, e.g. `theme`. */
    val name: String,
    /** Converts between the attribute text and [T]. */
    val codec: AttributeCodec<T>,
)

/** The attribute value of [attribute]: decoded, or its codec's `absent` when the attribute is missing. */
fun <T> HTMLElement.readAttribute(attribute: Attribute<T>): T {
    val raw = getAttribute(attribute.name)
        ?: return attribute.codec.absent
    return attribute.codec.decode(raw) ?: attribute.codec.absent
}

/**
 * Writes [value] to [attribute].
 *
 * `null` from the codec means "remove the attribute", which is how a boolean `false` and an empty
 * token list are expressed.
 */
fun <T> HTMLElement.writeAttribute(attribute: Attribute<T>, value: T) {
    val encoded = attribute.codec.encode(value)
    if (encoded == null) {
        removeAttribute(attribute.name)
    } else {
        setAttribute(attribute.name, encoded)
    }
}

/** Reads [attribute] as a mutable-looking token list; see [TokenList]. */
fun HTMLElement.tokens(attribute: Attribute<List<String>>): TokenList = TokenList(this, attribute)
