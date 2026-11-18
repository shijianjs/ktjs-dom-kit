package io.github.shijianjs.ktjs.demo.style

import io.github.shijianjs.ktjs.dom.css.cssSheet
import web.cssom.BoxSizing
import web.cssom.CSSStyleSheet
import web.cssom.Display

/**
 * The one sheet **every** shadow root of a demo site adopts: `:host { display: block }` (a custom
 * element is inline by default) and `box-sizing`, which is not inherited and therefore has to be
 * redeclared inside each tree — that is the reason a reset is shared at all.
 *
 * Everything else stays with its owner: the page's typography in
 * [io.github.shijianjs.ktjs.demo.page.PAGE_SHEET], the layout's sizing in
 * [io.github.shijianjs.ktjs.demo.layout.LAYOUT_SHEET].
 */
val RESET: CSSStyleSheet = cssSheet {
    ":host" { display = Display.block }

    "*, *::before, *::after" { boxSizing = BoxSizing.borderBox }
}
