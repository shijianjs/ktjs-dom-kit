package io.github.shijianjs.ktjs.demo.page

import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementAdapter
import web.dom.Node

/**
 * A responsive grid of its children, **as an element**.
 *
 * The host is the grid: children are appended straight into it, so they stay in the page's light DOM
 * and keep the page's sheet. The track function (`repeat(auto-fill, minmax(…)`) has no typed
 * counterpart in the CSS vocabulary, so it stays one declaration; the rest of the block is the
 * `.grid` rule in [PAGE_SHEET].
 *
 * The block spans its container. A section body is a vertical layout, which sizes children to their
 * content and aligns them to the start; without the width a grid would pick its column count from
 * its own narrowest width rather than from the page.
 *
 * Created through its own tag, the way every element in this kit is:
 *
 * ```kotlin
 * val cells = Grid.NAME { show(260); add(card, card) }
 * ```
 */
class Grid : CustomElementAdapter() {
    /** Sizes the tracks; the children are already the element's own children. */
    fun show(minColumnWidth: Int) {
        classList.add(GRID)
        style.setProperty("grid-template-columns", "repeat(auto-fill, minmax(${minColumnWidth}px, 1fr))")
        style.setProperty("gap", "12px")
        style.setProperty("width", "100%")
    }

    /** Adds one cell; the element keeps the order they arrive in. */
    fun add(vararg children: Node) {
        if (children.isNotEmpty()) append(*children)
    }

    companion object : GridDefaultReference()
}
