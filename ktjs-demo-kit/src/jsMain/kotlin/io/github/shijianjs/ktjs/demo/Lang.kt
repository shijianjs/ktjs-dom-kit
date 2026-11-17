package io.github.shijianjs.ktjs.demo

import js.reflect.unsafeCast

/**
 * A language the site can show.
 *
 * The value **is** the BCP 47 tag (`en`, `zh-Hans`), modelled as an external interface over a plain
 * JS string. It is not an `enum` on purpose: an enum freezes the set at compile time, and a site must
 * be able to register a language of its own — `Lang("ja")`, then one text object per class. Nothing
 * else in the kit enumerates languages except the list the application hands over.
 *
 * Two tags with the same text are the same language (they are the same string), so a language can be
 * reconstructed anywhere without threading an instance around.
 */
external interface Lang

/** The BCP 47 tag of this language; also written to `<html lang>`. */
val Lang.code: String
    get() = unsafeCast<String>()

/** Registers a language by its BCP 47 tag. */
fun Lang(tag: String): Lang = tag.unsafeCast<Lang>()

/** The two languages this kit names; a site may add any `Lang(tag)` of its own. */
object Langs {
    val EN: Lang = Lang("en")

    val ZH_HANS: Lang = Lang("zh-Hans")
}
