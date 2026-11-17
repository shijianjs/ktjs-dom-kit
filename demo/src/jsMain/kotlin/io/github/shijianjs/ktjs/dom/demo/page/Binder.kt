package io.github.shijianjs.ktjs.dom.demo.page

import io.github.shijianjs.ktjs.demo.Lang
import web.html.HTMLElement

/**
 * Binds the nodes of one page to the fields of its text class.
 *
 * Every page keeps one text class per language (`OverviewTexts` and so on) and resolves it with a
 * `(Lang) -> Texts` function. A node is registered once, with the field it reads, and [apply] walks
 * the registrations and rewrites `textContent` in place — no rebuild, no `data-i18n` attributes, and
 * a field that disappears from the text class fails to compile at the binding that used it.
 */
internal class Binder<T>(private val resolve: (Lang) -> T) {
    private val bindings: MutableList<Pair<HTMLElement, (T) -> String>> = mutableListOf()

    /** Registers [node] to show [field] of the text class; returns the node so it can be appended. */
    fun bind(node: HTMLElement, field: (T) -> String): HTMLElement {
        bindings += node to field
        return node
    }

    /** Rewrites every registered node from the text class of [lang]. */
    fun apply(lang: Lang) {
        val texts = resolve(lang)
        bindings.forEach { (node, field) -> node.textContent = field(texts) }
    }
}
