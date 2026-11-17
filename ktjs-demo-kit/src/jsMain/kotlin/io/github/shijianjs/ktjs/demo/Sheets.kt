package io.github.shijianjs.ktjs.demo

import io.github.shijianjs.ktjs.dom.css.cssSheet
import web.cssom.AlignItems
import web.cssom.Border
import web.cssom.BoxSizing
import web.cssom.CSSStyleSheet
import web.cssom.CustomPropertyName
import web.cssom.Display
import web.cssom.FlexDirection
import web.cssom.FlexWrap
import web.cssom.JustifyContent
import web.cssom.Length
import web.cssom.LineStyle
import web.cssom.Margin
import web.cssom.Padding
import web.cssom.TextAlign
import web.cssom.WhiteSpace
import web.cssom.number
import web.cssom.pct
import web.cssom.px
import web.cssom.vh

/**
 * The sheets a demo site shares.
 *
 * [RESET] is adopted by **every** shadow root: `:host { display: block }` (a custom element is inline
 * by default) and `box-sizing`, which is not inherited and therefore has to be redeclared inside each
 * tree — that is the reason a reset is shared at all.
 *
 * [PAGE_SHEET] styles typography and the few pieces a page owns. Boxes and gaps come from Vaadin's
 * `card` / `vertical-layout` / `horizontal-layout` components, which carry their own `padding` /
 * `spacing` theme variants — that is what the component library is for.
 *
 * [SHELL_SHEET] only sizes the app layout and its slots: navbar full width on top, drawer column under
 * it, content scrolling inside the layout. Colours are Lumo custom properties (see [Palette]), so the
 * light/dark switch at the document level repaints every tree without a `[data-theme]` selector.
 */
val RESET: CSSStyleSheet = cssSheet {
    ":host" { display = Display.block }

    "*, *::before, *::after" { boxSizing = BoxSizing.borderBox }
}

/** Typography of one page and the boxes a page owns. */
val PAGE_SHEET: CSSStyleSheet = cssSheet {
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
        margin = Margin(0.px, 0.px, 4.px)
        fontSize = 13.px
    }

    PAGE_LINK {
        marginRight = 16.px
        color = Palette.accent
        fontSize = 13.px
    }

    PAGE_BODY { display = Display.block }

    SECTION_TITLE {
        margin = 0.px
        fontSize = 18.px
        color = Palette.text
    }

    SECTION_HINT {
        margin = 0.px
        color = Palette.muted
        fontSize = 13.px
        lineHeight = number(1.5)
    }

    EXAMPLE_CAPTION {
        margin = Margin(0.px, 0.px, 8.px, 0.px)
        color = Palette.muted
        fontSize = 12.px
    }

    ROW { alignItems = AlignItems.center }

    READOUT {
        margin = 0.px
        color = Palette.muted
        fontSize = 13.px
    }

    // A card is the page's box: it fills the column, the surface itself stays the component's.
    "vaadin-card" { width = 100.pct }

    CODE {
        backgroundColor = Palette.surfaceRaised
        borderRadius = 6.px
        padding = Padding(2.px, 6.px)
        fontSize = 12.px
    }
}

/** The shell: app layout sizing, navbar, drawer column and content column. */
val SHELL_SHEET: CSSStyleSheet = cssSheet {
    ":host" {
        display = Display.block
        height = 100.vh
        backgroundColor = Palette.background
        color = Palette.text
        fontSize = 14.px
        // The app layout's own knobs: a fixed drawer width and the denser Lumo body size.
        set(CustomPropertyName<Length>("--vaadin-app-layout-drawer-width"), 264.px)
        set(CustomPropertyName<Length>("--lumo-font-size-m"), 14.px)
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

    SHELL_TOPBAR {
        width = 100.pct
        justifyContent = JustifyContent.spaceBetween
        flexWrap = FlexWrap.nowrap
        borderBottom = Border(1.px, LineStyle.solid, Palette.border)
    }


    SHELL_LINK {
        color = Palette.accent
        fontSize = 12.px
        whiteSpace = WhiteSpace.nowrap
    }

    NAV_CURRENT { color = Palette.accent }
}
