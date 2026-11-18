package io.github.shijianjs.ktjs.dom.attribute

import io.github.shijianjs.ktjs.dom.attribute.types.TokenListAttributeCodec
import io.github.shijianjs.ktjs.dom.attribute.types.tokenize
import web.html.HTMLElement

/**
 * One space-separated attribute **of one element** — the `classList` idea for `theme="primary small"`,
 * `aria-describedby="a b"`, `data-tags="x y"`.
 *
 * Obtained with `element.tokenListAttribute(name)`; every operation reads or writes the attribute itself, so
 * nothing has to be kept in sync and a write from anywhere (`setAttribute`, devtools, another
 * component) is visible on the next call.
 *
 * ```kotlin
 * val theme = button.tokenListAttribute("theme")
 * theme.add("primary")        // theme="primary"
 * theme.toggle("small")       // theme="primary small"
 * theme.remove("primary")     // theme="small"
 * theme.clear()               // attribute removed
 * ```
 *
 * Order follows the attribute (deduplicated, single spaces); unlike `DOMTokenList` no exception is
 * thrown for odd syntax — a malformed value is simply not a token.
 */
class TokenListAttribute(
    /** The element whose attribute this object reads and writes. */
    val element: HTMLElement,
    /** Attribute name as it appears in markup and CSS selectors, e.g. `theme`. */
    val name: String,
) {

    /** The tokens currently set, in attribute order. */
    val values: List<String> get() = raw?.let(TokenListAttributeCodec::decode) ?: emptyList()

    /** The attribute text exactly as the browser stores it, `null` when the attribute is absent. */
    val raw: String? get() = element.getAttribute(name)

    /** Whether the element carries the attribute at all. */
    val present: Boolean get() = element.hasAttribute(name)

    /** Replaces the whole list; an empty list removes the attribute. */
    var value: List<String>
        get() = values
        set(value) = write(value)

    /** Whether [token] is part of the list. */
    fun contains(token: String): Boolean = token in values

    /** Adds the missing [tokens] and writes the attribute once; existing tokens keep their place. */
    fun add(vararg tokens: String): TokenListAttribute {
        val current = values
        val missing = tokens.flatMap(::tokenize).filter { it !in current }
        if (missing.isNotEmpty()) write(current + missing)
        return this
    }

    /** Removes [tokens]; tokens that are not set are ignored. */
    fun remove(vararg tokens: String): TokenListAttribute {
        val current = values
        val removed = tokens.flatMap(::tokenize)
        val kept = current.filterNot { it in removed }
        if (kept.size != current.size) write(kept)
        return this
    }

    /** Adds [token] when absent, removes it when present; returns whether it is set afterwards. */
    fun toggle(token: String): Boolean = if (contains(token)) {
        remove(token)
        false
    } else {
        add(token)
        true
    }

    /** Sets or clears [token]; returns whether it is set afterwards. */
    fun toggle(token: String, present: Boolean): Boolean {
        if (present) add(token) else remove(token)
        return present
    }

    /** Removes every token, which removes the attribute. */
    fun clear() {
        element.removeAttribute(name)
    }

    private fun write(values: List<String>) {
        val encoded = TokenListAttributeCodec.encode(values)
        if (encoded == null) {
            element.removeAttribute(name)
        } else {
            element.setAttribute(name, encoded)
        }
    }
}
