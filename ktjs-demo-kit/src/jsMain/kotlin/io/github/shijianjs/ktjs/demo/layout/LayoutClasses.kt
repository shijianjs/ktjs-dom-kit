package io.github.shijianjs.ktjs.demo.layout

import web.cssom.ClassName

/**
 * The class names of the layout that a sheet and a node builder in **different files** agree on.
 *
 * `LayoutSheet` declares the rules, `DemoLayout` creates the nodes. A name used by a single file is
 * declared in that file instead, next to its sheet; a name that only exists as a hook for a test is
 * not a class at all — a test selects the element by its role (`[slot='drawer']`, `vaadin-app-layout`,
 * …).
 */
/** The site title, an `h1` inside the home link; the `h1` itself is not the navigation target. */
internal val LAYOUT_TITLE: ClassName = ClassName("layout__title")

/** The home link around the mark and the title; a real `<a href="#/">`, so the wordmark leads home. */
internal val LAYOUT_TITLE_LINK: ClassName = ClassName("layout__title-link")

/** The site mark, immediately left of the title; an `<img>`, sized by the sheet, not by attributes. */
internal val LAYOUT_LOGO: ClassName = ClassName("layout__logo")

/** The version tag next to the title; hidden when a site reports no version. */
internal val LAYOUT_VERSION: ClassName = ClassName("layout__version")

/** The navbar as a whole: title and breadcrumbs on the left, actions on the right. */
internal val LAYOUT_TOPBAR: ClassName = ClassName("layout__topbar")

/**
 * The drawer toggle: the component's own margin/padding are dropped so its glyph sits at the bar's
 * inset instead of inside a 44px box with blanks on both sides.
 */
internal val LAYOUT_TOGGLE: ClassName = ClassName("layout__toggle")

/** The left half of the navbar; vertical centring comes from a Lumo utility class, not from here. */
internal val LAYOUT_TOPBAR_START: ClassName = ClassName("layout__topbar-start")

internal val LAYOUT_ACTIONS: ClassName = ClassName("layout__actions")

/** Size of the Iconify icon in a side-nav item's `prefix` slot; the element renders at `1em`. */
internal val LAYOUT_NAV_ICON: ClassName = ClassName("layout__nav-icon")
