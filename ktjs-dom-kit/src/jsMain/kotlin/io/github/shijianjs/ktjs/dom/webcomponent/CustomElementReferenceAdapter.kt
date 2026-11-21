package io.github.shijianjs.ktjs.dom.webcomponent

import js.array.ReadonlyArray
import js.reflect.JsClass
import kotlin.js.JsString
import web.components.CustomElementReference
import web.dom.TagName
import web.html.HTMLElement

/**
 * The companion-object half of a custom element: it knows its class, its tag and who registers it.
 *
 * A Kotlin/JS element is defined once, and the companion object of the element class is the natural
 * owner of that "once": the object is created the first time anything touches it, so declaring
 *
 * ```kotlin
 * class DemoCounter : CustomElementAdapter() {
 *     companion object : DemoCounterDefaultReference()
 * }
 * ```
 *
 * already registers the tag — no `define()` to call, no `newCounter()` factory to forget. Creation is
 * the reference's own tag applied to a block, which is the same call the DOM helpers offer for native
 * tags:
 *
 * ```kotlin
 * val counter = DemoCounter.NAME { value = 3 }
 * ```
 *
 * `DemoCounterDefaultReference` is what `ktjs-dom-kit-ksp` generates: a subclass with every argument
 * filled in — the class, the tag (the class name in kebab case, behind a configured prefix unless the
 * name already carries it), the `observedAttributes` array and the property-name table the `attr`
 * delegations read. Extending this class by hand, spelling out those arguments once per element, is
 * the fallback for a module that does not run the processor.
 *
 * The three constructor parameters of [CustomElementReference] keep their meaning and default; [name]
 * is the tag, and [register] is the one addition — `false` keeps the reference (the constructor, the
 * `formAssociated` / `observedAttributes` statics) without defining anything yet, for the case where
 * a site decides at runtime whether a tag should exist.
 *
 * Registration order matters and is handled inside: [defineIfMissing] strips the compiler-generated
 * bridges before `customElements.define`, because the tag upgrades any element already in the
 * document.
 */
open class CustomElementReferenceAdapter<T : HTMLElement>(
    clazz: JsClass<T>,
    name: String,
    formAssociated: Boolean? = null,
    observedAttributes: ReadonlyArray<JsString>? = null,
    register: Boolean = true,
    /**
     * Property name → attribute name for this element class. Registered before the tag is defined,
     * so every instance constructed afterwards — the ones `define` upgrades included — resolves
     * `by attr` without a name argument. The generated `…DefaultReference` passes
     * `…Attributes.BY_PROPERTY`; a hand-written reference leaves it `null` and keeps the kebab-case
     * rule.
     */
    attributeNames: Map<String, String>? = null,
) : CustomElementReference<T>(clazz, formAssociated, observedAttributes) {

    /** The tag this reference defines; applied to a block it creates the element and configures it. */
    val NAME: TagName<T> = TagName(name)

    init {
        if (attributeNames != null) AttributeNameRegistry.register(clazz, attributeNames)
        if (register) defineIfMissing(NAME, this)
    }
}
