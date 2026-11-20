package io.github.shijianjs.ktjs.dom.webcomponent

import io.github.shijianjs.ktjs.dom.webcomponent.attr.AttributeDelegate
import io.github.shijianjs.ktjs.dom.webcomponent.attr.AttributeProvider
import io.github.shijianjs.ktjs.dom.webcomponent.attr.attr
import js.objects.unsafeJso
import kotlin.js.JsAny
import web.components.CustomElement
import web.html.HTMLElement
import web.mutation.MutationObserver
import web.mutation.MutationObserverInit

/**
 * Base class for a custom element written in Kotlin.
 *
 * It does three things a site should not have to repeat:
 *
 * 1. Implements `web.components.CustomElement.WithCallbacks` on top of `HTMLElement`, so every
 *    lifecycle hook is the interface method the browser calls, spelled the native way:
 *    `connectedCallback`, `connectedMoveCallback`, `disconnectedCallback`, `adoptedCallback`,
 *    `attributeChangedCallback`. Element classes just `override fun connectedCallback()` and friends;
 *    no `@JsName` is needed (and Kotlin forbids it on overrides anyway).
 *
 *    `attributeChangedCallback` only fires for attributes listed in the constructor's static
 *    `observedAttributes`; declaring that is up to the element class. The [attr] delegates below do
 *    not go through it — see [applyInitialAttributes].
 *
 * 2. Hosts the [attr] delegates of its subclasses. Each delegate is registered here when the property
 *    is initialised, and a [MutationObserver] on this element feeds every later attribute change back
 *    into them, so a property and its attribute cannot disagree:
 *
 *    ```kotlin
 *    class MyElement : CustomElementAdapter() {
 *        val badge: HTMLElement = …
 *        var label: String by attr { change -> badge.textContent = change.newValue }
 *
 *        override fun connectedCallback() {
 *            applyInitialAttributes()   // markup that was already there, once
 *        }
 *    }
 *    ```
 *
 *    The observer is used instead of the spec's `observedAttributes` on purpose: that list is read by
 *    `customElements.define` from a static getter on the class, and the attribute names are only known
 *    per property, at construction. A `MutationObserver` also sees attributes nobody declared, and it
 *    reports them for a detached element too.
 *
 * 3. Removes the self-recursive bridges Kotlin/JS emits for every external DOM member (`ParentNode`,
 *    `ChildNode`, `Animatable`, `HTMLOrSVGOrMathMLElement`, `EventTargetLike`, …):
 *
 *    ```js
 *    querySelectorAll(selectors) { return this.querySelectorAll(selectors); }
 *    ```
 *
 *    Call [stripBridgesFrom] once per element class before `customElements.define`. It walks the
 *    class's prototype chain down to (but excluding) `HTMLElement.prototype` and deletes those names,
 *    so instance lookups fall through to the native DOM methods and the normal spelling works:
 *    `querySelectorAll(...)`, `append(...)`, `animate(...)`, `focus()`, … No `…Super` forwarders and
 *    no aliases are needed.
 *
 * `getAttribute` / `setAttribute` / `hasAttribute` / `textContent` need no stripping: they are
 * abstract members of the external `Element` class, so no bridge is generated for them.
 *
 * This is a workaround for [KT-91505](https://youtrack.jetbrains.com/issue/KT-91505) (wrappers-side
 * report: kotlin-wrappers#2932), fixed by the compiler in 2.5.20-Beta1. 2.2.20, 2.4.21 and 2.5.0-RC
 * are the affected releases and this repository builds with 2.4.21, so the call stays; after an
 * upgrade it only deletes names the compiler no longer emits.
 */
open class CustomElementAdapter : HTMLElement(), CustomElement.WithCallbacks {

    /** Every `attr` delegate of this instance, in declaration order. */
    private val attributeDelegates: MutableList<AttributeDelegate<*>> = mutableListOf()

    private var attributeObserver: MutationObserver? = null

    override fun connectedCallback() {}

    override fun connectedMoveCallback() {}

    override fun disconnectedCallback() {}

    override fun adoptedCallback() {}

    /**
     * Notifies every [attr] delegate of the value its attribute has right now.
     *
     * Markup that was already parsed — and anything set before the element was constructed — never
     * reaches a property setter, so the callbacks have not seen it. Call this once from
     * `connectedCallback`, after the properties are initialised:
     *
     * ```kotlin
     * override fun connectedCallback() {
     *     applyInitialAttributes()
     * }
     * ```
     *
     * Nothing is written back to the markup and no delegate is notified twice: a delegate that has
     * already reported a value is skipped unless the attribute changed in the meantime. Calling it
     * again (on reconnect, for instance) is therefore safe.
     *
     * @param writeDefaults also copies each delegate's `default` into its attribute when the
     *   attribute is absent. `false` keeps the markup as written — a default is a Kotlin-side value,
     *   not a declaration.
     */
    fun applyInitialAttributes(writeDefaults: Boolean = false) {
        for (delegate in attributeDelegates) {
            if (writeDefaults) delegate.writeDefault()
        }
        // Whatever the observer has queued is about to be read anyway; dropping the records keeps
        // its callback from walking the delegates a second time.
        attributeObserver?.takeRecords()
        syncAttributeDelegates()
    }

    /** Called by [AttributeProvider.provideDelegate]; one observer per element, installed on first use. */
    internal fun registerAttributeDelegate(delegate: AttributeDelegate<*>) {
        attributeDelegates.add(delegate)
        if (attributeObserver != null) return

        val observer = MutationObserver { _, _ -> syncAttributeDelegates() }
        observer.observe(this, unsafeJso<MutationObserverInit> { attributes = true })
        attributeObserver = observer
    }

    private fun syncAttributeDelegates() {
        for (delegate in attributeDelegates) delegate.sync()
    }

    override fun attributeChangedCallback(name: String, oldValue: JsAny?, newValue: JsAny?) {}
}
