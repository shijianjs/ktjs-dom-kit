---
sidebar_position: 1
---

# Demo kit

`ktjs-demo-kit` is the frame a **live demo site** is built in — the site that documents a library and
doubles as its integration test. It is not a UI framework and it is not a page template: it is the
layout (app layout, drawer, navbar, menus), the page catalogue behind those menus, the language and
theme state, and the blocks pages are assembled from.

The demo of `ktjs-dom-kit` is an instance of it: the very pages you are reading the code examples from
were built as `Entry`s in a `DemoMenu` and mounted into one `DemoLayout`.

## Coordinates

```kotlin
implementation("io.github.shijianjs:ktjs-demo-kit:0.1.0")
```

It brings `ktjs-dom-kit`, `ktjs-vaadin-web-components` and `ktjs-iconify-icon` as `api` dependencies
(the layout is built out of Vaadin components and Iconify icons, and the page contract is spelled in
`HTMLElement`), so a demo project adds this one line and writes pages.

## What it gives you

```mermaid
flowchart TB
  L["vaadin-app-layout<br/>(navbar spans the full width)"] --> N["navbar<br/>mark · title · version · breadcrumb · menus"]
  L --> D["drawer<br/>vaadin-side-nav, icon per entry"]
  L --> B["content slot"]
  B --> P["the mounted page<br/>(DemoPage of the current route)"]
  N --> M["menu bars: docs · language · appearance"]
  M --> T["Theme / LanguageState<br/>written straight to &lt;html&gt;"]
```

| Package | Provides |
| --- | --- |
| `…ktjs.demo.layout` | [`DemoLayout`](./pages.md) and `LayoutConfig` — the site frame and its one config object; `LAYOUT_SHEET`, the class names the frame and its sheet agree on |
| `…ktjs.demo.nav` | `DemoMenu`, `Group`, `Entry`, `Page` and the hash `Router` behind them |
| `…ktjs.demo.i18n` | `Lang` (a BCP 47 tag, registrable), `LanguageState`, `Translations<T>` / `texts(…)` and `SiteTexts` |
| `…ktjs.demo.page` | [`DemoPage`](./pages.md) and the blocks: `paragraph`, `heading`, `anchor`, `codeText`, `bulletList`, `row`, `col`, `exampleBox`, `sectionBlock`, `Readout` |
| `…ktjs.demo.style` | `Theme` / `ThemeMode` (light / dark / follow-the-browser), `Palette` (the Lumo custom properties the site paints with) and `RESET`, the shared reset sheet |

## The shell is three cards, not three regions

The frame does not split the window with lines. The window is a neutral canvas (`Palette.page`) and each
of the three regions is a **card** floating on it — a white, rounded (16px) surface with a soft shadow and
no border, one gutter (8px) in from the window edge and one gap (8px) from its neighbours.

```mermaid
flowchart TB
  H["ktjs-demo-layout<br/>the canvas"] --> A["vaadin-app-layout"]
  A --> NP["::part(navbar)<br/>padding 6 / 8, 56px band"]
  NP --> NC["layout__topbar<br/>= the navbar card (44)"]
  A --> DP["::part(drawer)<br/>padding = the gutter, overflow: clip"]
  DP --> DC["vaadin-side-nav<br/>= the drawer card"]
  A --> CP["::part(content)<br/>padding = gutter + gap, overflow: clip"]
  CP --> CC["the content card"] --> P["the page<br/>= what the card scrolls"]
```

Geometry is still the app layout's: the navbar is fixed, its measured height is the content's top
offset, and the drawer's width is the content's inset from the left. Only the *surface* moved. Four
rules hold that shape, and each is there because the obvious alternative breaks:

| Rule | Why not the obvious alternative |
| --- | --- |
| The gutter and the gap are padding on the app layout's `::part()`s | A margin on the fixed navbar is outside the box the component measures, so the content below would start **under** the bar. A margin on the only child of a scrolling column adds to its scrollable area — a permanent scrollbar of exactly that height |
| The navbar's `::part()` has no `min-height`, and its row is the controls' own 44px | The base style's 56px minimum is *inside* the bar, so with a gutter of our own the band would be 56 + 16. The gutter is part of the 56 instead: no minimum height, and the band is 6 + 44 + 6. The row cannot be 40 — a menu-bar button is 40px in its own box but 44 with its margins, and a shorter container clips it |
| Each card is the scroll viewport; each column is `overflow: clip` | A card that grows with its page scrolls its own top edge — and its rounded corners — out of view. And a column left at `auto` paints a second bar the moment its child overflows (a drawer list longer than the window): that bar is the platform's, square and opaque, three pixels to the right of the card's own |
| The bar and the columns are transparent, and their hairline borders are cleared | The Lumo theme for the app layout paints them and sets safe-area padding from inside the component's own tree. Its `--vaadin-app-layout-navbar-*` custom properties would be the natural knob, but the theme overrides them — a `::part()` rule is the only thing that outranks it |

Those `::part()` rules are also why the sheet builds its selectors as
`compound(tag(APP_LAYOUT), pseudo(pseudoElement("part"), ident("navbar")))` instead of a string: the part
name is a value with a type, and a selector that stops matching fails the build rather than the layout.

Both cards that scroll get a thin scrollbar of their own (`::-webkit-scrollbar`: a 10px strip whose track
paints nothing and whose thumb is a 4px pill), so the platform's classic bar — opaque, square, arrowed —
never lands on a rounded corner. The track is inset by a card radius at both ends, which is a margin on
the scrollbar itself in Blink: even at either scroll limit the pill stops inside the corner arcs.

## Wiring a site

A demo's `main` is the same three steps every time: create the state, define the tag, mount the config.

```kotlin
fun main() {
    val language = LanguageState()          // starts with English + Simplified Chinese
    language.init()                         // applies the stored language to <html lang>
    val theme = Theme()                     // Light / Dark / System, persisted
    theme.applyStored()

    // The layout registers itself; reading its tag name is what upgrades <ktjs-demo-layout>.
    val layout = document.getElementById(ElementId("demo")) as? DemoLayout ?: error("no <ktjs-demo-layout>")

    layout.mount(
        LayoutConfig(
            menu = siteMenu(),
            language = language,
            theme = theme,
            texts = siteTexts,
            siteVersion = KIT_VERSION,
            docsUrl = DOCS_URL,
            apiUrl = API_URL,
            sourceUrl = SOURCE_URL,
            logoSrc = "img/ktjs-dom-kit-logo.png",
        ),
    )
}
```

`LayoutConfig` is one object on purpose. A `mount(menu, language, theme, config, texts)` signature
grows a parameter per knob; one config means the layout can gain a sixth concern (`siteVersion`, a
feedback link, a base path) without every demo project editing its call site. Everything after
`siteVersion` is optional — a site with only a repository gets a link menu with one entry, and a site
with no URLs hides the menu entirely.

The element itself must come from the page (`index.html` declares `<ktjs-demo-layout id="demo">`); a
second layout built in `main` would leave the declared one empty and the site with two drawers.

## Languages

The kit ships two languages and treats a language as **data**, not an enum branch:

```kotlin
val language = LanguageState(languages = listOf(Langs.EN, Langs.ZH_HANS, Lang("ja")))

private val siteTexts = texts(
    Langs.EN to SiteTexts(siteTitle = "ktjs-dom-kit", …),
    Langs.ZH_HANS to SiteTexts(siteTitle = "ktjs-dom-kit", …),
    Lang("ja") to SiteTexts(siteTitle = "ktjs-dom-kit", …),   // registered by the application
)
```

- `Lang` is an external interface over a tag; `Langs` holds the two built-ins and `Lang("ja")` registers
any other tag the site has copy for.
- `LanguageState` keeps the list, the current value, the `localStorage` persistence and
  `document.documentElement.lang`; the language menu is built from the list, so a tag the site does not
  register simply never appears.
- `Translations<T>` is one text object per language. It falls back to the **first declared** language,
  which is why a newly registered language shows sensible copy before its own translation lands. The
  declaration order is the menu order.
- Switching is a walk over the mounted pages (`Page.applyTranslation`), never a rebuild: scroll position
  and whatever the user typed survive.

## DemoMenu and routing

```kotlin
class Entry(val id: String, val group: Group, val icon: String?, val create: () -> Page)
```

Groups are derived from the entries, so adding a page is one entry and nothing else. `id` is the route
(`#/stylesheets`) and the key `SiteTexts.pageTitles` is looked up by — an id missing from the map shows
the id itself, so a page appears in the navigation before its translations land.

```mermaid
sequenceDiagram
  autonumber
  participant U as user
  participant R as Router
  participant C as DemoMenu
  participant P as Page (cached)
  U->>R: click an entry, or open #/stylesheets
  R->>C: Entry(id) for the route
  alt first visit
    C->>P: create()
    P->>P: applyTranslation(current language)
  else seen before
    C-->>P: the same instance
  end
  R->>U: the page is the content slot
```

Pages are created lazily on first visit and then cached, so a page's own state (typed input, opened
panels) survives navigating away and back.
