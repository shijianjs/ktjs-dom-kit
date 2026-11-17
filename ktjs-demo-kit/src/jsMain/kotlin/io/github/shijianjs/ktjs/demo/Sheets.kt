package io.github.shijianjs.ktjs.demo

import io.github.shijianjs.ktjs.dom.css.colorStop
import io.github.shijianjs.ktjs.dom.css.cssSheet
import io.github.shijianjs.ktjs.dom.css.dsl.supports
import io.github.shijianjs.ktjs.dom.css.dsl.webkitBackgroundClip
import io.github.shijianjs.ktjs.dom.css.dsl.webkitBackgroundClipName
import io.github.shijianjs.ktjs.dom.css.or
import io.github.shijianjs.ktjs.dom.css.supportsDeclaration
import io.github.shijianjs.ktjs.vaadin.lumoutility.LumoUtility
import web.cssom.AlignItems
import web.cssom.Auto
import web.cssom.BackgroundClip
import web.cssom.BoxSizing
import web.cssom.CSSStyleSheet
import web.cssom.Color
import web.cssom.CustomPropertyName
import web.cssom.Display
import web.cssom.JustifyContent
import web.cssom.Length
import web.cssom.Margin
import web.cssom.Padding
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
        // Lumo's radii, one step larger than the defaults (`s 0.25em`, base 0.25em, `l 0.5em`): every
        // card, button, field and overlay inside the shell inherits them, so the whole site rounds
        // together instead of each component being adjusted separately. `m` derives from the base.
        set(CustomPropertyName<Length>("--lumo-border-radius-s"), 0.5.em)
        set(CustomPropertyName<Length>("--lumo-border-radius"), 0.5.em)
        set(CustomPropertyName<Length>("--lumo-border-radius-l"), 1.em)
    }


    // `utility.css` is a document-level sheet of `@vaadin/vaadin-lumo-styles`, so its rules cannot
    // cross into this shadow tree. The classes are still the ones Lumo defines — the selectors below
    // are the wrapper's `ClassName` values — and the declarations have to be repeated here for them
    // to apply inside the shell. Only the handful the shell uses is repeated.
    LumoUtility.AlignItems.CENTER { alignItems = AlignItems.center }

    LumoUtility.JustifyContent.BETWEEN { justifyContent = JustifyContent.spaceBetween }

    // The title and its version tag live in the navbar. Sizes follow the app-layout examples:
    // `font-size: 1.125rem; margin: 0` for the h1, a muted 11px tag beside it.
    SHELL_TITLE {
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
        SHELL_TITLE {
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

    SHELL_VERSION {
        fontSize = 11.px
        color = Palette.muted
    }

    SHELL_TOPBAR {
        width = 100.pct
        // The bar spans the window, but its content must not touch the right edge: the last menu would
        // otherwise sit flush against the scrollbar. The drawer toggle already carries its own inset on
        // the left, so only the end side gets padding here.
        paddingRight = 12.px
    }

    // The left cluster may be narrower than the title plus the full breadcrumb trail; letting it
    // shrink keeps the actions on screen instead of pushing the navbar wider than the viewport.
    SHELL_TOPBAR_START {
        minWidth = 0.px
    }

    // The action row keeps its intrinsic width, so the left cluster is what gives way.
    SHELL_ACTIONS {
        flexShrink = number(0.0)
    }

    // The mark is an image, not an icon: its height is fixed by the navbar and the width follows the
    // file, so a wide logo is not squashed into a square box.
    SHELL_LOGO {
        height = 28.px
        width = Auto.auto
        flexShrink = number(0.0)
    }

    SHELL_NAV_ICON {
        fontSize = 18.px
    }

}
