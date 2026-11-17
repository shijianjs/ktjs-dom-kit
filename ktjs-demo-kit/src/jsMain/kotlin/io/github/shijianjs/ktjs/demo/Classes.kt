package io.github.shijianjs.ktjs.demo

import web.cssom.ClassName

/**
 * The class names that a sheet and a node builder in **different files** agree on.
 *
 * A name lives here only when the rule that selects it and the node that carries it sit in different
 * files. A name used by a single file is declared in that file instead, next to its sheet. A name that
 * only exists as a hook for a test is not a class at all — a test selects the element by its role
 * (`[slot='drawer']`, `vaadin-app-layout`, …).
 */
internal val PAGE_TITLE: ClassName = ClassName("page__title")

internal val PAGE_SUMMARY: ClassName = ClassName("page__summary")

internal val PAGE_LINKS: ClassName = ClassName("page__links")

/** One entry of the page's link row; added by the frame, so a page only supplies the anchors. */
internal val PAGE_LINK: ClassName = ClassName("page__link")

internal val SECTION: ClassName = ClassName("section")

internal val SECTION_TITLE: ClassName = ClassName("section__title")

internal val SECTION_HINT: ClassName = ClassName("section__hint")

internal val EXAMPLE_CAPTION: ClassName = ClassName("example__caption")

internal val ROW: ClassName = ClassName("row")

internal val READOUT: ClassName = ClassName("readout")

internal val CODE: ClassName = ClassName("code")

/** The site title, an `h1` that sits directly in the navbar's `slot="navbar"`. */
internal val SHELL_TITLE: ClassName = ClassName("shell__title")

/** The site mark, immediately left of the title; an `<img>`, sized by the sheet, not by attributes. */
internal val SHELL_LOGO: ClassName = ClassName("shell__logo")

/** The version tag next to the title; hidden when a site reports no version. */
internal val SHELL_VERSION: ClassName = ClassName("shell__version")

/** The navbar as a whole: title and breadcrumbs on the left, actions on the right. */
internal val SHELL_TOPBAR: ClassName = ClassName("shell__topbar")

/** The left half of the navbar; vertical centring comes from a Lumo utility class, not from here. */
internal val SHELL_TOPBAR_START: ClassName = ClassName("shell__topbar-start")

internal val SHELL_ACTIONS: ClassName = ClassName("shell__actions")

/** Size of the Iconify icon in a side-nav item's `prefix` slot; the element renders at `1em`. */
internal val SHELL_NAV_ICON: ClassName = ClassName("shell__nav-icon")
