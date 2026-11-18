package io.github.shijianjs.ktjs.dom.attribute

import io.github.shijianjs.ktjs.dom.attribute.types.BooleanAttributeCodec
import io.github.shijianjs.ktjs.dom.attribute.types.DoubleAttributeCodec
import io.github.shijianjs.ktjs.dom.attribute.types.IntAttributeCodec
import io.github.shijianjs.ktjs.dom.attribute.types.StringAttributeCodec
import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementAdapter
import js.reflect.unsafeCast

/**
 * Backs a property with an HTML attribute.
 *
 * ```kotlin
 * class KtjsBadge : CustomElementAdapter() {
 *     var href: String by attr { change -> anchor.href = change.newValue }
 *     var height: Int by attr("height", default = 20) { change -> image.height = change.newValue }
 * }
 * ```
 *
 * - **No duplicated state.** The getter reads the attribute and decodes it, the setter encodes and
 *   writes it, so the DOM is the only place the value lives. An attribute set from outside (markup,
 *   `setAttribute`, devtools) is visible to the getter immediately.
 * - **The name defaults to the property name** in kebab-case (`logoSrc` → `logo-src`); pass [name]
 *   for anything else. Native spellings are not special-cased: `tabIndex` needs
 *   `attr("tabindex")`, and a property named like an `HTMLElement` member (`title`, `id`, `hidden`)
 *   has to be named differently and mapped explicitly (`val titleText: String by attr("title")`).
 * - **Add `@JsName` when JavaScript should see the property.** Kotlin/JS compiles a delegated
 *   property to module-mangled accessor methods (`get_text_8f3k2_k$`) and *no* JS property; only
 *   `@JsName("titleText")` makes it emit real `get` / `set` accessors, so that
 *   `element.titleText = "x"` works from the page. Kotlin callers do not need it.
 * - **Values are converted by [codec]**, which defaults to the built-in one for [T]
 *   (`StringAttributeCodec`, `BooleanAttributeCodec`, `IntAttributeCodec`, `DoubleAttributeCodec`).
 *   Pass a codec for any other type; see `enumAttributeCodec`.
 * - **A string assigned from JavaScript is the attribute's raw value.** The property is also the
 *   element's JS-facing API, and a JS caller only has the value it was given: React 19 assigns every
 *   matching JSX prop to the property, so `<ktjs-action kind="secondary">` becomes
 *   `element.kind = "secondary"`. That string is written to the attribute verbatim, exactly where
 *   `setAttribute` would have put it; a value of the property's own type still goes through [codec].
 * - **[default] is not written to the markup**; it is what the getter returns while the attribute is
 *   absent. Removing the attribute goes back to it.
 * - **[onChange] runs whenever the decoded value really changed** — from the setter, from outside, or
 *   from [CustomElementAdapter.applyInitialAttributes] during `connectedCallback`. Writing the same
 *   value again is not a change, and removing the attribute is one.
 *
 * Only *reflecting* properties belong here: `attr` writes the attribute, and an element that reads
 * its own attribute back (as these components do) is a reflecting property. `input.value` is not one
 * — it is internal state that outlives the attribute — so it stays an ordinary property.
 */
inline fun <reified T> attr(
    name: String? = null,
    default: T? = null,
    codec: AttributeCodec<T>? = null,
    noinline onChange: (AttributeChange<T>) -> Unit = {},
): AttributeProvider<T> = attributeProvider(name, default, codec ?: defaultAttributeCodec<T>(), onChange)

/** Constructs the provider outside the inline function; see [attr]. */
@PublishedApi
internal fun <T> attributeProvider(
    name: String?,
    default: T?,
    codec: AttributeCodec<T>,
    onChange: (AttributeChange<T>) -> Unit,
): AttributeProvider<T> = AttributeProvider(name, default, codec, onChange)

/**
 * The codec [attr] picks when the caller passes none.
 *
 * Kotlin/JS has no runtime generics, so the type is matched on the reified [T] instead of by
 * reflection on a value.
 */
@PublishedApi
internal inline fun <reified T> defaultAttributeCodec(): AttributeCodec<T> {
    val codec: AttributeCodec<*> = when (T::class) {
        String::class -> StringAttributeCodec
        Boolean::class -> BooleanAttributeCodec
        Int::class -> IntAttributeCodec
        Double::class -> DoubleAttributeCodec
        else -> error(
            "No built-in AttributeCodec for ${T::class.simpleName}; " +
                "pass one with attr(codec = …), or use enumAttributeCodec(values).",
        )
    }
    return unsafeCast(codec)
}
