package io.github.shijianjs.ktjs.demo.layout

import io.github.shijianjs.ktjs.demo.style.Palette
import io.github.shijianjs.ktjs.dom.css.cssSheet
import io.github.shijianjs.ktjs.dom.css.dsl.CssSelector
import io.github.shijianjs.ktjs.dom.css.dsl.Selectors
import io.github.shijianjs.ktjs.dom.css.dsl.child
import io.github.shijianjs.ktjs.dom.css.dsl.className
import io.github.shijianjs.ktjs.dom.css.dsl.colorStop
import io.github.shijianjs.ktjs.dom.css.dsl.compound
import io.github.shijianjs.ktjs.dom.css.dsl.invoke
import io.github.shijianjs.ktjs.dom.css.dsl.or
import io.github.shijianjs.ktjs.dom.css.dsl.pseudo
import io.github.shijianjs.ktjs.dom.css.dsl.pseudoElement
import io.github.shijianjs.ktjs.dom.css.dsl.selectorList
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
 * One value on all four sides, and deliberately small: a shell spends this space twice per axis, so a
 * "comfortable" 16px here costs the content 32px of height on a 914px window. 8 is the point where the
 * cards still read as floating and the page keeps nearly the area it had before they existed.
 */
private val GUTTER: Length = 8.px

/** The space between two cards — the same step as [GUTTER], so the shell is one grid and not two. */
private val GAP: Length = 8.px

/**
 * A card's corner radius — bigger than any control's, because a card is a surface and not a control.
 *
 * Deliberately not `--lumo-border-radius-l`: that token also rounds dialogs, fields and buttons, and
 * 16px on a 32px control turns it into a pill.
 */
private val CARD_RADIUS: Length = 16.px

/**
 * A card's shadow — now the only thing that separates it from the canvas, since the hairline is gone.
 *
 * Lumo's 20 % shade is a *translucent* dark in both themes, so one value works for both without a
 * `light-dark()` pair of its own.
 */
private val CARD_SHADOW: Color = `var`(CustomPropertyName<Color>("--lumo-shade-20pct"), rgb(207, 214, 224))

/**
 * A card's scrollbar thumb: Lumo's 30 % contrast, translucent in both themes like the shadow.
 *
 * The cards are their own scroll viewports, so the platform's classic scrollbar would be painted on
 * top of the surface — a square, opaque strip over the rounded corner on the right. The three rules
 * below replace it with a thin, inset pill on a transparent track, which is the one way to keep both
 * the card's shape and a visible scroll affordance.
 */
private val CARD_THUMB: Color = `var`(CustomPropertyName<Color>("--lumo-contrast-30pct"), rgb(151, 158, 168))

/** The width of a card's own scrollbar: thin enough to read as part of the card's inset. */
private val SCROLLBAR_WIDTH: Length = 10.px

/**
 * One part of a card's scrollbar — `::-webkit-scrollbar`, `…-track`, `…-thumb` — on both cards.
 *
 * A selector list rather than a rule nested in each card's block: the two cards scroll for the same
 * reason and must look the same while doing it, and a part of a scrollbar cannot be reached with the
 * `pseudo { }` helper because that attaches to a single parent rule.
 */
private fun cardScrollbarPart(name: String): CssSelector = selectorList(
    compound(className(LAYOUT_DRAWER), pseudo(pseudoElement("-webkit-scrollbar$name"))),
    compound(className(LAYOUT_CONTENT), pseudo(pseudoElement("-webkit-scrollbar$name"))),
)


/**
 * The parts of the card scrollbar, resolved once: the track is invisible and the thumb is the only
 * thing painted, so the card's rounded corner is never covered by a track.
 */
private val CARD_SCROLLBAR: CssSelector = cardScrollbarPart("")

private val CARD_SCROLLBAR_TRACK: CssSelector = cardScrollbarPart("-track")

private val CARD_SCROLLBAR_THUMB: CssSelector = cardScrollbarPart("-thumb")

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
 * - the navbar bar keeps no background of its own and no minimum height. Its padding is the gutter
 *   *inside* the 56px a Vaadin navbar is, so the card is the row we own ([LAYOUT_TOPBAR]) and the cards
 *   cost the page two gutters of height, nothing more. A margin on the fixed bar would be outside the
 *   box the component measures, and the content below would start under it.
 * - the drawer and content columns are `::part()`s of the app layout. Their padding is the gutter and
 *   the gap; the card is the element inside them ([LAYOUT_DRAWER], [LAYOUT_CONTENT]).
 *
 * Both cards are `height: 100%` rather than growing into the column: a card that grows with its page
 * scrolls its own top edge — and its rounded corners — out of view on the first scroll, while a card
 * that is the viewport keeps its shape and scrolls what is inside it. Padding cannot be replaced by a
 * margin here either: the margin of the only child of a scrolling column adds to the scrollable area,
 * so the column would show a scrollbar of exactly that height.
 *
 * What scrolls inside the content card is the *page*, one step in at the card's padding box: a
 * scrollbar sits on the edge of whatever scrolls it, and on the card's own edge it would be a square
 * strip across the rounded corner. The drawer scrolls its side nav, and both get a thin transparent
 * scrollbar of their own so the platform's classic bar never lands on a corner either.
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

    // One card, three regions: the same surface, radius and shadow. A bar that rounds differently
    // from the surface below it reads as two designs meeting, not as one shell. No border: the
    // surface against the canvas is enough separation, and a 1px line drawn around a rounded card
    // turns it back into a box — the shadow is what lifts it, and it stays a shadow, not a band.
    LAYOUT_CARD {
        backgroundColor = Palette.background
        borderRadius = CARD_RADIUS
        // One shadow only (cssom's `boxShadow` takes a single `BoxShadow`): short and soft, a surface
        // resting on the canvas rather than a raised button.
        boxShadow = BoxShadow(0.px, 2.px, 10.px, -3.px, CARD_SHADOW)
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
    // surface, inset by that padding. No block padding — the row's own height (the toggle's 40px) is
    // what fills the bar, which is how the bar stays the 56px every Vaadin navbar is. Its height is
    // what the app layout measures to place the columns below it, so no margin may stand in for the
    // inset.
    LAYOUT_TOPBAR {
        width = 100.pct
        paddingInline = 10.px
    }

    // The drawer's card: the whole side nav, filling the drawer column instead of floating as tall as
    // its items. 4px keeps the item highlights off the rounded edge without spending a row of height on
    // nothing; the component's own `--vaadin-side-nav-item-padding` still insets the glyphs inside the
    // highlight.
    LAYOUT_DRAWER {
        height = 100.pct
        overflow = Auto.auto
        padding = 4.px
    }

    // The content's card: the page's sheet, fixed to the column, and the page scrolls *inside its padding*
    // — not the card itself. The card is a rounded surface, and a scrollbar painted on the card's own
    // edge is a square strip laid across the top-right corner, which is what turns a card into a panel
    // with a cut corner. One step in, at the padding box, the scrollbar starts below the corner arc.
    // `--vaadin-vertical-layout-padding` is the `padding` variant's own knob, so the variant stays in
    // charge of the padding — it is only told how much a card insets its content (16px: the page brings
    // its own rhythm, and 24 was white space charged twice).
    LAYOUT_CONTENT {
        height = 100.pct
        set(CustomPropertyName<Length>("--vaadin-vertical-layout-padding"), 16.px)
    }

    // The page is the content card's scroller, and it is the column's only child, so one rule covers
    // every page: fill the card's content box and scroll whatever runs past it.
    (className(LAYOUT_CONTENT) child Selectors.universal) {
        height = 100.pct
        overflow = Auto.auto
    }

    // The cards are their own scroll viewports, and the platform's classic scrollbar would be painted
    // on top of the surface as an opaque square strip across the rounded right corner. Ours is a 10px
    // strip whose track paints nothing (so the card's white and its corner show through) and whose thumb
    // is a 4px pill inset 3px — a scroll affordance that never touches the corner arc.
    CARD_SCROLLBAR {
        width = SCROLLBAR_WIDTH
        backgroundColor = Color.transparent
    }

    CARD_SCROLLBAR_TRACK {
        backgroundColor = Color.transparent
    }

    CARD_SCROLLBAR_THUMB {
        backgroundColor = CARD_THUMB
        borderRadius = 8.px
        // A transparent border is how a webkit thumb is inset: the pill is what remains of the 10px
        // strip after `background-clip` cuts the border away.
        border = Border(3.px, LineStyle.solid, Color.transparent)
        backgroundClip = BackgroundClip.paddingBox
    }

    // The bar is a strip, not a surface. Its padding is the gutter around the card, and the same
    // padding under it is the gap to the columns below; its own paint, its bottom hairline and the
    // page-level tint all go, because the card carries them now. `background-image` is cleared as well
    // as the colour: with `primary-section="navbar"` the Lumo theme paints a `contrast-5pct` gradient
    // over the whole bar, which is exactly the grey-on-grey the cards replace.
    NAVBAR_PART {
        // The base style gives the bar `min-height: 56px` and Lumo pads it with the safe area; both are
        // *inside* the box, so with a gutter of our own the bar would be 56 + 16. The gutter is part of
        // the 56 instead: no minimum height, and the bar is exactly as tall as the row it holds.
        minHeight = 0.px
        padding = Padding(GUTTER, GUTTER, GUTTER, GUTTER)
        backgroundColor = Color.transparent
        backgroundImage = None.none
        borderBottomStyle = None.none
    }

    // The drawer's gutter: the window edge on the left, the bottom edge under it, and nothing on the
    // right — the gap to the content card belongs to the content column instead (next rule), so that
    // it is still there when the drawer is collapsed and the column becomes the first one.
    DRAWER_PART {
        padding = Padding(0.px, 0.px, GUTTER, GUTTER)
        // The column is transparent — the card is the surface — and its `contrast-10pct` end hairline
        // goes with the bar's: the cards are separated by canvas, not by lines.
        backgroundColor = Color.transparent
        borderInlineEndWidth = 0.px
    }

    // The content column's own gutter on all three sides it owns: the left one is the gap to the drawer
    // when it is open and the window margin when it is collapsed, and one value does both.
    CONTENT_PART {
        padding = Padding(0.px, GUTTER, GUTTER, GAP)
    }

    // The toggle is a 44px icon-button box around a 21px glyph: Lumo's own 9px padding and 8px margin
    // around it read as a blank gap between the toggle and the wordmark, on top of the layout's gap.
    // Both are dropped here; the inset comes from [LAYOUT_TOPBAR], the separation from the layout gap.
    //
    // 40 instead of 44 as well: the toggle is the tallest thing in the bar, so its box is what decides
    // the navbar's height. At 44 the bar cannot be 56 (gutter + 44 + gutter) without eating the gutter;
    // at 40 the bar is exactly 56 and the toggle keeps a 40px square target, which is the button size
    // Lumo gives a bare icon button anyway.
    LAYOUT_TOGGLE {
        marginInlineStart = 0.px
        marginInlineEnd = 0.px
        paddingInlineStart = 0.px
        paddingInlineEnd = 0.px
        height = 40.px
        width = 40.px
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
