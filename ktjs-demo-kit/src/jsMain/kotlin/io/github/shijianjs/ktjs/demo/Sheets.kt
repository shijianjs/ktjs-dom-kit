package io.github.shijianjs.ktjs.demo

import io.github.shijianjs.ktjs.dom.css.cssSheet
import web.cssom.BoxSizing
import web.cssom.CSSStyleSheet
import web.cssom.Display
import web.cssom.Margin
import web.cssom.Padding
import web.cssom.number
import web.cssom.px

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

    ".page__title" {
        margin = Margin(0.px, 0.px, 6.px)
        fontSize = 26.px
        color = Palette.text
    }

    ".page__summary" {
        margin = Margin(0.px, 0.px, 10.px)
        color = Palette.muted
        fontSize = 14.px
        lineHeight = number(1.55)
    }

    ".page__links" {
        margin = Margin(0.px, 0.px, 22.px)
        fontSize = 13.px
    }

    ".page__link" {
        marginRight = 14.px
        color = Palette.accent
    }

    ".page__body" {
        display = Display.block
    }

    ".section" {
        marginTop = 28.px
    }

    ".section__title" {
        margin = Margin(0.px, 0.px, 4.px)
        fontSize = 18.px
        color = Palette.text
    }

    ".section__hint" {
        margin = Margin(0.px, 0.px, 14.px)
        color = Palette.muted
        fontSize = 13.px
        lineHeight = number(1.5)
    }

    ".section__body" {
        display = Display.block
    }

    ".example" {
        display = Display.block
        backgroundColor = Palette.surface
        borderRadius = 10.px
        padding = Padding(16.px, 16.px, 16.px, 16.px)
        marginBottom = 12.px
    }

    ".example__caption" {
        margin = Margin(0.px, 0.px, 10.px)
        color = Palette.muted
        fontSize = 12.px
    }

    ".row" {
        display = Display.flex
        columnGap = 10.px
        rowGap = 10.px
        flexWrap = web.cssom.FlexWrap.wrap
        alignItems = web.cssom.AlignItems.center
    }

    ".col" {
        display = Display.flex
        flexDirection = web.cssom.FlexDirection.column
        rowGap = 10.px
    }

    ".grid" {
        display = Display.grid
        columnGap = 12.px
        rowGap = 12.px
    }

    ".readout" {
        margin = Margin(8.px, 0.px, 0.px)
        color = Palette.muted
        fontSize = 13.px
    }

    ".event-log" {
        margin = Margin(10.px, 0.px, 0.px)
        padding = Padding(0.px, 0.px, 0.px, 0.px)
        fontSize = 12.px
        color = Palette.muted
    }

    ".code" {
        backgroundColor = Palette.surfaceRaised
        borderRadius = 6.px
        padding = Padding(2.px, 6.px, 2.px, 6.px)
        fontSize = 12.px
    }
}

/** The shell: a sidebar column and the content column beside it. */
val SHELL_SHEET: CSSStyleSheet = cssSheet {
    ":host" {
        display = Display.grid
        backgroundColor = Palette.background
        color = Palette.text
        fontSize = 14.px
    }

    ".shell__nav" {
        display = Display.flex
        flexDirection = web.cssom.FlexDirection.column
        rowGap = 16.px
        padding = Padding(20.px, 16.px, 20.px, 16.px)
        backgroundColor = Palette.surface
    }

    ".shell__brand" {
        display = Display.flex
        flexDirection = web.cssom.FlexDirection.column
        rowGap = 2.px
    }

    ".shell__brand-name" {
        fontSize = 18.px
        color = Palette.text
    }

    ".shell__brand-version" {
        fontSize = 11.px
        color = Palette.muted
    }

    ".shell__group" {
        display = Display.flex
        flexDirection = web.cssom.FlexDirection.column
        rowGap = 4.px
    }

    ".shell__group-title" {
        backgroundColor = Palette.surface
        padding = Padding(2.px, 0.px)
        fontSize = 11.px
        color = Palette.muted
        textAlign = web.cssom.TextAlign.left
    }

    ".shell__main" {
        display = Display.flex
        flexDirection = web.cssom.FlexDirection.column
        minWidth = 0.px
    }

    ".shell__topbar" {
        display = Display.flex
        alignItems = web.cssom.AlignItems.center
        justifyContent = web.cssom.JustifyContent.spaceBetween
        columnGap = 12.px
        padding = Padding(12.px, 24.px, 12.px, 24.px)
        backgroundColor = Palette.surface
    }

    ".shell__breadcrumb" {
        color = Palette.muted
        fontSize = 12.px
    }

    ".shell__actions" {
        display = Display.flex
        alignItems = web.cssom.AlignItems.center
        columnGap = 8.px
    }

    ".shell__content" {
        padding = Padding(24.px, 32.px, 48.px, 32.px)
        minWidth = 0.px
    }

    ".shell__link" {
        marginRight = 12.px
        color = Palette.accent
        fontSize = 12.px
    }

    ".nav-current" {
        color = Palette.accent
    }
}
