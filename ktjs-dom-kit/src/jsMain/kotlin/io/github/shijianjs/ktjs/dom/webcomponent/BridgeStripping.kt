package io.github.shijianjs.ktjs.dom.webcomponent

import js.reflect.Reflect
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
 *
 * Only needed while compiling with a Kotlin version the compiler bug still affects. Upstream is
 * [KT-91505](https://youtrack.jetbrains.com/issue/KT-91505) (fixed in 2.5.20-Beta1, build
 * `2.5.20-dev-4666`, reported for wrappers as kotlin-wrappers#2932); 2.2.20, 2.4.21 and 2.5.0-RC are
 * the affected releases, and this repository builds with 2.4.21. After upgrading, the prototypes no
 * longer carry these bridges and deleting them is a no-op, so this function and its call in
 * [defineIfMissing] can be retired once the browser tests still pass without them.
 */
fun stripBridgesFrom(klass: dynamic) {
    val htmlPrototype: dynamic = js("HTMLElement.prototype")
    var prototype: dynamic = klass.prototype
    while (prototype != null && prototype !== htmlPrototype) {
        deleteBridges(prototype)
        prototype = Reflect.getPrototypeOf(prototype)
    }
}

private fun deleteBridges(prototype: dynamic) {
    for (name in bridgeNames) {
        Reflect.deleteProperty(prototype, name)
    }
}
