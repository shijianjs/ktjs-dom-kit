package io.github.shijianjs.ktjs.demo

import io.github.shijianjs.ktjs.dom.css.cssSheet
import js.reflect.unsafeCast
import web.cssom.AlignItems
import web.cssom.Border
import web.cssom.BoxSizing
import web.cssom.CSSStyleSheet
import web.cssom.CustomPropertyName
import web.cssom.Length
import web.cssom.Display
import web.cssom.FlexDirection
import web.cssom.FlexWrap
import web.cssom.GridTemplateTracks
import web.cssom.JustifyContent
import web.cssom.LineStyle
import web.cssom.Margin
import web.cssom.Overflow
import web.cssom.Padding
import web.cssom.Position
import web.cssom.TextAlign
import web.cssom.TextOverflow
import web.cssom.WhiteSpace
import web.cssom.number
import web.cssom.px
import web.cssom.vh

/**
 * The sheets a demo site shares.
 *
 * [RESET] is adopted by **every** shadow root: `:host { display: block }` (a custom element is
 * inline by default) and `box-sizing`, which is not inherited and therefore has to be redeclared
 * inside each tree — that is the reason a reset is shared at all.
 *
 * [PAGE_SHEET] is adopted by the page element only: it styles the page frame and the boxes a page is
 * built from. [SHELL_SHEET] is adopted by the shell only.
 *
 * Colours are Lumo custom properties (see [Palette]), so the light/dark switch at the document level
 * repaints every tree without a `[data-theme]` selector inside any component.
 *
 * None of them is ever adopted on `document`: the host page is not painted by this site's sheets.
 */
val RESET: CSSStyleSheet = cssSheet {
    ":host" { display = Display.block }

    "*, *::before, *::after" { boxSizing = BoxSizing.borderBox }
}

/** Typography and chrome of one page: its frame and the boxes examples sit in. */
val PAGE_SHEET: CSSStyleSheet = cssSheet {
    ":host" {
        display = Display.block
        padding = Padding(0.px, 0.px, 40.px, 0.px)
    }

    PAGE_TITLE {
        margin = Margin(0.px, 0.px, 6.px)
        fontSize = 26.px
        color = Palette.text
    }

    PAGE_SUMMARY {
        margin = Margin(0.px, 0.px, 10.px)
        color = Palette.muted
        fontSize = 14.px
        lineHeight = number(1.55)
    }

    PAGE_LINKS {
        margin = Margin(0.px, 0.px, 22.px)
        fontSize = 13.px
    }

    PAGE_LINK {
        marginRight = 16.px
        color = Palette.accent
        fontSize = 13.px
    }

    PAGE_BODY { display = Display.block }

    SECTION { marginTop = 28.px }

    SECTION_TITLE {
        margin = Margin(0.px, 0.px, 4.px)
        fontSize = 18.px
        color = Palette.text
    }

    SECTION_HINT {
        margin = Margin(0.px, 0.px, 14.px)
        color = Palette.muted
        fontSize = 13.px
        lineHeight = number(1.5)
    }

    SECTION_BODY { display = Display.block }

    EXAMPLE {
        display = Display.block
        backgroundColor = Palette.surface
        borderRadius = 10.px
        padding = Padding(16.px, 16.px, 16.px, 16.px)
        marginBottom = 12.px
    }

    EXAMPLE_CAPTION {
        margin = Margin(0.px, 0.px, 10.px)
        color = Palette.muted
        fontSize = 12.px
    }

    ROW {
        display = Display.flex
        columnGap = 10.px
        rowGap = 10.px
        flexWrap = FlexWrap.wrap
        alignItems = AlignItems.center
    }

    COL {
        display = Display.flex
        flexDirection = FlexDirection.column
        rowGap = 10.px
    }

    GRID {
        display = Display.grid
        columnGap = 12.px
        rowGap = 12.px
    }

    READOUT {
        margin = Margin(8.px, 0.px, 0.px)
        color = Palette.muted
        fontSize = 13.px
    }

    CODE {
        backgroundColor = Palette.surfaceRaised
        borderRadius = 6.px
        padding = Padding(2.px, 6.px, 2.px, 6.px)
        fontSize = 12.px
    }
}

/**
 * The shell's two columns: a fixed sidebar and the content column that takes the rest.
 *
 * Written as text because cssom types `grid-template-columns` as `GridTemplateTracks`, which has no
 * constructor for a fixed-px + `minmax()` pair — the same escape hatch `autoFitGrid` in
 * ktjs-docusaurus-kit-browser uses.
 */
private val SHELL_COLUMNS: GridTemplateTracks = unsafeCast("256px minmax(0, 1fr)")

/** The shell: a sidebar column and the content column beside it. */
val SHELL_SHEET: CSSStyleSheet = cssSheet {
    ":host" {
        display = Display.grid
        gridTemplateColumns = SHELL_COLUMNS
        minHeight = 100.vh
        backgroundColor = Palette.background
        color = Palette.text
        fontSize = 14.px
        // Lumo's body size drives the components inside the sidebar; the site is denser than Lumo's default.
        set(CustomPropertyName<Length>("--lumo-font-size-m"), 14.px)
    }

    SHELL_NAV {
        display = Display.flex
        flexDirection = FlexDirection.column
        rowGap = 18.px
        padding = Padding(20.px, 16.px)
        backgroundColor = Palette.surface
        borderRight = Border(1.px, LineStyle.solid, Palette.border)
        position = Position.sticky
        top = 0.px
        height = 100.vh
        overflow = Overflow.scroll
    }

    SHELL_BRAND {
        display = Display.flex
        flexDirection = FlexDirection.column
        rowGap = 2.px
    }

    SHELL_BRAND_NAME {
        fontSize = 18.px
        color = Palette.text
    }

    SHELL_BRAND_VERSION {
        fontSize = 11.px
        color = Palette.muted
    }

    SHELL_GROUP {
        display = Display.flex
        flexDirection = FlexDirection.column
        rowGap = 2.px
    }

    SHELL_GROUP_TITLE {
        padding = Padding(2.px, 0.px)
        fontSize = 11.px
        color = Palette.muted
        textAlign = TextAlign.left
    }

    SHELL_MAIN {
        display = Display.flex
        flexDirection = FlexDirection.column
        minWidth = 0.px
    }

    SHELL_TOPBAR {
        display = Display.flex
        alignItems = AlignItems.center
        justifyContent = JustifyContent.spaceBetween
        columnGap = 16.px
        padding = Padding(12.px, 24.px)
        backgroundColor = Palette.surface
        borderBottom = Border(1.px, LineStyle.solid, Palette.border)
        position = Position.sticky
        top = 0.px
    }

    SHELL_BREADCRUMB {
        color = Palette.muted
        fontSize = 12.px
        overflow = Overflow.hidden
        textOverflow = TextOverflow.ellipsis
        whiteSpace = WhiteSpace.nowrap
        minWidth = 0.px
    }

    SHELL_ACTIONS {
        display = Display.flex
        alignItems = AlignItems.center
        columnGap = 8.px
        flexWrap = FlexWrap.nowrap
    }

    SHELL_CONTENT {
        padding = Padding(24.px, 32.px, 48.px, 32.px)
        minWidth = 0.px
    }

    SHELL_LINK {
        marginRight = 12.px
        color = Palette.accent
        fontSize = 12.px
    }

    NAV_CURRENT { color = Palette.accent }
}
