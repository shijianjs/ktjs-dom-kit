package io.github.shijianjs.ktjs.demo.i18n

/**
 * One text object per language.
 *
 * This is the whole of the site's translation model: a class holds a `Translations<XxxTexts>` and
 * asks it for a language — no `if (lang == …)` chain, no index into a list, no per-class function.
 * An application declares it in one line and gets the resolution rules for free:
 *
 * ```kotlin
 * private val texts: Translations<OverviewTexts> = texts(
 *     Langs.EN to overviewEnglish,
 *     Langs.ZH_HANS to overviewChinese,
 * )
 * ```
 *
 * A language that has no entry falls back to the **first** declared one, so a newly registered
 * language (`Lang("ja")`) shows something sensible before its own copy lands, and the language menu
 * can list it immediately. The declaration order is also the order of the menu.
 */
class Translations<T : Any> internal constructor(private val byLanguage: Map<Lang, T>) {
    /** The languages this object has copy for, in declaration order. */
    val languages: List<Lang> = byLanguage.keys.toList()

    private val fallback: T = byLanguage.values.first()

    /** Text for [lang], or the first declared language when [lang] has no entry. */
    operator fun get(lang: Lang): T = byLanguage[lang] ?: fallback
}

/** Declares a translation table: `texts(Langs.EN to english, Langs.ZH_HANS to chinese)`. */
fun <T : Any> texts(vararg entries: Pair<Lang, T>): Translations<T> {
    require(entries.isNotEmpty()) { "texts(...) needs at least one language" }
    return Translations(linkedMapOf(*entries))
}
