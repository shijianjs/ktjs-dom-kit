package io.github.shijianjs.ktjs.demo.layout

import io.github.shijianjs.ktjs.demo.i18n.LanguageState
import io.github.shijianjs.ktjs.demo.i18n.SiteTexts
import io.github.shijianjs.ktjs.demo.i18n.Translations
import io.github.shijianjs.ktjs.demo.nav.DemoMenu
import io.github.shijianjs.ktjs.demo.style.Theme

/**
 * Everything the frame needs to render a site, in one object: the pages and the runtime state it drives
 * ([menu], [language], [theme], [texts]), plus the site's identity and destination URLs.
 *
 * One object rather than five parameters on [DemoLayout.mount]: a method whose argument list keeps growing
 * is a config class waiting to be written, and a layout gaining a sixth knob (a base path, a feedback
 * link) then costs no call-site change. The site title itself is not here — it lives in [SiteTexts],
 * because it changes with the language; the version tag and the targets do not. Every URL is optional:
 * a site with only a repository gets a menu with one entry, and a site with none hides the menu
 * entirely. [logoSrc] is a plain image URL, so a site reuses the very file its documentation and README
 * show instead of inventing a second mark.
 */
class LayoutConfig(
    /** The site's pages, grouped as the drawer groups them; also the router's source of truth. */
    val menu: DemoMenu,
    /** The languages the site offers, and the one selected now; the language menu is built from it. */
    val language: LanguageState,
    /** Light / dark / follow-the-browser state; the layout owns the menu, this object owns the value. */
    val theme: Theme,
    /** One translation table per language, declared with `texts(...)`. */
    val texts: Translations<SiteTexts>,
    /** Shown next to the title in the navbar, e.g. `Kotlin/JS 2.4.21`; empty hides the line. */
    val siteVersion: String = "",
    /** Documentation site, usually the root the demo is published under. */
    val docsUrl: String? = null,
    /** Dokka entry point of the wrapper this demo belongs to. */
    val apiUrl: String? = null,
    /** Source repository. */
    val sourceUrl: String? = null,
    /** Site mark, drawn immediately left of the title; `null` leaves the title to stand alone. */
    val logoSrc: String? = null,
)
