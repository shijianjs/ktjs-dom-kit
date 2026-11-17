package io.github.shijianjs.ktjs.demo

import web.cssom.ClassName

/**
 * The class names the shell and the page frame agree on.
 *
 * A name lives here only when the rule that selects it and the node that carries it sit in
 * different files — every entry below is used by both a sheet and at least one node builder. A name
 * used by a single file is declared in that file instead, next to its sheet.
 */
internal val PAGE_TITLE: ClassName = ClassName("page__title")

internal val PAGE_SUMMARY: ClassName = ClassName("page__summary")

internal val PAGE_LINKS: ClassName = ClassName("page__links")

/** One entry of the page's link row; added by the frame, so a page only supplies the anchors. */
internal val PAGE_LINK: ClassName = ClassName("page__link")

internal val PAGE_BODY: ClassName = ClassName("page__body")

internal val SECTION: ClassName = ClassName("section")

internal val SECTION_TITLE: ClassName = ClassName("section__title")

internal val SECTION_HINT: ClassName = ClassName("section__hint")

internal val SECTION_BODY: ClassName = ClassName("section__body")

internal val EXAMPLE: ClassName = ClassName("example")

internal val EXAMPLE_CAPTION: ClassName = ClassName("example__caption")

internal val ROW: ClassName = ClassName("row")

internal val COL: ClassName = ClassName("col")

internal val GRID: ClassName = ClassName("grid")

internal val READOUT: ClassName = ClassName("readout")

internal val CODE: ClassName = ClassName("code")

internal val SHELL_BRAND: ClassName = ClassName("shell__brand")

internal val SHELL_BRAND_NAME: ClassName = ClassName("shell__brand-name")

internal val SHELL_BRAND_VERSION: ClassName = ClassName("shell__brand-version")

internal val SHELL_NAV: ClassName = ClassName("shell__nav")

internal val SHELL_GROUP: ClassName = ClassName("shell__group")

internal val SHELL_GROUP_TITLE: ClassName = ClassName("shell__group-title")

internal val SHELL_MAIN: ClassName = ClassName("shell__main")

internal val SHELL_TOPBAR: ClassName = ClassName("shell__topbar")

internal val SHELL_BREADCRUMB: ClassName = ClassName("shell__breadcrumb")

internal val SHELL_ACTIONS: ClassName = ClassName("shell__actions")

internal val SHELL_CONTENT: ClassName = ClassName("shell__content")

internal val SHELL_LINK: ClassName = ClassName("shell__link")

internal val NAV_CURRENT: ClassName = ClassName("nav-current")
