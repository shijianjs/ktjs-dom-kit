---
sidebar_position: 3
---

# Custom elements

A component is a Kotlin class that extends `CustomElementAdapter`, declares its attributes with
[`attr`](./attributes.md), and extends the reference `ktjs-dom-kit-ksp` generates for it. No
JavaScript class literal, no component library, and no `@JsName`-aliased lifecycle hooks.

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

    companion object : KtjsBadgeDefaultReference()
}

val badge: KtjsBadge = KtjsBadge.NAME { href = "docs/intro" }
```

Not one literal in that file says what the tag is or which attributes it observes: `KtjsBadgeDefaultReference`
is the class name in kebab case behind the configured prefix, observing every `by attr` property.
[What the processor generates](#what-the-processor-generates) is the whole of it.

## What the adapter does

`CustomElementAdapter : HTMLElement()` takes over the three things a component would otherwise repeat
in every class:

1. **The lifecycle hooks**, declared as overridable methods under their native names:
   `connectedCallback`, `connectedMoveCallback`, `disconnectedCallback`, `adoptedCallback`,
   `attributeChangedCallback`. An element class writes `override fun connectedCallback()` and the
   browser calls it.
2. **The `attr` delegates** of its subclasses. Each property registers its delegate with the element
   when it is initialised, and the browser's synchronous `attributeChangedCallback` feeds every later
   change of a *registered* attribute back into the delegate of that name, so a property and its
   attribute cannot disagree — and they never do, not even for one microtask. What "registered" means
   is the next section; the names come from the generated reference. See [Attributes](./attributes.md).
3. **The bridge removal** described below, through `stripBridgesFrom`.

## `observedAttributes` is the contract

`attributeChangedCallback` is the platform's own hook, and it only fires for attributes the class's
static `observedAttributes` lists. That list is read once, by `customElements.define`, so it has to
exist **before** the companion's `init` defines the tag — before any instance, and long before a
delegate exists. Nothing on the Kotlin side can answer the question at that moment: the delegates are
created per instance, in the constructor, and a Kotlin/JS class has no static initializer that could
see its own properties. The compiler can, so the list is generated rather than written.

The alternative — a hand-kept array beside the declarations — repeats what the `by attr` properties
already say, and a forgotten name is not a loud failure: the property still reads the attribute, the
initial value still arrives (through `applyInitialAttributes()` or the upgrade callback), but a
**later** external write to that attribute goes unreported. Two guards survive for a reference written
by hand:

- `applyInitialAttributes()` is the catch-up that runs once the element is connected, so a missed name
  still reports the markup it was created with.
- `AttributeProbe` in this repository's browser tests compares the generated array with what the
  properties actually registered, so a name the processor misses fails the build instead of going quiet.

## What the processor generates

`ktjs-dom-kit-ksp` reads the class declarations and writes, next to the element, both the names and the
reference its companion extends:

```mermaid
flowchart LR
  SRC["class KtjsHero : CustomElementAdapter()<br/>var logoSrc by attr { }<br/>@AttributeName(&quot;title&quot;) var titleText by attr { }<br/>optional @CustomElementTag"]
  SRC --> KSP["ktjs-dom-kit-ksp<br/>property name → kebab case, @AttributeName wins<br/>class name → tag, prefixed unless it already is"]
  KSP --> ATTRS["KtjsHeroAttributes<br/>const val LOGO_SRC · TITLE<br/>ALL · BY_PROPERTY"]
  KSP --> REF["KtjsHeroDefaultReference<br/>name · observedAttributes · attributeNames"]
  ATTRS --> REF
  REF --> COMP["companion object : KtjsHeroDefaultReference()"]
  ATTRS --> LOOKUP["by attr without a name<br/>resolves through BY_PROPERTY"]
```

The object holds the constants and the two tables the runtime needs:

```kotlin
internal object KtjsHeroAttributes {
    const val LOGO_SRC: String = "logo-src"
    const val TITLE: String = "title"
    val ALL: Array<String> = arrayOf(LOGO_SRC, TITLE)
    val BY_PROPERTY: Map<String, String> = mapOf("logoSrc" to LOGO_SRC, "titleText" to TITLE)
}
```

`ALL` is what the browser reads as `observedAttributes`. `BY_PROPERTY` exists for the other half of the
problem: a delegated property cannot see its own class at runtime, only its name, so `by attr` needs a
table to learn that `titleText` stands for `title`. The reference registers that table at
`customElements.define`, before any instance exists, and every delegate created afterwards reads it.

The reference fills in every argument, so the companion is one line:

```kotlin
internal open class KtjsHeroDefaultReference(
    name: String = "ktjs-hero",
    formAssociated: Boolean? = null,
    observedAttributes: ReadonlyArray<JsString>? = KtjsHeroAttributes.ALL,
    register: Boolean = true,
    attributeNames: Map<String, String>? = KtjsHeroAttributes.BY_PROPERTY,
) : CustomElementReferenceAdapter<KtjsHero>(…)
```

Its parameters stay open on purpose: a tag that is defined later passes `register = false`
(`UpgradeProbeDefaultReference(register = false)` in the tests), and a site that wants a different tag
passes `name = …`.

The element then writes neither an attribute name nor a reference:

```kotlin
class KtjsHero : CustomElementAdapter() {
    var logoSrc: String by attr { … }

    @AttributeName("title")                                     // the attribute is not `title-text`
    var titleText: String by attr { … }

    companion object : KtjsHeroDefaultReference()
}
```

Rules the generator follows:

- **An attribute name** is the property name in kebab case (`logoSrc` → `logo-src`, `maxCount` →
  `max-count`) unless `@AttributeName` says otherwise. Which properties are attributes at all is
  `isDelegated()`: inside an adapter, the only delegated properties are `attr` ones. Two properties
  mapping to the same attribute is a compile error.
- **The tag** is the class name in kebab case, prefixed with `ktjs.tagPrefix` (default `ktjs-`) only
  when the name does not already start that way. Both families name their classes after their mark,
  so `KtjsHero` is `ktjs-hero` — not `ktjs-ktjs-hero` — while `StepBadge` in a `demo-` module is
  `demo-step-badge`. A module declares the prefix once:

  ```kotlin
  ksp { arg("ktjs.tagPrefix", "demo-") }        // StepBadge → demo-step-badge
  ```

  and a single class that does not fit passes `@CustomElementTag("…")` instead of the option.
- **The visibility follows the element**: an `internal` element gets `internal` declarations, because a
  public companion cannot extend an `internal` supertype.

Wire it in two lines, next to the element's own module:

```kotlin
plugins { id("com.google.devtools.ksp") }

dependencies { add("kspJs", project(":ktjs-dom-kit-ksp")) }
```

`AttributeProbe` and `UpgradeProbe` in this repository's browser tests are the reference wiring: the
processor runs over `jsTest`, both classes carry `@CustomElementTag` (their tags are `ktjs-attr-probe`
and `ktjs-attr-upgrade-probe`, not the class names), and `theGeneratedListMatchesTheDeclarations`
compares `ALL` with the names the properties registered. The `demo` module is the same wiring in an
application shape: `ktjs.tagPrefix = "demo-"`, nine elements, no literal tag anywhere, and
`GeneratedReferenceTest` writes attributes onto them from the outside and reads the repainted shadow.

Nothing else is generated: a module that does not run the processor still compiles, because
`CustomElementReferenceAdapter` itself is public and takes the same arguments by hand — that is the
fallback, not the intended path.

## The official `CustomElement` interface

The adapter is `CustomElementAdapter : HTMLElement(), CustomElement.WithCallbacks`. Declaring the hooks
as `override fun`s is what makes the interface work: `web.components.CustomElement` declares them as
`val`s, Kotlin compiles an `override val connectedCallback` into a **getter**, and the browser only
looks for a method — the element would render and every callback would be silently skipped.
`CustomElement.WithCallbacks` is the variant that spells them as functions, so overriding
`connectedCallback` here is the method the browser calls, once, in the base class instead of in every
element.

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
[kotlin-wrappers#2932](https://github.com/JetBrains/kotlin-wrappers/issues/2932) and, on the compiler
side, as [KT-91505](https://youtrack.jetbrains.com/issue/KT-91505): the bridge is generated for
members declared `= definedExternally` on external *interfaces* (a member of an external *class* such
as `getAttribute` is abstract and stops at the `ABSTRACT` guard), it is named after its own target,
and the call inside it therefore resolves to itself. The JVM backend has a guard against exactly this
pattern (`isErroneousSpecialBridge`, KT-82651); the JS backend got one in Kotlin **2.5.20-Beta1**
(build `2.5.20-dev-4666`). 2.4.21 — the version this repository compiles with — is one of the listed
affected versions (alongside 2.2.20 and 2.5.0-RC), so the workaround still has to live here. Once the
toolchain is upgraded the compiler stops emitting those bridges, deleting them becomes a no-op, and
`stripBridgesFrom` can be retired from `defineIfMissing` and the public API in one change — the
element code itself needs no edit.

`stripBridgesFrom(klass)` deletes those names from every prototype in the class's chain, stopping
before `HTMLElement.prototype`, where they are the real DOM methods. Deleting a name that was never
bridged is a no-op, so the list can stay wider than a given class strictly needs. It walks the whole
chain on purpose: Kotlin puts a bridge on **each** class, so cleaning only `CustomElementAdapter`
would leave the element class's own copy in place.

After that, the normal spelling works — `querySelectorAll(...)`, `append(...)`, `animate(...)` — with
no `…Super` forwarders. (A `@JsName` alias is not even allowed on an override.)

## Defining the tag

```kotlin
companion object : CustomElementReferenceAdapter<KtjsBadge>(KtjsBadge::class.js, "ktjs-badge")
```

`CustomElementReferenceAdapter<T>(clazz, name, formAssociated, observedAttributes, register)` extends
the `web.components.CustomElementReference` the browser wants — the three original arguments are passed
through unchanged — and adds the two things an element needs on top: `NAME`, a typed `TagName<T>`, and
registration inside its `init`, which calls `defineIfMissing(NAME, this)` unless it was built with
`register = false`.

`defineIfMissing(tag, reference)` runs `stripBridgesFrom` and then `customElements.define`, and returns
immediately if the tag is already registered. The order matters and is the reason not to call
`customElements.define` yourself: defining a tag upgrades any matching element already in the
document, whose `connectedCallback` runs right away — before the bridges are gone, that is a stack
overflow. Calling it twice is safe, which is what makes it usable from a module that may be evaluated
more than once.

Because registration happens in the companion's `init`, **reading `NAME` is the whole protocol**: it
registers the tag (upgrading any markup already parsed) and creates the element in one step, and the
element's own methods are in scope inside `NAME { … }`.

```mermaid
flowchart TB
  D["companion object : CustomElementReferenceAdapter(X::class.js, \"tag\")"] --> I["init { if (register) defineIfMissing(NAME, this) }"]
  I --> S["stripBridgesFrom(klass)<br/>delete the forwarding bridges on every prototype,<br/>stopping before HTMLElement.prototype"]
  S --> R["customElements.define(tag, companion)"]
  R --> U["matching markup upgrades now"]
  U --> AC["attributeChangedCallback<br/>once per observed attribute the markup carried"]
  AC --> C["connectedCallback<br/>shadow subtree already built in the constructor"]
  C --> A["applyInitialAttributes()<br/>defaults, plus an idempotent catch-up"]
  N["X.NAME { … }"] --> R
```


| Piece | Comes from |
| --- | --- |
| `CustomElementReferenceAdapter<T>(T::class.js, "tag")` | `ktjs-dom-kit`; wraps `web.components.CustomElementReference`, registers in `init` |
| `NAME: TagName<T>` | the adapter; one tag name per element, and reading it registers and creates |
| `defineIfMissing` | `ktjs-dom-kit`; what the adapter calls, and what to call by hand only for a `register = false` tag |

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
