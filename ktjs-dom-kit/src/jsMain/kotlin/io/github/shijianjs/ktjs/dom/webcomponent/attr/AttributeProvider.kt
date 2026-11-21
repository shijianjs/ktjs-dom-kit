package io.github.shijianjs.ktjs.dom.webcomponent.attr

import io.github.shijianjs.ktjs.dom.attribute.AttributeCodec
import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementAdapter
import io.github.shijianjs.ktjs.dom.webcomponent.attributeNameOf
import io.github.shijianjs.ktjs.dom.webcomponent.codecOf
import js.reflect.unsafeCast
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * The object a delegated property is declared with: `var kind: String by attr(...)`.
 *
 * [provideDelegate] runs once per element instance, when the property is initialised. It creates the
 * delegate, works out the attribute name and the codec, and registers the delegate with the element,
 * which is how [CustomElementAdapter.applyInitialAttributes] later finds it.
 *
 * Both answers are narrowed in the same order:
 *
 * 1. what `attr` was handed (`name`, `codec`), for a given declaration;
 * 2. what the element class was generated with (`…DefaultReference.ATTRIBUTES`), which is what keeps
 *    `by attr` argument-free even for a property whose attribute is spelled differently
 *    (`titleText` → `title`) or whose type is an enum (`kind` → `enumAttributeCodec`);
 * 3. the rule `attr` has always applied: the property name in kebab case, and the built-in codec for
 *    `String`, `Boolean`, `Int`, `Double`.
 *
 * Steps 1 and 2 need different moments: [attr] is inlined and knows the type but not the element, this
 * object knows the element but not the type — which is why the built-in codec is also passed in, and
 * why a type none of the three covers fails here rather than at the declaration site.
 */
class AttributeProvider<T> internal constructor(
    private val name: String?,
    private val default: T?,
    private val codec: AttributeCodec<T>?,
    private val onChange: (AttributeChange<T>) -> Unit,
) {

    operator fun provideDelegate(
        thisRef: CustomElementAdapter,
        property: KProperty<*>,
    ): ReadWriteProperty<CustomElementAdapter, T> {
        val attributeName = name ?: thisRef.attributeNameOf(property.name)
        val delegate = AttributeDelegate(
            element = thisRef,
            name = attributeName,
            default = default,
            codec = codec ?: thisRef.codecOf(property.name)?.let { unsafeCast(it) } ?: missingCodec(property.name, attributeName),
            onChange = onChange,
        )
        thisRef.registerAttributeDelegate(delegate)
        return delegate
    }

    private fun missingCodec(property: String, attribute: String): Nothing = error(
        "attr: no AttributeCodec for '$property' (attribute '$attribute'). Kotlin/JS erases the type at " +
            "runtime, so a type outside String / Boolean / Int / Double / enum needs attr(codec = …). " +
            "ktjs-dom-kit-ksp generates the enum one; a module without the processor writes it out.",
    )
}
