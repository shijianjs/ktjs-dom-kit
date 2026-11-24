package io.github.shijianjs.ktjs.demo.layout

import io.github.shijianjs.ktjs.demo.style.Palette
import io.github.shijianjs.ktjs.dom.css.cssSheet
import io.github.shijianjs.ktjs.dom.css.dsl.CssSelector
import io.github.shijianjs.ktjs.dom.css.dsl.colorStop
import io.github.shijianjs.ktjs.dom.css.dsl.compound
import io.github.shijianjs.ktjs.dom.css.dsl.invoke
import io.github.shijianjs.ktjs.dom.css.dsl.or
import io.github.shijianjs.ktjs.dom.css.dsl.pseudo
import io.github.shijianjs.ktjs.dom.css.dsl.pseudoElement
import io.github.shijianjs.ktjs.dom.css.dsl.supports
import io.github.shijianjs.ktjs.dom.css.dsl.supportsDeclaration
import io.github.shijianjs.ktjs.dom.css.dsl.tag
import io.github.shijianjs.ktjs.dom.css.dsl.webkitBackgroundClip
import io.github.shijianjs.ktjs.dom.css.dsl.webkitBackgroundClipName
import io.github.shijianjs.ktjs.vaadin.lumoutility.LumoUtility
import web.cssom.AlignItems
import web.cssom.Auto
import web.cssom.BackgroundClip
import web.cssom.Border
import web.cssom.BoxShadow
import web.cssom.CSSStyleSheet
import web.cssom.Color
import web.cssom.CustomPropertyName
import web.cssom.Display
import web.cssom.JustifyContent
import web.cssom.Length
import web.cssom.LineStyle
import web.cssom.Margin
import web.cssom.None
import web.cssom.Padding
import web.cssom.PropertyName
import web.cssom.WhiteSpace
import web.cssom.deg
import web.cssom.em
import web.cssom.ident
import web.cssom.integer
import web.cssom.linearGradient
import web.cssom.number
import web.cssom.pct
import web.cssom.px
import web.cssom.rgb
import web.cssom.unaryMinus
import web.cssom.`var`
import web.cssom.vh
import web.dom.TagName
import web.html.HTMLElement

/**
 * The space between the window edge and a card.
 *
 * One value on all four sides: a card that is 16px from one edge and 12px from another reads as a
 * mistake rather than as a layout, and the fact that two of these insets belong to a fixed bar and two
 * to a scrolling column is the shell's business, not the reader's.
 */
private val GUTTER: Length = 16.px

/** The space between two cards. Smaller than a gutter, so the three surfaces still read as one group. */
private val GAP: Length = 12.px

/**
 * A card's corner radius — bigger than any control's, because a card is a surface and not a control.
 *
 * Deliberately not `--lumo-border-radius-l`: that token also rounds dialogs, fields and buttons, and
 * 20px on a 32px control turns it into a pill.
 */
private val CARD_RADIUS: Length = 20.px

/**
 * A card's edge: a hairline, so the surface keeps a boundary where the shadow has faded — on the light
 * canvas the shadow alone is nearly invisible at the card's top edge, and in the dark theme the two
 * colours are only one step apart.
 */
private val CARD_EDGE: Color = Color("light-dark(rgba(23, 32, 44, .08), rgba(255, 255, 255, .07))")

/**
 * A card's shadow colour: Lumo's 20 % shade, which is a *translucent* dark in both themes, so one value
 * works for both without a `light-dark()` pair of its own.
 */
private val CARD_SHADOW: Color = `var`(CustomPropertyName<Color>("--lumo-shade-20pct"), rgb(226, 230, 236))

/**
 * `vaadin-app-layout` as a selector target, for the three `::part()` rules below.
 *
 * The wrapper's tag catalog is not reachable from this module's API surface (it exposes element
 * *classes*, not tag names), and the only thing needed here is a type selector: no element is created
 * through it. The parts are the component's public styling contract, so the rules live in this sheet
 * instead of a `Flow`-style theme override.
 */
private val APP_LAYOUT: TagName<HTMLElement> = TagName("vaadin-app-layout")

/** `vaadin-app-layout::part(navbar)` — the fixed bar, seen from outside the component. */
private val NAVBAR_PART: CssSelector =
    compound(tag(APP_LAYOUT), pseudo(pseudoElement("part"), ident("navbar")))

/** `vaadin-app-layout::part(drawer)` — the drawer column, seen from outside the component. */
private val DRAWER_PART: CssSelector =
    compound(tag(APP_LAYOUT), pseudo(pseudoElement("part"), ident("drawer")))

/** `vaadin-app-layout::part(content)` — the content column, seen from outside the component. */
private val CONTENT_PART: CssSelector =
    compound(tag(APP_LAYOUT), pseudo(pseudoElement("part"), ident("content")))

/**
 * The shell: three cards on the page's canvas.
 *
 * A classic app layout splits the window with lines — a bar on top, a drawer column, a content column,
 * all sharing one background. This shell does the opposite, the way current systems draw an app (iOS /
 * One UI's grouped cards, Vaadin's own settings demo): the window is a neutral canvas, and each of the
 * three regions is a *surface* on it — a white rounded card, [GUTTER] away from the window edge and
 * [GAP] away from its neighbours. That also fixes what the bar got wrong: its background used to be the
 * same `--vaadin-background-container` tint that the buttons and menus inside it paint themselves with,
 * so a control had no contrast against the surface it sat on. A control has to contrast with the card,
 * not with the window.
 *
 * The geometry stays `vaadin-app-layout`'s: the navbar is fixed, its measured height is the content's
 * top offset, and the drawer's width is the content's inset from the left. What moves is where the
 * *surface* is painted:
 *
 * - the navbar bar keeps no background of its own. Its padding is the gutter, so the card is the row
 *   we own ([LAYOUT_TOPBAR]), inset by the bar. A margin on the fixed bar would be outside the box the
 *   component measures, and the content below would start under it.
 * - the drawer and content columns are `::part()`s of the app layout. Their padding is the gutter and
 *   the gap; the card is the element inside them ([LAYOUT_DRAWER], [LAYOUT_CONTENT]).
 *
 * Both cards are `height: 100%` and scroll *inside* themselves rather than growing into the column: a
 * card that grows with its page scrolls its own top edge — and its rounded corners — out of view on the
 * first scroll, while a card that is the viewport keeps its shape and scrolls its content. Padding
 * cannot be replaced by a margin here either: the margin of the only child of a scrolling column adds
 * to the scrollable area, so the column would show a scrollbar of exactly that height.
 *
 * Colours are Lumo custom properties (see [Palette]), so the light/dark switch at the document level
 * repaints every tree without a `[data-theme]` selector.
 */
val LAYOUT_SHEET: CSSStyleSheet = cssSheet {
    ":host" {
        display = Display.block
        height = 100.vh
        // The canvas, not a region: everything that is not a card is this colour.
        backgroundColor = Palette.page
        color = Palette.text
        fontSize = 14.px
        // The app layout's own knobs: a fixed drawer width and the denser Lumo body size.
        set(CustomPropertyName<Length>("--vaadin-app-layout-drawer-width"), 264.px)
        set(CustomPropertyName<Length>("--lumo-font-size-m"), 14.px)
        // The bar's inset and paint are *not* set through the component's custom properties
        // (`--vaadin-app-layout-navbar-padding-*`, `--vaadin-app-layout-navbar-background`): the Lumo
        // theme for the component overrides those with safe-area padding and `--lumo-base-color`, and
        // that theme is inside the component's own tree, so the only place that outranks it is a
        // `::part()` rule ([NAVBAR_PART]).
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

    // One card, three regions: the same surface, radius, edge and shadow. A bar that rounds differently
    // from the surface below it reads as two designs meeting, not as one shell.
    LAYOUT_CARD {
        backgroundColor = Palette.background
        borderRadius = CARD_RADIUS
        border = Border(1.px, LineStyle.solid, CARD_EDGE)
        // One shadow only: a wide, soft drop with a negative spread, so the card lifts off the canvas
        // without a visible dark band under its edge. `--lumo-shade-*` is translucent in both themes,
        // which is what makes it a shadow in the light one and merely a darkening in the dark one.
        boxShadow = BoxShadow(0.px, 12.px, 32.px, -16.px, CARD_SHADOW)
    }

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
        // "Vaadin 25" is one label: a flex row shrinks a short span below its content width and would
        // break it at the space, so it keeps its intrinsic width and never wraps.
        whiteSpace = WhiteSpace.nowrap
        flexShrink = number(0.0)
        // And it is a separate statement from the wordmark, not part of it.
        marginLeft = 8.px
    }

    // The navbar's card: the bar spans the window and its padding is the gutter, so this row *is* the
    // surface, inset by that padding. Its height is the bar's own height — which is exactly what the
    // app layout measures to place the columns below it, so no margin may stand in for its inset.
    LAYOUT_TOPBAR {
        width = 100.pct
        // A card has an inner inset of its own: the toggle and the actions must not touch the rounded
        // edge. `--vaadin-padding-block-container` (6px) would read as a hairline; 8/12 is the same
        // rhythm the button next to it uses.
        paddingBlock = 8.px
        paddingInline = 12.px
    }

    // The drawer's card: the whole side nav, filling the drawer column instead of floating as tall as
    // its items. The padding keeps the item highlights off the rounded edge; the component's own
    // `--vaadin-side-nav-item-padding` still insets the glyphs inside the highlight.
    LAYOUT_DRAWER {
        height = 100.pct
        overflow = Auto.auto
        padding = 8.px
    }

    // The content's card: the page's sheet, fixed to the column and scrolling the page inside it.
    // `--vaadin-vertical-layout-padding` is the `padding` variant's own knob, so the variant stays in
    // charge of the padding — it is only told how much a card insets its content (24px, not the
    // variant's 12px: the page's title line sits right at the top edge of the card).
    LAYOUT_CONTENT {
        height = 100.pct
        overflow = Auto.auto
        set(CustomPropertyName<Length>("--vaadin-vertical-layout-padding"), 24.px)
    }

    // The bar is a strip, not a surface. Its padding is the gutter around the card and its bottom padding
    // is the gap to the columns; its own paint, its bottom hairline and the page-level tint all go, because
    // the card carries them now. `background-image` is cleared as well as the colour: with
    // `primary-section="navbar"` the Lumo theme paints a `contrast-5pct` gradient over the whole bar, which
    // is exactly the grey-on-grey the cards replace.
    NAVBAR_PART {
        padding = Padding(GUTTER, GUTTER, GAP, GUTTER)
        backgroundColor = Color.transparent
        backgroundImage = None.none
        borderBottomStyle = None.none
    }

    // The two columns' gutters. The right padding of the drawer is the *gap* to the content card: the
    // content column starts at the drawer column's right edge (that is the app layout's own offset), so
    // the separation between the two cards belongs to the card on the left.
    DRAWER_PART {
        padding = Padding(0.px, GAP, GUTTER, GUTTER)
        // The column is transparent — the card is the surface — and its `contrast-10pct` end hairline
        // goes with the bar's: the cards are separated by canvas, not by lines.
        backgroundColor = Color.transparent
        borderInlineEndWidth = 0.px
    }

    CONTENT_PART {
        padding = Padding(0.px, GUTTER, GUTTER, 0.px)
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

    // The content column sizes its children to their content instead of stretching them, and a page is
    // the column's only child: without this a page — and every card, grid and table inside it — would be
    // as wide as its longest line rather than as wide as the column. The page's tag is the site's own
    // (`aggrid-overview-page`, …), so the rule keys off the column, not off a tag name.
    "vaadin-vertical-layout > *" {
        width = 100.pct
    }
}
