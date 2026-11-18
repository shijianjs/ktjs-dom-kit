---
sidebar_label: Demo
---

# Demo

The interactive demo is published at [`/demo/`](pathname:///demo/). It is a single-page site: one
shell with a grouped sidebar, a top bar (logo, title, breadcrumb, and three menus) and a content area
that swaps one page at a time. Routes live in the fragment (`#/elements`), so a deep link works on a
static host and survives a reload.

| Route | What it shows |
| --- | --- |
| `#/overview` | what the library is, its coordinates, where the reference lives |
| `#/elements` | `TagName<T>.createElement { }` — the tag decides the element type |
| `#/attributes` | `attr { }` — a property backed by an HTML attribute, written from Kotlin and from `setAttribute` alike |
| `#/stylesheets` | `cssSheet { }`, adopted by a component's shadow root, with a `:hover` rule that inline styles could not express |
| `#/custom-elements` | `CustomElementAdapter` + `defineIfMissing`, two independent instances of one tag |

Two invariants the page keeps, and the end-to-end suite asserts:

- **nothing of ours is adopted on `document`** — `document.adoptedStyleSheets.length` stays `0`, every
  shadow root adopts only the sheets it needs, and the page body carries no inline `style` attribute
  (the only inline declarations are the Iconify sizes on menu entries, which travel into the menu's
  overlay — a shadow tree our sheets cannot reach);
- **the language switch never rebuilds a page** — it rewrites text in place, so a counter that was
  clicked keeps its value when the language changes.

### Shell

The shell is one `vaadin-app-layout`:

- the site mark, the title (`<h1>`), the version tag, the drawer toggle and the breadcrumbs sit in the
  navbar, the way the official `app-layout` drawer example does it; the grouped `vaadin-side-nav` sits
  in the drawer, one parent item per group and one child item per page;
- the mark and the title are one home link, `<a href="#/">` around them: a reader who wandered into a
  page gets back by clicking the wordmark, and the browser keeps link behaviour (middle-click, copy
  address, keyboard activation). `#/` parses to the router's default route, so the link means "home"
  without the shell knowing which slug that is;
- every navigation entry carries an `iconify-icon` (`lucide:*`), so the demo doubles as a check that
  `ktjs-iconify` and the shell work together;
- the action row is three `vaadin-menu-bar` drop-downs — **Docs** (documentation, API reference,
  repository), **Language** and **Theme** — instead of a row of buttons: a menu is what these are
  semantically, the current value lives inside the menu, and fixed nouns on the buttons mean opening
  or choosing one never resizes the navbar;
- the theme menu has three entries, **Light**, **Dark** and **Follow browser**; the last one sets
  `theme="light-dark"` on the document element, so Lumo resolves `prefers-color-scheme` itself and the
  site repaints when the visitor's system preference changes — there is no `matchMedia` listener in the
  shell;
- the drawer toggle is an icon button: Lumo ships it as a 44px box with `margin: 0 var(--lumo-space-s)`,
  which puts a blank slot between the hamburger and the mark. The shell keeps the component's size (a
  real tap target) but drops its margins, and takes the bar's left inset itself, so the glyph, the
  toggle and the mark read as one cluster;
- an entry that is not on our side of `menu-bar`'s shadow boundary (the submenu content) is sized and
  emphasised inline, because a class declared in the shell's sheet cannot reach it;
- Lumo's `utility.css` is a document-level stylesheet, so its classes cannot reach into a shadow tree.
  The shell still uses the wrapper's class names, but repeats the two declarations it needs
  (`items-center`, `justify-between`) in its own sheet (shell/ShellSheet.kt).

### Languages and the site mark

- A language is a BCP 47 tag, not an enum: `Lang("ja")` registers one, `LanguageState(languages = …)`
  decides which ones the site offers, and every text class declares its copy as
  `texts(Langs.EN to …, Langs.ZH_HANS to …)`. A language without an entry falls back to the first
  declared one, so a newly registered language appears in the menu before its copy is complete, and
  declaration order is menu order.
- The site mark is an `<img>`: `ShellConfig.logoSrc` points at the same logo file the README and the
  documentation site show. The demo build (`scaleLogo`) renders it down to 128px, so the repository
  keeps exactly one logo and the bundle does not ship a 1254px original for a 28px slot.

### Running it

`./gradlew :demo:jsBrowserDevelopmentRun` serves the demo on a local port; `:demo:jsBrowserProductionRun`
serves the production bundle. The `js { }` block sets `target = "es2015"`: without it Kotlin emits
ES5-style classes whose `HTMLElement.call(this)` cannot be constructed, and no custom element ever
upgrades. If a run fails with "Error occurred. See log for details" and nothing else, stop the Gradle
daemon (`./gradlew --stop`) and try again — that failure has been seen once and was a daemon problem,
not a build problem.

The demo is built by `./gradlew pagesSite`, which puts the documentation at `/`, this page at `/demo/`
and the API reference at `/api/`.
