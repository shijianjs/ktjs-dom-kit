package io.github.shijianjs.ktjs.dom.webcomponent

import io.github.shijianjs.ktjs.dom.attribute.AttributeCodec
import io.github.shijianjs.ktjs.dom.webcomponent.attr.toAttributeName
import kotlin.js.JsAny
import web.html.HTMLElement

/**
 * The [CustomElementAttributes] of each element class, filed by the reference at
 * `customElements.define` time and read when a delegate is created.
 *
 * A `MutableMap` is a JavaScript `Map` on this target, so the class object is the key by identity —
 * the same object `X::class.js` hands to the reference and the one a running instance reports through
 * [elementClass]. An element whose reference was written by hand has no entry, and falls back to the
 * rules `attr` had before: the property name in kebab case, and the built-in codec for its type.
 */
internal object ElementAttributesRegistry {

    private val byClass: MutableMap<JsAny, CustomElementAttributes> = mutableMapOf()

    fun register(clazz: JsAny, attributes: CustomElementAttributes) {
        byClass[clazz] = attributes
    }

    fun lookup(clazz: JsAny?): CustomElementAttributes? = if (clazz == null) null else byClass[clazz]
}

/**
 * The class object of this element — `X.prototype.constructor` — which is the same object
 * `X::class.js` hands to the reference, and therefore the key the table above is filed under.
 *
 * `Object.getPrototypeOf` rather than `this.constructor` on purpose: the latter is a property lookup
 * that a page could shadow on the instance, while the prototype of a custom element is the class's.
 *
 * An instance of a subclassed element reports the subclass, whose generated table already contains
 * what its base declared — the processor writes the two lists merged, because the browser reads
 * `observedAttributes` per defined tag and knows nothing of the Kotlin-side inheritance.
 */
internal fun HTMLElement.elementClass(): JsAny? {
    val prototype: dynamic = js("Object.getPrototypeOf")(this)
    return prototype.constructor as JsAny?
}

/** The attribute [property] stands for: the class's table first, kebab case when there is none. */
internal fun CustomElementAdapter.attributeNameOf(property: String): String =
    ElementAttributesRegistry.lookup(elementClass())?.names?.get(property) ?: property.toAttributeName()

/** The codec [property] was generated with — only enums get one — or `null` to use the built-in. */
internal fun CustomElementAdapter.codecOf(property: String): AttributeCodec<*>? =
    ElementAttributesRegistry.lookup(elementClass())?.codecs?.get(property)
