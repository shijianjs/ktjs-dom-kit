package io.github.shijianjs.ktjs.dom.webcomponent

import js.array.ReadonlyArray
import js.reflect.JsClass
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
 *     companion object : CustomElementReferenceAdapter<DemoCounter>(DemoCounter::class.js, "demo-counter")
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
) : CustomElementReference<T>(clazz, formAssociated, observedAttributes) {

    /** The tag this reference defines; applied to a block it creates the element and configures it. */
    val NAME: TagName<T> = TagName(name)

    init {
        if (register) defineIfMissing(NAME, this)
    }
}
