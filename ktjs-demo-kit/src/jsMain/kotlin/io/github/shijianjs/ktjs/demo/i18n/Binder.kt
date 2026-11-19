package io.github.shijianjs.ktjs.demo.i18n

import web.html.HTMLElement

/**
 * Binds the nodes of one page to the fields of its text class.
 *
 * A page keeps a [Translations] of its text class, hands it here, and registers each node once with
 * the field it reads. [apply] walks the registrations and rewrites `textContent` in place — no
 * rebuild, no `data-i18n` attributes, and a field that disappears from the text class fails to
 * compile at the binding that used it.
 */
class Binder<T : Any>(private val translations: Translations<T>) {
    private val bindings: MutableList<Pair<HTMLElement, (T) -> String>> = mutableListOf()

    /**
     * Registers [node] to show [field] of the text class; returns the node unchanged, still typed as
     * the concrete element, so a binding can be chained into the API of that element.
     */
    fun <E : HTMLElement> bind(node: E, field: (T) -> String): E {
        bindings += node to field
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
        val texts = translations[lang]
        bindings.forEach { (node, field) ->
            val text = field(texts)
            node.textContent = text
            node.toggleAttribute("hidden", text.isEmpty())
        }
    }
}
