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

internal val PAGE_BODY: ClassName = ClassName("page__body")

internal val SECTION: ClassName = ClassName("section")

internal val SECTION_TITLE: ClassName = ClassName("section__title")

internal val SECTION_HINT: ClassName = ClassName("section__hint")

internal val EXAMPLE_CAPTION: ClassName = ClassName("example__caption")

internal val ROW: ClassName = ClassName("row")

internal val READOUT: ClassName = ClassName("readout")

internal val CODE: ClassName = ClassName("code")

internal val SHELL_BRAND_NAME: ClassName = ClassName("shell__brand-name")

internal val SHELL_BRAND_VERSION: ClassName = ClassName("shell__brand-version")

/** The brand stack that rides in the side nav's `label` slot. */
internal val SHELL_BRAND: ClassName = ClassName("shell__brand")

internal val SHELL_TOPBAR: ClassName = ClassName("shell__topbar")


internal val SHELL_LINK: ClassName = ClassName("shell__link")

