package io.github.shijianjs.ktjs.dom.webcomponent

import js.reflect.unsafeCast
import web.components.CustomElementReference
import web.components.customElements
import web.components.define
import web.dom.TagName
import web.html.HTMLElement

/**
 * Defines a custom element tag once, after removing the compiler-generated bridges from the element
 * class.
 *
 * Order matters: [stripBridgesFrom] has to run before `customElements.define`, because defining the
 * tag upgrades any element already in the document and their `connectedCallback` runs immediately.
 * Calling this twice is safe; the second call sees the registered tag and returns.
 */
fun <T : HTMLElement> defineIfMissing(name: TagName<T>, reference: CustomElementReference<T>) {
    if (customElements.get(name) != null) return
    stripBridgesFrom(unsafeCast<dynamic>(reference.value))
    customElements.define(name, reference)
}
