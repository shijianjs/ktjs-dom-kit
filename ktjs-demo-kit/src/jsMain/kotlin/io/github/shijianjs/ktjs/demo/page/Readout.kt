package io.github.shijianjs.ktjs.demo.page

import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementAdapter
import web.html.HTMLElement

/**
 * Text that is updated in place to show the current state of an example, **as an element**.
 *
 * The element is the readout: `root` is itself, so a page appends it wherever the text belongs, and
 * `set` rewrites its own text. A page that wants a different look wraps it — that is the inheritance
 * and composition case, not a reason to keep this a bare class.
 *
 * The constructor takes no child: a custom element may not add children to itself, so the text and
 * the class arrive through [show], applied by the caller through the element's own tag:
 *
 * ```kotlin
 * private val status = Readout.NAME { show("Ready") }
 * ```
 */
class Readout : CustomElementAdapter() {
    /** The readout is its own root node. */
    val root: HTMLElement get() = this

    /** Writes the class and the first text; called once, when the element is created. */
    fun show(initial: String = "") {
        classList.add(READOUT)
        textContent = initial
    }

    /** Rewrites the text; the node itself stays put. */
    fun set(text: String) {
        textContent = text
    }

    companion object : ReadoutDefaultReference()
}
