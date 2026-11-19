package io.github.shijianjs.ktjs.demo.page

import web.cssom.ClassName

/**
 * The class names of a page that a sheet and a node builder in **different files** agree on.
 *
 * A name lives here only when the rule that selects it and the node that carries it sit in different
 * files (`PageSheet` declares the rules, `DemoPage` / `Blocks` create the nodes). A name used by a
 * single file is declared in that file instead, next to its sheet. A name that only exists as a hook
 * for a test is not a class at all — a test selects the element by its role (`[slot='drawer']`,
 * `vaadin-app-layout`, …).
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

/** A responsive grid of cards or boxes. */
internal val GRID: ClassName = ClassName("grid")

internal val READOUT: ClassName = ClassName("readout")

/** The box an [EventLog] appends its lines to. */
internal val EVENT_LOG: ClassName = ClassName("event-log")

/** One line inside an [EventLog]. */
internal val EVENT_LINE: ClassName = ClassName("event-log__line")

internal val CODE: ClassName = ClassName("code")
