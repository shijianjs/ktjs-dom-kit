package io.github.shijianjs.ktjs.demo

/** The two languages a demo site switches between, in place. */
enum class Lang(val code: String, val label: String) {
    EN("en", "EN"),
    ZH("zh-Hans", "中文"),
}

/** The other language, i.e. what the switch button offers. */
fun Lang.other(): Lang = if (this == Lang.EN) Lang.ZH else Lang.EN

/** A string in both languages, resolved on display. */
class Bilingual(private val zh: String, private val en: String) {
    fun resolve(lang: Lang): String = if (lang == Lang.ZH) zh else en
}

/** Shorthand for `Bilingual(zh, en)` at a call site. */
fun bilingual(zh: String, en: String): Bilingual = Bilingual(zh, en)
