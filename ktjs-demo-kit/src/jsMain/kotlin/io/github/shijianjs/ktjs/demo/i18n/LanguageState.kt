package io.github.shijianjs.ktjs.demo.i18n

import web.dom.document
import web.storage.localStorage

/**
 * The active language of the site.
 *
 * One instance per site, created in `main` and handed to the layout. [languages] is the site's own
 * registration list — the kit starts with [Langs.EN] and [Langs.ZH_HANS], and a site adds any other
 * `Lang(tag)` it has copy for; the language menu and the stored value are both checked against it.
 *
 * The state is persisted so a reload keeps the language, and `document.documentElement.lang` follows
 * it — that is what screen readers and hyphenation read.
 *
 * Listeners are notified in place: switching is a walk over the mounted pages, never a rebuild.
 */
class LanguageState(
    val languages: List<Lang> = listOf(Langs.EN, Langs.ZH_HANS),
    private val storageKey: String = "ktjs-demo-lang",
    initial: Lang = languages.first(),
) {
    init {
        require(languages.isNotEmpty()) { "a site needs at least one language" }
        require(languages.any { it == initial }) { "the initial language ${initial.code} is not registered" }
    }

    var current: Lang = readStored() ?: initial
        private set

    private val listeners: MutableList<(Lang) -> Unit> = mutableListOf()

    /** Applies the stored language to the document and notifies every listener once. */
    fun init() {
        applyDocumentLang()
        listeners.forEach { it(current) }
    }

    /** Switches to [lang] and tells every listener; a no-op when it is already active. */
    fun set(lang: Lang) {
        if (lang == current) return
        current = lang
        store(lang)
        applyDocumentLang()
        listeners.forEach { it(lang) }
    }

    /** Registers [listener] for later switches. */
    fun onChange(listener: (Lang) -> Unit) {
        listeners += listener
    }

    private fun applyDocumentLang() {
        document.documentElement.lang = current.code
    }

    private fun readStored(): Lang? =
        runCatching { localStorage.getItem(storageKey) }.getOrNull()?.let { stored ->
            languages.firstOrNull { it.code == stored }
        }

    private fun store(lang: Lang) {
        runCatching { localStorage.setItem(storageKey, lang.code) }
    }
}
