package io.github.shijianjs.ktjs.demo

import web.dom.document
import web.cssom.MediaQuery
import web.cssom.matchMedia
import web.storage.localStorage

/**
 * Light / dark theme of the site.
 *
 * Lumo's dark theme is a top-level switch: the stylesheet selects on `html[theme~="dark"]`, so the
 * only thing to do here is put that attribute on the document element and remember the choice.
 * Components inside shadow trees follow the tokens it flips — no component carries its own
 * `[data-theme]` selector.
 */
class Theme(private val storageKey: String = "ktjs-demo-theme") {
    var isDark: Boolean = readStored() ?: prefersDark()
        private set

    /** Applies the stored (or system) theme; call once before building the shell. */
    fun applyStored() {
        apply(isDark)
    }

    /** Applies [dark] and persists it. */
    fun apply(dark: Boolean) {
        isDark = dark
        val root = document.documentElement
        if (dark) {
            root?.setAttribute("theme", "dark")
        } else {
            root?.removeAttribute("theme")
        }
        runCatching { localStorage.setItem(storageKey, if (dark) "dark" else "light") }
    }

    private fun prefersDark(): Boolean =
        runCatching { matchMedia(MediaQuery("(prefers-color-scheme: dark)")).matches }.getOrDefault(false)

    private fun readStored(): Boolean? =
        runCatching { localStorage.getItem(storageKey) }.getOrNull()?.let { stored ->
            if (stored == "dark") true else if (stored == "light") false else null
        }
}
