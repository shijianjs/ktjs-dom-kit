package io.github.shijianjs.ktjs.dom.demo

import io.github.shijianjs.ktjs.demo.Chrome
import io.github.shijianjs.ktjs.demo.DemoShell
import io.github.shijianjs.ktjs.demo.LanguageState
import io.github.shijianjs.ktjs.demo.Theme
import web.dom.ElementId
import web.dom.document

/**
 * Entry point of the demo page.
 *
 * The `<ktjs-demo-shell>` comes from `index.html`: the markup names it, this file defines the tag and
 * mounts the catalog into the already-connected element. Creating a second shell here would leave the
 * declared one empty and the site with two sidebars.
 */
fun main() {
    val language = LanguageState()
    language.init()

    val theme = Theme()
    theme.applyStored()

    DemoShell.define()
    val shell = document.getElementById(ElementId("demo")) as? DemoShell ?: error("index.html has no <ktjs-demo-shell id=\"demo\">")

    shell.mount(siteCatalog(), language, theme, Chrome(SITE_TITLE, KIT_VERSION, API_URL))
}
