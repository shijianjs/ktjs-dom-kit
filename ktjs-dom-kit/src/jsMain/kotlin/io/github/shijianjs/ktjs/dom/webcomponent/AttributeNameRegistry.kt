package io.github.shijianjs.ktjs.dom.webcomponent

import io.github.shijianjs.ktjs.dom.webcomponent.attr.toAttributeName
import kotlin.js.JsAny
import web.html.HTMLElement

/**
 * The property-name → attribute-name table of each element class, filled by the reference at
 * `customElements.define` time and read when a delegate is created.
 *
 * The table exists because a `by attr` property may stand for an attribute spelled differently —
 * `titleText` backs `title`, `iconName` backs `icon` — and a delegated property cannot see its own
 * class at runtime, only its name. `ktjs-dom-kit-ksp` writes the table next to the element
 * (`…Attributes.BY_PROPERTY`) and the generated `…DefaultReference` registers it here, so
 *
 * ```kotlin
 * var titleText: String by attr { … }
 * ```
 *
 * resolves to `title` without repeating the name in the call. An element registered by hand (no
 * table) keeps the previous behaviour: the property name in kebab case.
 *
 * A `MutableMap` is a JavaScript `Map` on this target, so the class object is the key by identity.
 */
internal object AttributeNameRegistry {

    private val byClass: MutableMap<JsAny, Map<String, String>> = mutableMapOf()

    fun register(clazz: JsAny, names: Map<String, String>) {
        byClass[clazz] = names
    }

    fun lookup(clazz: JsAny?): Map<String, String>? = if (clazz == null) null else byClass[clazz]
}

/**
 * The class object of this element — `X.prototype.constructor` — which is the same object
 * `X::class.js` hands to the reference, and therefore the key the table above is filed under.
 *
 * `Object.getPrototypeOf` rather than `this.constructor` on purpose: the latter is a property lookup
 * that a page could shadow on the instance, while the prototype of a custom element is the class's.
 */
internal fun HTMLElement.elementClass(): JsAny? {
    val prototype: dynamic = js("Object.getPrototypeOf")(this)
    return prototype.constructor as JsAny?
}

/** The attribute [property] stands for: the class's table first, kebab case when there is none. */
internal fun CustomElementAdapter.attributeNameOf(property: String): String =
    AttributeNameRegistry.lookup(elementClass())?.get(property) ?: property.toAttributeName()
