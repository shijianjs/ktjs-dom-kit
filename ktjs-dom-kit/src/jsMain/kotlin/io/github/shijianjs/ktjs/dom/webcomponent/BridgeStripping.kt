package io.github.shijianjs.ktjs.dom.webcomponent

import kotlin.js.js

/**
 * Members Kotlin/JS re-declares on a custom element class for the external DOM interfaces it inherits
 * (`ParentNode`, `ChildNode`, `NonDocumentTypeChildNode`, `Animatable`, `HTMLOrSVGElement`,
 * `EventTargetLike`, `Slottable`). Every one of them is a self-recursive bridge sitting on that
 * class's prototype.
 *
 * Deleting a name that was not bridged is a no-op, so the list can stay wider than strictly needed.
 */
private val bridgeNames = listOf(
    "dispatchEvent", "addEventListener", "removeEventListener", "animate", "getAnimations",
    "after", "before", "remove", "replaceWith", "append", "moveBefore", "prepend",
    "querySelector", "querySelectorAll", "replaceChildren", "blur", "focus",
)

/**
 * Deletes the bridges from [klass]'s prototype chain, stopping before `HTMLElement.prototype`, where
 * these names are the real DOM methods.
 *
 * Run it once per element class, before `customElements.define` and therefore before any instance
 * exists. Passing the concrete class is enough: an instance resolves a member through the whole
 * chain, so every class in it has to be cleaned — Kotlin emits a bridge on each class, not only on
 * [CustomElementAdapter].
 */
fun stripBridgesFrom(klass: dynamic) {
    val htmlPrototype: dynamic = js("HTMLElement.prototype")
    var prototype: dynamic = klass.prototype
    while (prototype != null && prototype !== htmlPrototype) {
        deleteBridges(prototype)
        prototype = js("Object.getPrototypeOf")(prototype)
    }
}

private fun deleteBridges(prototype: dynamic) {
    val deleteProperty: dynamic = js("Reflect.deleteProperty")
    for (name in bridgeNames) {
        deleteProperty(prototype, name)
    }
}
