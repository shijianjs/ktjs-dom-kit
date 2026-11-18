package io.github.shijianjs.ktjs.demo.layout

import io.github.shijianjs.ktjs.demo.style.Palette
import io.github.shijianjs.ktjs.dom.css.cssSheet
import io.github.shijianjs.ktjs.dom.css.dsl.colorStop
import io.github.shijianjs.ktjs.dom.css.dsl.or
import io.github.shijianjs.ktjs.dom.css.dsl.supports
import io.github.shijianjs.ktjs.dom.css.dsl.supportsDeclaration
import io.github.shijianjs.ktjs.dom.css.dsl.webkitBackgroundClip
import io.github.shijianjs.ktjs.dom.css.dsl.webkitBackgroundClipName
import io.github.shijianjs.ktjs.vaadin.lumoutility.LumoUtility
import web.cssom.AlignItems
import web.cssom.Auto
import web.cssom.BackgroundClip
import web.cssom.CSSStyleSheet
import web.cssom.Color
import web.cssom.CustomPropertyName
import web.cssom.Display
import web.cssom.JustifyContent
import web.cssom.Length
import web.cssom.Margin
import web.cssom.None
import web.cssom.PropertyName
import web.cssom.WhiteSpace
import web.cssom.deg
import web.cssom.em
import web.cssom.integer
import web.cssom.linearGradient
import web.cssom.number
import web.cssom.pct
import web.cssom.px
import web.cssom.`var`
import web.cssom.vh

/**
 * The layout: app layout sizing, navbar, drawer column and content column.
 *
 * Colours are Lumo custom properties (see [Palette]), so the light/dark switch at the document level
 * repaints every tree without a `[data-theme]` selector.
 */
val LAYOUT_SHEET: CSSStyleSheet = cssSheet {
    ":host" {
        display = Display.block
        height = 100.vh
        backgroundColor = Palette.background
        color = Palette.text
        fontSize = 14.px
        // The app layout's own knobs: a fixed drawer width and the denser Lumo body size.
        set(CustomPropertyName<Length>("--vaadin-app-layout-drawer-width"), 264.px)
        set(CustomPropertyName<Length>("--lumo-font-size-m"), 14.px)
        // Lumo's radii, scaled up (`s` 0.25em stays the default, `m` 0.5em, `l` 1em): every card,
        // button, field and overlay inside the layout inherits them, so the whole site rounds together
        // instead of each component being adjusted separately.
        //
        // `m` is written out even though Lumo defines it as `var(--lumo-border-radius, 0.25em)`: some
        // components redeclare `--lumo-border-radius-m` on their own `:host`, so inheriting only the
        // base left those corners at the old value. `s` is deliberately kept smaller than `m` — three
        // distinct steps, not two aliases of one value.
        set(CustomPropertyName<Length>("--lumo-border-radius-s"), 0.25.em)
        set(CustomPropertyName<Length>("--lumo-border-radius"), 0.5.em)
        set(CustomPropertyName<Length>("--lumo-border-radius-m"), 0.5.em)
        set(CustomPropertyName<Length>("--lumo-border-radius-l"), 1.em)
    }

    // `utility.css` is a document-level sheet of `@vaadin/vaadin-lumo-styles`, so its rules cannot
    // cross into this shadow tree. The classes are still the ones Lumo defines — the selectors below
    // are the wrapper's `ClassName` values — and the declarations have to be repeated here for them
    // to apply inside the layout. Only the handful the layout uses is repeated.
    LumoUtility.AlignItems.CENTER { alignItems = AlignItems.center }

    LumoUtility.JustifyContent.BETWEEN { justifyContent = JustifyContent.spaceBetween }

    // The title and its version tag live in the navbar. Sizes follow the app-layout examples:
    // `font-size: 1.125rem; margin: 0` for the h1, a muted 11px tag beside it.
    //
    // The link keeps the browser's link behaviour but none of its look: the `h1` inside carries the
    // wordmark (colour, weight, gradient), so the anchor only removes the underline and the visited
    // colour. `display: flex` makes the mark and the `h1` one flex row, so the anchor stays the same
    // height as the plain `h1` did and the navbar's centre line does not move.
    LAYOUT_TITLE_LINK {
        display = Display.flex
        alignItems = AlignItems.center
        // The mark and the wordmark are one lock-up, but not one glyph pair: a small gap keeps them
        // readable as two elements of the same brand instead of a logo glued to the first letter.
        columnGap = 8.px
        textDecorationLine = None.none
        color = Palette.text
        flexShrink = number(0.0)
    }

    LAYOUT_TITLE {
        margin = Margin(0.px, 0.px)
        fontSize = 18.px
        color = Palette.text
        // One line: the navbar has room for the actions only if the title stops shrinking.
        whiteSpace = WhiteSpace.nowrap
        flexShrink = number(0.0)
        // The wordmark reads like vuejs.org's: painted with a gradient of the theme's primary colour
        // into teal instead of a solid fill. The gradient itself carries the "slant" (its axis is
        // angled); the glyphs stay upright, so no italic.
        fontWeight = integer(700)
    }

    // `background-clip: text` is the only way to paint text with a gradient, and it needs the
    // `-webkit-` spelling to be portable; without support the rule above keeps the solid colour, so
    // the title is never invisible.
    supports(
        supportsDeclaration(webkitBackgroundClipName, BackgroundClip.text) or
            supportsDeclaration(PropertyName.backgroundClip, BackgroundClip.text),
    ) {
        LAYOUT_TITLE {
            backgroundImage = linearGradient(
                100.deg,
                colorStop(`var`(CustomPropertyName<Color>("--lumo-primary-color"))),
                colorStop(Color("#2ec4b6")),
            )
            webkitBackgroundClip(BackgroundClip.text)
            backgroundClip = BackgroundClip.text
            color = Color.transparent
            // The clip only paints inside the padding box, so a descender ("j") would be cut off.
            paddingBottom = 0.12.em
        }
    }

    LAYOUT_VERSION {
        fontSize = 11.px
        color = Palette.muted
    }

    LAYOUT_TOPBAR {
        width = 100.pct
        // The bar spans the window, but its content must not touch the edges: the last menu would sit
        // flush against the scrollbar, and the drawer toggle flush against the window frame.
        paddingLeft = 8.px
        paddingRight = 12.px
    }

    // The toggle is a 36px icon-button box around a 21px glyph: Lumo's own 9px padding and 8px margin
    // around it read as a blank gap between the toggle and the wordmark, on top of the layout's gap.
    // Both are dropped here; the inset comes from [LAYOUT_TOPBAR], the separation from the layout gap.
    LAYOUT_TOGGLE {
        marginInlineStart = 0.px
        marginInlineEnd = 0.px
        paddingInlineStart = 0.px
        paddingInlineEnd = 0.px
    }

    // The left cluster may be narrower than the title plus the full breadcrumb trail; letting it
    // shrink keeps the actions on screen instead of pushing the navbar wider than the viewport.
    LAYOUT_TOPBAR_START {
        minWidth = 0.px
    }

    // The action row keeps its intrinsic width, so the left cluster is what gives way.
    LAYOUT_ACTIONS {
        flexShrink = number(0.0)
    }

    // The mark is an image, not an icon: its height is fixed by the navbar and the width follows the
    // file, so a wide logo is not squashed into a square box.
    LAYOUT_LOGO {
        height = 28.px
        width = Auto.auto
        flexShrink = number(0.0)
    }

    LAYOUT_NAV_ICON {
        fontSize = 18.px
    }
}
