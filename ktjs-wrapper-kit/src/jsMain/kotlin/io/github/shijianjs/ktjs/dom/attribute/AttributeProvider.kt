package io.github.shijianjs.ktjs.dom.attribute

import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementAdapter
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * The object a delegated property is declared with: `var kind: String by attr(...)`.
 *
 * [provideDelegate] runs once per element instance, when the property is initialised. It creates the
 * delegate, works out the attribute name and registers it with the element, which is how
 * [CustomElementAdapter.applyInitialAttributes] later finds it.
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
            name = name ?: property.name.toAttributeName(),
            default = default,
            codec = codec,
            onChange = onChange,
        )
        thisRef.registerAttributeDelegate(delegate)
        return delegate
    }
}
