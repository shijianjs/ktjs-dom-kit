---
sidebar_position: 2
---

# Custom elements

A component is a Kotlin class that extends `CustomElementAdapter`, declares its attributes with
[`attr`](./attributes.md), and is registered as a tag with `defineIfMissing`. No JavaScript class
literal, no component library, and no `@JsName`-aliased lifecycle hooks.

```kotlin
class KtjsBadge : CustomElementAdapter() {

    private val anchor = TagName<HTMLAnchorElement>("a").createElement()

    var href: String by attr { change -> anchor.href = change.newValue }

    init {
        // A shadow root may be attached from the constructor; appending to the element itself may not.
        attachShadowRoot().append(anchor)
    }

    override fun connectedCallback() {
        applyInitialAttributes()
    }

    companion object : CustomElementReference<KtjsBadge>(KtjsBadge::class.js) {
        val NAME: TagName<KtjsBadge> = TagName("ktjs-badge")
    }
}

defineIfMissing(KtjsBadge.NAME, KtjsBadge.Companion)
```

## What the adapter does

`CustomElementAdapter : HTMLElement()` takes over the three things a component would otherwise repeat
in every class:

1. **The lifecycle hooks**, declared as overridable methods under their native names:
   `connectedCallback`, `connectedMoveCallback`, `disconnectedCallback`, `adoptedCallback`,
   `attributeChangedCallback`. An element class writes `override fun connectedCallback()` and the
   browser calls it.
2. **The `attr` delegates** of its subclasses. Each property registers its delegate with the element
   when it is initialised, and one `MutationObserver` on the element feeds every later attribute
   change back into them, so a property and its attribute cannot disagree. See
   [Attributes](./attributes.md).
3. **The bridge removal** described below, through `stripBridgesFrom`.

`attributeChangedCallback` is declared for completeness: it only fires for the names listed in the
class's static `observedAttributes`, which is the element class's own business. The `attr` delegates
do not go through it — a delegate is not a static declaration, and it has to work for a detached
element too.

## Why the official `CustomElement` interface is not implemented

`web.components.CustomElement` declares its hooks as `val`s. Kotlin compiles an `override val
connectedCallback` into a **getter**, and the browser only looks for a method, so the element renders
but the callback never runs — a silent failure with no error to search for. Declaring the same names
as real functions is what makes them callable, and doing it once in the base class is why no element
needs an alias like `onConnected`.

## The self-recursive bridges

Kotlin/JS emits a forwarding member on a class for every external DOM member it inherits
(`ParentNode`, `ChildNode`, `Animatable`, `HTMLOrSVGElement`, `EventTargetLike`, …). The generated
member calls itself:

```js
querySelector(selectors) { return this.querySelector(selectors); }
```

The first call inside `connectedCallback` that goes through one of those names — `querySelector`,
`querySelectorAll`, `append`, `animate`, `focus`, `addEventListener`, … — recurses until the stack
overflows. `getAttribute` / `setAttribute` / `hasAttribute` / `textContent` need no fix: they are
abstract members of the external `Element` class, so no bridge is generated for them at all.

Reported upstream as
[kotlin-wrappers#2932](https://github.com/JetBrains/kotlin-wrappers/issues/2932), which is where the
compiler-side explanation lives: the bridge is generated for members declared `= definedExternally`
on external *interfaces* (a member of an external *class* such as `getAttribute` is abstract and
stops at the `ABSTRACT` guard), it is named after its own target, and the call inside it therefore
resolves to itself. The JVM backend has a guard against exactly this pattern (`isErroneousSpecialBridge`,
KT-82651); JS does not, so the fix has to live here until it does.

`stripBridgesFrom(klass)` deletes those names from every prototype in the class's chain, stopping
before `HTMLElement.prototype`, where they are the real DOM methods. Deleting a name that was never
bridged is a no-op, so the list can stay wider than a given class strictly needs. It walks the whole
chain on purpose: Kotlin puts a bridge on **each** class, so cleaning only `CustomElementAdapter`
would leave the element class's own copy in place.

After that, the normal spelling works — `querySelectorAll(...)`, `append(...)`, `animate(...)` — with
no `…Super` forwarders. (A `@JsName` alias is not even allowed on an override.)

## Defining the tag

```kotlin
defineIfMissing(KtjsBadge.NAME, KtjsBadge.Companion)
```

`defineIfMissing(tag, reference)` runs `stripBridgesFrom` and then `customElements.define`, and
returns immediately if the tag is already registered. The order matters and is the reason to prefer
it over calling `customElements.define` yourself: defining a tag upgrades any matching element
already in the document, whose `connectedCallback` runs right away — before the bridges are gone, that
is a stack overflow. Calling it twice is safe, which is what makes it usable from a module that may
be evaluated more than once.

| Piece | Comes from |
| --- | --- |
| `NAME: TagName<T>` | your element's `companion object`; the tag name lives in one place per element |
| `CustomElementReference<T>(T::class.js)` | `web.components`; the companion object **is** the reference passed to `customElements.define` |
| `defineIfMissing` | `ktjs-dom-kit` |

## Creating an element

`TagName<T>.createElement { … }` wraps the typed `document.createElement(TagName<T>)`, so the tag
name decides the property type and the callback receives it without a cast:

```kotlin
val action = KtjsAction.NAME.createElement {
    kind = ActionKind.Primary      // the same property MDX writes as kind="primary"
    href = "docs/intro"
    textContent = "Get started"
}
```

## Constructor rules

The Custom Elements specification forbids a constructor from adding attributes or children to the
element itself. `attachShadow()` is explicitly allowed, so a component builds its subtree into its
shadow root:

```kotlin
class KtjsBadge : CustomElementAdapter() {
    init {
        attachShadowRoot().append(anchor)      // fine: the shadow root is not the element
        // this.setAttribute("…", "…")         // throws NotSupportedError on createElement
        // this.append(anchor)                 // same
    }
}
```

Two consequences worth knowing:

- **Do not set an attribute on the element in its constructor.** `document.createElement("ktjs-badge")`
  throws `NotSupportedError` when the constructed element already carries attributes (the parser and
  the upgrade path do not). A style the element always needs therefore goes on `:host` in its own
  sheet, not into a `style` attribute.
- **Listeners follow their owner.** A listener on one of the component's own nodes belongs in the
  constructor and needs no cleanup; a listener on `document`, `window` or `matchMedia` belongs in
  `disconnectedCallback`, guarded by a flag, because moving an element re-runs `connectedCallback`.
