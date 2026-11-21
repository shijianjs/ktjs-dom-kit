package io.github.shijianjs.ktjs.demo.i18n

import web.dom.ChildNode
import web.dom.Node
import web.dom.document
import web.html.HTMLElement

/**
 * Binds the nodes of one page to the fields of its text class.
 *
 * A page keeps a [Translations] of its text class, hands it here, and registers each node once with
 * the field it reads. [apply] walks the registrations and rewrites the node's own text in place — no
 * rebuild, no `data-i18n` attributes, and a field that disappears from the text class fails to
 * compile at the binding that used it.
 *
 * A node whose copy does not live in `textContent` registers the property it does live in
 * ([bind]'s `set` overload): component labels, `helperText`, `placeholder`, a tooltip's `text`.
 * Both overloads keep the same rule for empty copy — the node is hidden, so a section that declares
 * no hint leaves no blank line behind.
 */
class Binder<T : Any>(private val translations: Translations<T>) {
    /** node, the field it shows, and how that field is written onto it. */
    private val bindings: MutableList<Triple<HTMLElement, (T) -> String, (String) -> Unit>> = mutableListOf()

    /** The copy [apply] last resolved, for text a page composes itself (`"Error: " + message`). */
    var current: T = translations.languages.first().let { translations[it] }
        private set

    /**
     * Registers [node] to show [field] of the text class as its own text; returns the node
     * unchanged, still typed as the concrete element, so a binding can be chained into its API.
     *
     * Only the node's text children are rewritten — element children (a slotted tooltip, a prefix
     * icon) stay where they are. `textContent` would drop them, and a language switch happens after
     * the page has already been built, so dropping them is silent until the next hover.
     */
    fun <E : HTMLElement> bind(node: E, field: (T) -> String): E {
        bindings += Triple(node, field) { text -> node.replaceOwnText(text) }
        return node
    }

    /**
     * Registers [node] to show [field] of the text class through [set], for copy that lives in a
     * property rather than in `textContent` (`label`, `helperText`, `placeholder`, `text`).
     */
    fun <E : HTMLElement> bind(node: E, field: (T) -> String, set: (String) -> Unit): E {
        bindings += Triple(node, field, set)
        return node
    }

    /**
     * Rewrites every registered node from the text class of [lang].
     *
     * The `hidden` attribute follows the text: `SectionBlock` hides an empty hint line, and a page
     * that rewrites its sections through a binder never calls `setText` again — without this the
     * hint would appear at construction and stay hidden for every later language.
     */
    fun apply(lang: Lang) {
        current = translations[lang]
        bindings.forEach { (node, field, set) ->
            val text = field(current)
            set(text)
            node.toggleAttribute("hidden", text.isEmpty())
        }
    }
}

/** Replaces the text children of this node with [text], leaving its element children untouched. */
private fun HTMLElement.replaceOwnText(text: String) {
    var child: ChildNode? = firstChild
    while (child != null) {
        val next = child.nextSibling
        if (child.nodeType == Node.TEXT_NODE) removeChild(child)
        child = next
    }
    insertBefore(document.createTextNode(text), firstChild)
}
