---
sidebar_position: 4
---

# Styling

A component's style lives on the component, in one of three places — and the choice between them is
not a matter of taste:

| Where | What goes there | How |
| --- | --- | --- |
| the nodes themselves | everything a plain declaration can say | `node.inlineStyle.apply { display = Display.flex; padding = Padding(1.5.rem) }` |
| a sheet the component class owns | what inline styles cannot: `:hover`, `:focus-visible`, `::before`, `@media`, `@supports`, `@keyframes` | one `CSSStyleSheet` in the class's `companion object`, adopted by the shadow root |
| a sheet several components share | a reset, the theme tokens, a layout a parent imposes on its children | the same mechanism, one sheet adopted by more than one element class |

## Inline styles, typed

`HTMLElement.style` comes from lib.dom, where every property is a plain `String`. `inlineStyle` is the
very same `CSSStyleDeclaration` seen as kotlin-csstype's `Properties`, so the element's own box,
colours and typography are written with real types instead of hand-built strings:

```kotlin
card.inlineStyle.apply {
    display = Display.flex
    padding = Padding(1.5.rem)
    borderRadius = 1.rem
    color = Color("var(--ktjs-accent, #1f6feb)")   // the last resort: a string the types cannot express
}
```

Values come from `web.cssom` / csstype: `Display.flex`, `1.5.rem`, `Padding(top, horizontal, bottom)`,
`BoxShadow(…)`, `rgb(…)`, `NamedColor.white` (a named colour **is** a `Color`, so `"#fff"` is not
needed), `var(hook, fallback)`, `clamp(…)`. This is a cast to a richer interface over the same
object — the style is still ordinary inline style on the element.

## Shadow roots

```kotlin
fun HTMLElement.attachShadowRoot(mode: ShadowRootMode = ShadowRootMode.open): ShadowRoot

fun HTMLElement.attachStyledShadow(
    vararg styles: CSSStyleSheet,
    mode: ShadowRootMode = ShadowRootMode.open,
): ShadowRoot
```

`attachShadowRoot` attaches the root; `attachStyledShadow` does the same and adopts the sheets it is
given in one step, which is what a component with any CSS rule wants:

```kotlin
class Card : CustomElementAdapter() {
    private val shadow = attachStyledShadow(HOME_RESET, SHEET)

    companion object {
        private val cardClass = ClassName("card")

        private val SHEET: CSSStyleSheet = cssSheet {
            cardClass { Pseudos.hover { borderColor = `var`(accentHook, `var`(primaryColorHook)) } }
        }
    }
}
```

**One sheet per class, in the class's `companion object`.** The rules are static, so they are parsed
once and one `CSSStyleSheet` object serves every instance — Lit's `static styles`, in the place
Kotlin keeps a class's statics. Never create a `<style>` node per instance: a page with four hundred
cells would carry four hundred copies of the same text and pay the parse cost four hundred times.
Adoption is a sharing mechanism, and it works at both levels in the table above.

## Why shadow by default

A shadow root keeps the page stylesheet out and the component's rules in. Theme changes cross the
boundary the other way, through inherited custom properties, which are the one kind of value that
does inherit:

```css
:root {
  --ktjs-accent: #1f6feb;
  --ktjs-card-bg: var(--ifm-background-surface-color);
}
```

Two rules follow from the boundary:

- **Declare the properties outside**, on `:root` / `[data-theme]`. A component must not write its own
  `[data-theme]` selector or depend on a host class: nothing of that crosses the boundary.
- **Only inherited properties cross it.** `box-sizing` is not inherited, so a shadow tree has to
  re-declare its own reset; `:host { display: block }` belongs in a sheet for the same reason a
  `display` assignment does not belong in a constructor (writing an attribute there throws).

Fall back to light DOM only when the component genuinely has to reuse the host's CSS rules, and say
so in review — the prefix and the custom properties are what keep light-DOM styling out of the way.

## Building the sheet

The rules themselves are a typed object, not CSS text. That is the subject of
[Typed stylesheets](./css-sheet.md).
