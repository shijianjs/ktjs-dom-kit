package io.github.shijianjs.ktjs.demo.nav

import io.github.shijianjs.ktjs.demo.i18n.Lang
import web.html.HTMLElement

interface Page {
    /** Root node the layout mounts. */
    val node: HTMLElement

    /** Rewrites every string this page owns to [lang]. */
    fun applyTranslation(lang: Lang)
}
