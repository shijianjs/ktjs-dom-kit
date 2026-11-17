package io.github.shijianjs.ktjs.demo

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

    /** Registers [node] to show [field] of the text class; returns the node so it can be appended. */
    fun bind(node: HTMLElement, field: (T) -> String): HTMLElement {
        bindings += node to field
        return node
    }

    /** Rewrites every registered node from the text class of [lang]. */
    fun apply(lang: Lang) {
        val texts = translations[lang]
        bindings.forEach { (node, field) -> node.textContent = field(texts) }
    }
}
