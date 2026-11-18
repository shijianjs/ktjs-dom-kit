package io.github.shijianjs.ktjs.demo.style

import web.dom.document
import web.storage.localStorage

/**
 * Light / dark / follow-the-browser theme of the site.
 *
 * Lumo 25 resolves its colours through `light-dark()`, so the switch is the document's `color-scheme`:
 * `theme="dark"` for dark, `theme="light-dark"` lets the CSS follow `prefers-color-scheme`, and no
 * attribute at all is the plain light theme. The attribute lives on the document element, so custom
 * properties are inherited into every shadow tree — no component carries its own `[data-theme]`
 * selector, and no component needs a change when the user's preference flips.
 */
enum class ThemeMode {
    LIGHT,
    DARK,

    /** Delegates to the browser: Lumo's `light-dark` colour scheme. */
    SYSTEM,
}

class Theme(private val storageKey: String = "ktjs-demo-theme") {
    var mode: ThemeMode = readStored() ?: ThemeMode.SYSTEM
        private set

    /** Applies the stored mode; call once before building the shell. */
    fun applyStored() {
        apply(mode)
    }

    /** Applies [mode] and persists it. */
    fun apply(mode: ThemeMode) {
        this.mode = mode
        val root = document.documentElement
        when (mode) {
            ThemeMode.LIGHT -> root.removeAttribute("theme")
            ThemeMode.DARK -> root.setAttribute("theme", "dark")
            ThemeMode.SYSTEM -> root.setAttribute("theme", "light-dark")
        }
        runCatching { localStorage.setItem(storageKey, mode.name.lowercase()) }
    }

    private fun readStored(): ThemeMode? = runCatching { localStorage.getItem(storageKey) }.getOrNull()
        ?.let { stored -> ThemeMode.entries.firstOrNull { it.name.lowercase() == stored } }
}
