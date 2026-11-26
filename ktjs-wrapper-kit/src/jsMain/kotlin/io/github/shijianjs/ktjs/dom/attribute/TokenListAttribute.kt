package io.github.shijianjs.ktjs.dom.attribute

import io.github.shijianjs.ktjs.dom.attribute.types.TokenCodec
import io.github.shijianjs.ktjs.dom.attribute.types.TokenListAttributeCodec
import io.github.shijianjs.ktjs.dom.attribute.types.tokenize
import web.html.HTMLElement

/**
 * One space-separated attribute **of one element** — the `classList` idea for `theme="primary small"`,
 * `aria-describedby="a b"`, `data-tags="x y"`.
 *
 * Obtained with `element.tokenListAttribute(name)` (plain strings) or
 * `element.tokenListAttribute(name, codec)` (a typed vocabulary); every operation reads or writes the
 * attribute itself, so nothing has to be kept in sync and a write from anywhere (`setAttribute`,
 * devtools, another component) is visible on the next call.
 *
 * ```kotlin
 * val theme = button.tokenListAttribute("theme")
 * theme.add("primary")        // theme="primary"
 * theme.toggle("small")       // theme="primary small"
 * theme.remove("primary")     // theme="small"
 * theme.clear()               // attribute removed
 * ```
 *
 * There is exactly **one** storage — the attribute text — and [T] only adds a second way to read and
 * write it. [tokens] is the raw vocabulary, [values] the decoded one:
 *
 * ```kotlin
 * val theme = button.tokenListAttribute("theme", ThemeVariantTokenCodec)
 * theme.add(ButtonVariants.primary)       // typed: written as its token
 * theme.addTokens("my-third-party-theme") // raw: a token the codec does not know
 * theme.values                            // [ButtonVariants.primary]; unknown tokens are not here
 * theme.tokens                            // ["primary", "my-third-party-theme"]
 * ```
 *
 * A token the codec cannot decode is never dropped by a typed `add` / `remove` / `toggle`: those
 * rewrite the raw list, so hand written and third-party tokens survive. Only assigning [value]
 * replaces the whole list, and therefore only keeps what the codec can produce from the values
 * handed to it.
 *
 * Order follows the attribute (deduplicated, single spaces); unlike `DOMTokenList` no exception is
 * thrown for odd syntax — a malformed value is simply not a token.
 */
class TokenListAttribute<T>(
    /** The element whose attribute this object reads and writes. */
    val element: HTMLElement,
    /** Attribute name as it appears in markup and CSS selectors, e.g. `theme`. */
    val name: String,
    /** Converts one token to/from [T]; see [TokenCodec]. */
    val codec: TokenCodec<T>,
) {

    /**
     * The tokens currently set, in attribute order — the raw vocabulary, unknown tokens included.
     * Assigning replaces the whole list; an empty list removes the attribute.
     */
    var tokens: List<String>
        get() = raw?.let(TokenListAttributeCodec::decode) ?: emptyList()
        set(value) = write(value)

    /** The tokens [codec] can read back, in attribute order; unknown tokens are left out. */
    val values: List<T> get() = tokens.mapNotNull(codec::decode)

    /** Replaces the whole list with [values] encoded; an empty list removes the attribute. */
    var value: List<T>
        get() = values
        set(value) = write(value.map(codec::encode))

    /** The attribute text exactly as the browser stores it, `null` when the attribute is absent. */
    val raw: String? get() = element.getAttribute(name)

    /** Whether the element carries the attribute at all. */
    val present: Boolean get() = element.hasAttribute(name)

    /** Whether [value] is part of the list, compared by its token. */
    fun contains(value: T): Boolean = codec.encode(value) in tokens

    /** Whether the raw [token] is part of the list — for tokens this codec does not know. */
    fun containsToken(token: String): Boolean = token in tokens

    /** Adds the missing [values] and writes the attribute once; existing tokens keep their place. */
    fun add(vararg values: T): TokenListAttribute<T> =
        addTokens(*values.map(codec::encode).toTypedArray())

    /** Adds the missing raw [tokens] and writes the attribute once; existing tokens keep their place. */
    fun addTokens(vararg tokens: String): TokenListAttribute<T> {
        val current = this.tokens
        val missing = tokens.flatMap(::tokenize).filter { it !in current }
        if (missing.isNotEmpty()) write(current + missing)
        return this
    }

    /** Removes [values] by their tokens; tokens that are not set are ignored. */
    fun remove(vararg values: T): TokenListAttribute<T> =
        removeTokens(*values.map(codec::encode).toTypedArray())

    /** Removes raw [tokens]; tokens that are not set are ignored. */
    fun removeTokens(vararg tokens: String): TokenListAttribute<T> {
        val current = this.tokens
        val removed = tokens.flatMap(::tokenize)
        val kept = current.filterNot { it in removed }
        if (kept.size != current.size) write(kept)
        return this
    }

    /** Adds [value] when absent, removes it when present; returns whether it is set afterwards. */
    fun toggle(value: T): Boolean = if (contains(value)) {
        remove(value)
        false
    } else {
        add(value)
        true
    }

    /** Sets or clears [value]; returns whether it is set afterwards. */
    fun toggle(value: T, present: Boolean): Boolean {
        if (present) add(value) else remove(value)
        return present
    }

    /** Removes every token, which removes the attribute. */
    fun clear() {
        element.removeAttribute(name)
    }

    private fun write(tokens: List<String>) {
        val encoded = TokenListAttributeCodec.encode(tokens)
        if (encoded == null) {
            element.removeAttribute(name)
        } else {
            element.setAttribute(name, encoded)
        }
    }
}
