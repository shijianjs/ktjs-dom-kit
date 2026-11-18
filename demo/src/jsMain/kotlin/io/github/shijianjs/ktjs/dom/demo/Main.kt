package io.github.shijianjs.ktjs.dom.demo

import io.github.shijianjs.ktjs.demo.layout.DemoLayout
import io.github.shijianjs.ktjs.demo.i18n.LanguageState
import io.github.shijianjs.ktjs.demo.layout.LayoutConfig
import io.github.shijianjs.ktjs.demo.style.Theme
import web.dom.ElementId
import web.dom.document

/**
 * Entry point of the demo page.
 *
 * The `<ktjs-demo-layout>` comes from `index.html`: the markup names it, this file defines the tag and
 * mounts the menu into the already-connected element. Creating a second layout here would leave the
 * declared one empty and the site with two sidebars.
 */
fun main() {
    val language = LanguageState()
    language.init()

    val theme = Theme()
    theme.applyStored()

    DemoLayout.define()
    val layout = document.getElementById(ElementId("demo")) as? DemoLayout
        ?: error("index.html has no <ktjs-demo-layout id=\"demo\">")

    layout.mount(
        LayoutConfig(
            menu = siteMenu(),
            language = language,
            theme = theme,
            texts = siteTexts,
            siteVersion = KIT_VERSION,
            docsUrl = DOCS_URL,
            apiUrl = API_URL,
            sourceUrl = SOURCE_URL,
            // The same file the documentation and the README show; the demo build scales it down.
            logoSrc = "img/ktjs-dom-kit-logo.png",
        ),
    )
}
