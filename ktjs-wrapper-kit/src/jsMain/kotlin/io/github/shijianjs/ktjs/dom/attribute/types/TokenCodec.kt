package io.github.shijianjs.ktjs.dom.attribute.types

/**
 * Converts between **one token** of a space-separated attribute and its Kotlin value.
 *
 * A token list is stored as text, so a component that has a vocabulary (`theme="primary small"`)
 * can only offer a typed view if it can spell each value and read it back. That is this codec, one
 * token at a time — the per-element counterpart of [io.github.shijianjs.ktjs.dom.attribute.AttributeCodec],
 * which converts a whole attribute.
 *
 * [decode] returning `null` means "not one of my values": the token stays in the attribute (hand
 * written or third-party tokens survive a typed write) but is left out of
 * [io.github.shijianjs.ktjs.dom.attribute.TokenListAttribute.values]. [encode] is total, so any
 * value can be written.
 *
 * ```kotlin
 * object ThemeVariantTokenCodec : TokenCodec<ThemeVariant> {
 *     override fun encode(value: ThemeVariant): String = value.name
 *     override fun decode(token: String): ThemeVariant = ThemeVariant(token)
 * }
 *
 * button.tokenListAttribute("theme", ThemeVariantTokenCodec).add(ButtonVariants.primary)
 * ```
 *
 * [StringTokenCodec] is the identity codec, which is what `element.tokenListAttribute(name)`
 * uses — a plain `List<String>` vocabulary is the common case, and the typed machinery must not cost
 * anything there.
 */
interface TokenCodec<T> {

    /** How [value] spells itself as a token, e.g. a variant's `name`. */
    fun encode(value: T): String

    /** [token] read back as a value, or `null` when it is not one this codec knows. Never throws. */
    fun decode(token: String): T?
}

/** The identity codec: tokens are plain strings, and every string is a valid token. */
object StringTokenCodec : TokenCodec<String> {

    override fun encode(value: String): String = value

    override fun decode(token: String): String = token
}
