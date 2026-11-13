---
sidebar_position: 1
---

# DOM kit

`ktjs-dom-kit` is a published library: helpers for writing components in Kotlin/JS against the native
DOM, with no knowledge of any documentation framework and no component runtime behind them.

It is plumbing for UI code written without a UI framework — a base class a custom element can extend,
an attribute delegate that makes the DOM the single source of truth, a typed view of an element's
inline style, a way to build a `CSSStyleSheet` out of Kotlin values instead of CSS text, and a typed
face for the `loadjs` loader.

## Where it is used

The [ktjs-docusaurus](https://github.com/shijianjs/ktjs-docusaurus) site is built on it: every
`<ktjs-…>` element on that landing page extends `CustomElementAdapter` and styles itself with
`cssSheet { … }`. That is a consumer, not a parent — this repository publishes the artifact, and the
site depends on it the same way any other project would.

## Why its own repository

Nothing here wraps an npm package, so the version is a hand-written semantic one (`0.1.0`) rather than
the `{npm version}-kt.{revision}` scheme the npm-backed libraries in this repository follow.

The repository is deliberately not one-library-per-project. A wrapper whose API is a handful of
declarations does not repay the cost of its own repository, CI, release pipeline and Pages site, so
small libraries land here as additional Gradle modules and get a directory in this site. The build
picks up every `ktjs-*` module automatically: publishing, Dokka and the version report need no edit.

## Coordinates

```kotlin
implementation("io.github.shijianjs:ktjs-dom-kit:0.1.0")
```

The artifact is published to Maven Central and its API reference is under `/api/` on this site.

## What is in it

The module is split by concern: a package per job, a subpackage wherever a package has an obvious
parent–child or one-to-many relation (a type and the implementations of it, a sheet and the sugar
written inside it), and a file per declaration — nothing here is a catch-all.

| Package | Provides |
| --- | --- |
| `element/` | the generic element helpers: `TagName<T>.createElement { … }`, and `HTMLElement.inlineStyle`, the element's own style seen as `csstype.Properties` |
| `webcomponent/` | what a custom element is built from: `CustomElementAdapter` (the lifecycle hooks under their native names, the `attr` delegate host), `defineIfMissing`, [`stripBridgesFrom`](./custom-elements.md), and `attachShadowRoot` / `attachStyledShadow` |
| `attribute/` | the `attr` property delegate: `attr`, `AttributeChange`, the provider and the delegate behind it, and the property-name rule; `attribute/types/` holds the codecs — one per type the delegate knows, plus `enumAttributeCodec` |
| `css/` | `cssSheet { … }` / `cssText { … }`, and the typed values cssom does not model (`RadialGradientShape`, `radialGradient`, `pointerEventsNone`, `SupportsQuery`, …); `css/dsl/` holds the sugar written inside a sheet — `Pseudos` / `pseudo` / `pseudos`, the `supports { … }` at-rule and `webkitBackgroundClip` |
| `loadjs/` | a wrapper for the [`loadjs`](https://github.com/kubetail-org/loadjs) loader: its four call shapes as overloads, the path prefixes (`css!`, `img!`, `module!`, `nomodule!`), and the bundle registry (`ready` / `done` / `reset` / `isDefined`) |

## What it does not do

- **No component model.** There is no state, no `render()`, no diffing and no UI runtime. A component
  builds its subtree once and mutates the nodes it owns from then on — see
  [home sections](https://shijianjs.github.io/ktjs-docusaurus/docs/kit/home-sections/) in the
  ktjs-docusaurus site for what that looks like in practice.
- **No HTML strings.** Nodes are created as nodes (`document.createElement` through a typed
  `TagName<T>`), never assembled from markup and re-parsed.
- **No `<style>` nodes.** A component's own style is either inline declarations on the node
  (`inlineStyle`) or a `CSSStyleSheet` adopted by a shadow root; a sheet belongs to the class, not to
  the instance.

## The pages

- [Custom elements](./custom-elements.md) — the adapter, the lifecycle hooks, and defining a tag.
- [Attributes](./attributes.md) — the `attr` delegate and its codecs.
- [Styling](./styling.md) — inline styles, shadow roots and adopted sheets.
- [Typed stylesheets](./css-sheet.md) — `cssSheet { … }`: the serializer, the typed values, the
  traps, and what to do when cssom has no type for something.
