package io.github.shijianjs.ktjs.dom.attribute

import io.github.shijianjs.ktjs.dom.attribute.types.tokenize
import web.html.HTMLElement

/**
 * An attribute made of space-separated tokens — the `classList` idea applied to any attribute
 * (`theme="primary small"`, `aria-describedby="a b"`, `data-tags="x y"`).
 *
 * Obtained with `element.tokens(attribute)`; every operation reads or writes the attribute itself,
 * so there is no in-memory copy to keep in sync and a token written elsewhere (`setAttribute`,
 * devtools, another component) is visible on the next call.
 *
 * ```kotlin
 * val Theme = Attribute("theme", TokenListAttributeCodec)
 * val tokens = button.tokens(Theme)
 * tokens.add("primary")          // theme="primary"
 * tokens.toggle("small")         // theme="primary small"
 * tokens.remove("primary")       // theme="small"
 * tokens.clear()                 // attribute removed
 * ```
 *
 * Order follows the attribute (deduplicated, single spaces); unlike `DOMTokenList` no exception is
 * thrown for token syntax — a malformed value is simply not a token.
 */
class TokenList internal constructor(
    private val element: HTMLElement,
    private val attribute: Attribute<List<String>>,
) {

    /** The tokens currently set, in attribute order. */
    val values: List<String> get() = element.readAttribute(attribute)

    /** The attribute text exactly as the browser stores it, `null` when the attribute is absent. */
    val raw: String? get() = element.getAttribute(attribute.name)

    /** Whether [token] is part of the list. */
    fun contains(token: String): Boolean = token in values

    /** Adds the missing [tokens] and writes the attribute once; existing tokens keep their place. */
    fun add(vararg tokens: String): TokenList {
        val current = values
        val missing = tokens.flatMap(::tokenize).filter { it !in current }
        if (missing.isNotEmpty()) element.writeAttribute(attribute, current + missing)
        return this
    }

    /** Removes [tokens]; tokens that are not set are ignored. */
    fun remove(vararg tokens: String): TokenList {
        val current = values
        val removed = tokens.flatMap(::tokenize)
        val kept = current.filterNot { it in removed }
        if (kept.size != current.size) element.writeAttribute(attribute, kept)
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
        element.removeAttribute(attribute.name)
    }
}
