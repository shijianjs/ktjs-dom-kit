package io.github.shijianjs.ktjs.demo.page

import io.github.shijianjs.ktjs.demo.style.Palette
import io.github.shijianjs.ktjs.dom.css.cssSheet
import web.cssom.AlignItems
import web.cssom.CSSStyleSheet
import web.cssom.Display
import web.cssom.FlexDirection
import web.cssom.FontFamily
import web.cssom.Margin
import web.cssom.Padding
import web.cssom.WhiteSpace
import web.cssom.number
import web.cssom.pct
import web.cssom.px
import web.cssom.rem

/**
 * Typography of one page and the boxes a page owns.
 *
 * Boxes and gaps come from Vaadin's `card` / `vertical-layout` / `horizontal-layout` components, which
 * carry their own `padding` / `spacing` theme variants — that is what the component library is for.
 */
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

    // A section is an element (`<ktjs-demo-section>`), and an unknown element is inline by default;
    // its heading, hint and body are block-level, so the host has to say so.
    SECTION {
        display = Display.block
        width = 100.pct
    }

    SECTION_TITLE {
        margin = Margin(0.px, 0.px, 6.px)
        fontSize = 18.px
        color = Palette.text
    }

    SECTION_HINT {
        margin = Margin(0.px, 0.px, 10.px)
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

    // The gap is a function-free length per grid; the block sets it inline (style.setProperty).
    GRID {
        display = Display.grid
        width = 100.pct
    }

    READOUT {
        margin = 0.px
        color = Palette.muted
        fontSize = 13.px
    }

    EVENT_LOG {
        display = Display.flex
        flexDirection = FlexDirection.column
        margin = 0.px
        padding = Padding(8.px, 10.px)
        maxHeight = 9.rem
        backgroundColor = Palette.surfaceRaised
        borderRadius = 6.px
        fontFamily = FontFamily.monospace
        fontSize = 12.px
        color = Palette.muted
    }

    EVENT_LINE {
        margin = 0.px
        whiteSpace = WhiteSpace.preWrap
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
