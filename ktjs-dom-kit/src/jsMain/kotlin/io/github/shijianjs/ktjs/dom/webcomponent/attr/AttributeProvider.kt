package io.github.shijianjs.ktjs.dom.webcomponent.attr

import io.github.shijianjs.ktjs.dom.attribute.AttributeCodec
import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementAdapter
import io.github.shijianjs.ktjs.dom.webcomponent.attributeNameOf
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * The object a delegated property is declared with: `var kind: String by attr(...)`.
 *
 * [provideDelegate] runs once per element instance, when the property is initialised. It creates the
 * delegate, works out the attribute name and registers it with the element, which is how
 * [CustomElementAdapter.applyInitialAttributes] later finds it.
 *
 * The name comes from [name] when `attr` was given one, otherwise from the element class's generated
 * table (`…Attributes.BY_PROPERTY`), which is what keeps `by attr` argument-free even for a property
 * whose attribute is spelled differently (`titleText` → `title`). No table means the property name
 * in kebab case, the rule `attr` has always applied.
 */
class AttributeProvider<T> internal constructor(
    private val name: String?,
    private val default: T?,
    private val codec: AttributeCodec<T>,
    private val onChange: (AttributeChange<T>) -> Unit,
) {

    operator fun provideDelegate(
        thisRef: CustomElementAdapter,
        property: KProperty<*>,
    ): ReadWriteProperty<CustomElementAdapter, T> {
        val delegate = AttributeDelegate(
            element = thisRef,
            name = name ?: thisRef.attributeNameOf(property.name),
            default = default,
            codec = codec,
            onChange = onChange,
        )
        thisRef.registerAttributeDelegate(delegate)
        return delegate
    }
}
