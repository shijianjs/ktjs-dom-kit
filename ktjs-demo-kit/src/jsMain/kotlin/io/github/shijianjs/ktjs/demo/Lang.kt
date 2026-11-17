package io.github.shijianjs.ktjs.demo

/**
 * A language the site ships.
 *
 * Only the tag travels with the language: every string lives in a [SiteTexts] instance or in a page's
 * own text class, one instance per language. Adding a language therefore means adding one instance of
 * each text class and one enum entry — never editing a string that already exists.
 */
enum class Lang(val code: String) {
    EN("en"),
    ZH("zh-Hans"),
}

/** The other language, i.e. what the switch button offers. */
fun Lang.other(): Lang = if (this == Lang.EN) Lang.ZH else Lang.EN
