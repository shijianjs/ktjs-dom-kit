---
sidebar_label: Demo
---

# Demo

The interactive demo is published at [`/demo/`](pathname:///demo/). It is a single-page site: one
shell with a grouped sidebar, a top bar (breadcrumb, API link, language switch, light/dark switch) and
a content area that swaps one page at a time. Routes live in the fragment (`#/elements`), so a deep
link works on a static host and survives a reload.

| Route | What it shows |
| --- | --- |
| `#/overview` | what the library is, its coordinates, where the reference lives |
| `#/elements` | `TagName<T>.createElement { }` — the tag decides the element type |
| `#/attributes` | `attr { }` — a property backed by an HTML attribute, written from Kotlin and from `setAttribute` alike |
| `#/stylesheets` | `cssSheet { }`, adopted by a component's shadow root, with a `:hover` rule that inline styles could not express |
| `#/custom-elements` | `CustomElementAdapter` + `defineIfMissing`, two independent instances of one tag |

Two invariants the page keeps, and the end-to-end suite asserts:

- **nothing of ours is adopted on `document`** — `document.adoptedStyleSheets.length` stays `0`, every
  shadow root adopts only the sheets it needs, and the only `style` attribute in the page is the one on
  the host element;
- **the language switch never rebuilds a page** — it rewrites text in place, so a counter that was
  clicked keeps its value when the language changes.

The demo is built by `./gradlew pagesSite`, which puts the documentation at `/`, this page at `/demo/`
and the API reference at `/api/`.
