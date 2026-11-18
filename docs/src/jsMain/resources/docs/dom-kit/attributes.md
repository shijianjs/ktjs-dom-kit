---
sidebar_position: 3
---

# Attributes

`attr` backs a Kotlin property with an HTML attribute:

```kotlin
class KtjsBadge : CustomElementAdapter() {
    var href: String by attr { change -> anchor.href = change.newValue }
    var height: Int by attr("height", default = 20) { change -> image.height = change.newValue }
}
```

The attribute is the property. The getter reads and decodes it, the setter encodes and writes it, and
nothing is kept on the Kotlin side except the last reported value, which exists only to tell a real
change from a repeated write. Markup, `setAttribute`, devtools and Kotlin all end up in the same
place, so there is no second copy to keep in sync.

`attr` is an inline function taking `name`, `default`, `codec` and the change callback, all optional:

```kotlin
var logoSrc: String by attr                                              // logo-src
var ratio: Double by attr("data-ratio", default = 1.0) { … }             // explicit name
var kind: ActionKind by attr(codec = enumAttributeCodec(ActionKind.entries), default = ActionKind.Primary)
```

## Naming

The attribute name defaults to the property name in kebab case: `maxCount` → `max-count`,
`XMLHttp` → `xml-http`. A dash goes before an upper-case letter that starts a word; digits and
existing dashes are left alone.

Native spellings are not special-cased. `tabIndex` needs `attr("tabindex")`, and a property whose
name is already an `HTMLElement` member (`title`, `id`, `hidden`) has to be named differently and
mapped explicitly:

```kotlin
@JsName("titleText")
var titleText: String by attr("title") { change -> heading.textContent = change.newValue }
```

**Add `@JsName` when JavaScript should see the property.** Kotlin/JS compiles a delegated property to
module-mangled accessor methods and *no* JS property, so `element.titleText = "x"` from the page
silently writes a new expando field instead. `@JsName("titleText")` is what makes it emit real
`get`/`set` accessors. Kotlin callers do not need it. That is also the reason the property name and
the attribute name are two different things in this API — `titleText` is what TypeScript sees,
`title` is what the browser stores, and `@JsName` + `attr("title")` is the bridge between them.

## Values and codecs

The value is converted by an `AttributeCodec<T>`. The built-in ones:

| Codec | Property type | Behaviour |
| --- | --- | --- |
| `StringAttributeCodec` | `String` | verbatim; a missing attribute reads as `""`, so a `String` property is never null |
| `BooleanAttributeCodec` | `Boolean` | presence, like `hidden` — `<x flagged>` and `<x flagged="false">` are both `true`; `true` writes a bare attribute, `false` removes it |
| `IntAttributeCodec` | `Int` | `toIntOrNull`; anything else falls back to `default` (or `0`) |
| `DoubleAttributeCodec` | `Double` | `toDoubleOrNull`; falls back to `default` (or `0.0`) |
| `TokenListAttributeCodec` | `List<String>` | space-separated tokens (`theme="primary small"`); deduplicated on write, an empty list removes the attribute |

Anything else is an `AttributeCodec<T>` implementation — three functions, no serialization library:

```kotlin
object InstantCodec : AttributeCodec<Instant> {
    override fun encode(value: Instant): String = value.toString()
    override fun decode(value: String): Instant? = runCatching { Instant.parse(value) }.getOrNull()
    override val absent: Instant = Instant.EPOCH
}
```

`decode` returning `null` is treated exactly like a missing attribute — the delegate falls back to its
`default`, or to `absent`. Decoding never throws, so a typo in the markup cannot break the element.

Enums need a codec because Kotlin/JS cannot enumerate one through reflection. `enumAttributeCodec`
takes the values explicitly and matches case-insensitively against a label (the lower-cased constant
name by default), also accepting the constant name itself when decoding:

```kotlin
enum class ActionKind { Primary, Secondary }

var kind: ActionKind by attr(codec = enumAttributeCodec(ActionKind.entries), default = ActionKind.Primary)
```

## Attributes on someone else's element

`attr` belongs to *your own* component: it registers itself, reports changes and owns a default. A
wrapper element from another repository (`createButton()` → `ButtonElement`) is an `external` class
you cannot add members to, so for those — and for any element you do not want to give a delegate to —
declare an `Attribute` instead and use the three functions that come with it.

```kotlin
object ThemeAttribute : Attribute<List<String>>("theme", TokenListAttributeCodec)
object StepsAttribute : Attribute("steps", IntAttributeCodec)

val button = createButton()
button.writeAttribute(ThemeAttribute, listOf("primary", "small"))   // theme="primary small"
assertEquals(listOf("primary", "small"), button.readAttribute(ThemeAttribute))

button.setAttribute("steps", "12")                                  // written elsewhere
assertEquals(12, button.readAttribute(StepsAttribute))
```

An `Attribute` is only a name plus a codec — **no registration, no observer, no default, no
callback**. Reading goes through `getAttribute` every time, so a value written by another component,
by devtools or by a raw `setAttribute` is visible on the next read; there is no cached copy that can
drift. How to treat "missing" and what to do when it changes is the business of the element's owner,
not of the definition, which is why both are absent here rather than defaulted like `attr`.

There is deliberately **no `by` variant**: a wrapper element is an `external` class, so the only
property you could declare on it is an extension property, and Kotlin does not delegate those. If a
call site reads better as a property, write the two accessors there:

```kotlin
val ButtonElement.labelText: String
    get() = readAttribute(LabelAttribute)
    set(value) = writeAttribute(LabelAttribute, value)
```

### Token lists

`TokenListAttributeCodec` covers the `classList` shape — attributes whose value is a set of
space-separated words (`theme`, `aria-describedby`, `data-tags`). Write the list and it is
normalised; write an empty list and the attribute is removed, so "no tokens" and "absent" are one
state, exactly like an empty `class`.

Mutating one token at a time reads better than handing over a whole list, so `tokens(attribute)`
gives a small view over the attribute:

```kotlin
val tokens = button.tokens(ThemeAttribute)
tokens.add("primary")        // theme="primary"
tokens.toggle("small")       // theme="primary small"
tokens.toggle("small")       // theme="primary"
tokens.remove("primary")     // attribute removed
tokens.clear()               // attribute removed
```

Every call reads and writes the attribute itself, in order: no in-memory set to keep in sync, no
`DOMTokenList` exceptions for odd syntax (a malformed value simply is not a token), and
`tokens.values` / `tokens.raw` show what the browser actually stores.

## `AttributeChange`

The callback receives the change, not just the new value, so a name, a removal or the raw strings are
available without changing the signature later:

| Member | Meaning |
| --- | --- |
| `name` | the attribute name, e.g. `max-count` |
| `oldValue` / `newValue` | decoded values; equal on the first report |
| `oldRaw` / `newRaw` | raw strings; `null` when the attribute is absent |
| `isRemoval` | `true` when the attribute was removed rather than written |

`newRaw == null` is what tells a removal apart from a value that happens to decode to the default.

## `default` is not written to the markup

`default` is what the getter reports while the attribute is absent — a Kotlin-side value, not a
declaration. Removing the attribute goes back to it. Writing it into the markup is opt-in and happens
in one place:

```kotlin
override fun connectedCallback() {
    applyInitialAttributes()                          // defaults stay in Kotlin
    applyInitialAttributes(writeDefaults = true)      // …and are copied into the markup first
}
```

## When the callback runs

- **From the setter:** synchronously.
- **From outside** (`setAttribute`, devtools, another element): on a microtask. The element watches
  itself with a `MutationObserver` instead of declaring `observedAttributes`, because that list is
  read by `customElements.define` from a static getter, before any delegate exists — and a
  `MutationObserver` also sees attributes nobody declared, including on a detached element.
- **On connect:** from `applyInitialAttributes()`. Markup that was already parsed never passes
  through a setter, so this is how the callbacks first hear about it. It is idempotent — a delegate
  that has already reported a value is skipped — so calling it again on a reconnect does not repaint
  anything, and it drops whatever the observer had queued rather than reporting twice.

Only real changes are reported. Writing the same decoded value again is not a change; removing the
attribute is. A callback that writes its own property does not re-enter itself, so an element can
normalize what it was given.

## What a string assigned from JavaScript means

A property is also the element's JS-facing API, and a JS caller only has the value it was given.
React 19 assigns every matching JSX prop to the property, so `<ktjs-action kind="secondary">` arrives
as the **string** `"secondary"` even though the property's type is `ActionKind`.

The delegate therefore treats a string as the attribute's raw form — exactly the text `setAttribute`
would have stored — and only a value of the property's own type goes through the codec. For `String`
the two are the same thing, since that codec is the identity. Skipping this is a subtle bug: a full
page load is fine (React only reconciles during hydration, it does not reset props), while a
client-side navigation to a page whose element React creates throws a `ClassCastException` inside the
codec and replaces the page with an error boundary.

## Only reflecting properties belong here

`attr` writes the attribute, so it is for properties whose value *is* the attribute — what the markup
declares and CSS can select on. `input.value` is not one of those: it is internal state that outlives
the attribute, so it stays an ordinary property.
