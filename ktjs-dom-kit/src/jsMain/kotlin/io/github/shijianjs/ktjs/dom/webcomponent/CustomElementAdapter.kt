package io.github.shijianjs.ktjs.dom.webcomponent

import io.github.shijianjs.ktjs.dom.webcomponent.attr.AttributeDelegate
import io.github.shijianjs.ktjs.dom.webcomponent.attr.AttributeProvider
import io.github.shijianjs.ktjs.dom.webcomponent.attr.attr
import kotlin.js.JsAny
import web.components.CustomElement
import web.html.HTMLElement

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
 * 2. Hosts the [attr] delegates of its subclasses and feeds the browser's
 *    `attributeChangedCallback` into them, so a property and its attribute cannot disagree:
 *
 *    ```kotlin
 *    class MyElement : CustomElementAdapter() {
 *        val badge: HTMLElement = …
 *        var label: String by attr { change -> badge.textContent = change.newValue }
 *
 *        companion object : MyElementDefaultReference()
 *    }
 *    ```
 *
 *    The companion is what `ktjs-dom-kit-ksp` generates — see [CustomElementReferenceAdapter] for
 *    the tag, the observed list and the attribute-name table it fills in from these declarations.
 *    The callback is synchronous, so the report happens before the statement that changed the
 *    attribute returns — no microtask, no frame in which the property and the attribute disagree.
 *    Its price is the contract the platform itself imposes: `attributeChangedCallback` only fires for
 *    attributes the class's static `observedAttributes` lists, and that list is read once, at
 *    `customElements.define` time. The generated reference passes the element's `ALL` array, so every
 *    `by attr` property is listed; an element whose reference is written by hand must list them all.
 *    See [applyInitialAttributes] for the one-time catch-up that keeps a missing entry from silently
 *    doing nothing at startup.
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

    /**
     * The same delegates, looked up by attribute name: [attributeChangedCallback] is handed a name
     * and must reach the one property that stands for it without walking the whole list.
     *
     * A name can back more than one property (`by attr("second")` declared twice), so the value is a
     * list; every one of them is re-read on a change.
     */
    private val delegatesByAttribute: MutableMap<String, MutableList<AttributeDelegate<*>>> = mutableMapOf()

    override fun connectedCallback() {}

    override fun connectedMoveCallback() {}

    override fun disconnectedCallback() {}

    override fun adoptedCallback() {}

    /**
     * Notifies every [attr] delegate of the value its attribute has right now.
     *
     * The browser already reports an observed attribute once the element is upgraded, so this is not
     * the primary path any more; what it is for:
     *
     * - **Defaults.** With `writeDefaults = true` it copies each `default` into a missing attribute
     *   first, so the markup a page is handed matches the values the properties report.
     * - **Catch-up.** A delegate whose name is missing from `observedAttributes` never hears about
     *   later writes, but the value the attribute has at this point is still reported — which is
     *   exactly the markup case, and the reason a forgotten entry does not turn startup silent.
     *
     * Nothing is written back to the markup beyond the defaults, and no delegate is notified twice:
     * a delegate that has already reported a value is skipped unless the attribute changed in the
     * meantime. Calling it again (on reconnect, for instance) is therefore safe.
     *
     * @param writeDefaults also copies each delegate's `default` into its attribute when the
     *   attribute is absent. `false` keeps the markup as written — a default is a Kotlin-side value,
     *   not a declaration.
     */
    fun applyInitialAttributes(writeDefaults: Boolean = false) {
        for (delegate in attributeDelegates) {
            if (writeDefaults) delegate.writeDefault()
        }
        syncAttributeDelegates()
    }

    /** Called by [AttributeProvider.provideDelegate]; registers the delegate under its attribute name. */
    internal fun registerAttributeDelegate(delegate: AttributeDelegate<*>) {
        attributeDelegates.add(delegate)
        delegatesByAttribute.getOrPut(delegate.name) { mutableListOf() }.add(delegate)
    }

    private fun syncAttributeDelegates() {
        for (delegate in attributeDelegates) delegate.sync()
    }

    /**
     * The attribute names this instance's delegates answer to, in declaration order.
     *
     * Internal, and for one check only: the `ALL` array a companion hands to `observedAttributes` must
     * be exactly this list, and a name the processor failed to generate would otherwise fail
     * silently — the attribute would read fine and only later external writes would go unreported.
     */
    internal fun attributeNames(): List<String> = attributeDelegates.map { it.name }

    /**
     * The platform hook: called synchronously for every change to an attribute listed in the class's
     * static `observedAttributes` — a write from anywhere, a removal, and once per attribute the
     * element already carried when `customElements.define` upgraded it.
     *
     * The value is not read from [newValue]: a delegate re-reads its attribute, which keeps the
     * codec's `absent` / `default` handling in one place, and makes "the property is the attribute"
     * true regardless of which path asked.
     */
    override fun attributeChangedCallback(name: String, oldValue: JsAny?, newValue: JsAny?) {
        val delegates = delegatesByAttribute[name] ?: return
        for (delegate in delegates) delegate.sync()
    }
}
