package io.github.shijianjs.ktjs.demo

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The translation model: a language is its tag, and a table resolves per language with a first-entry
 * fallback. These are the guarantees a site relies on when it registers a language of its own.
 */
class TranslationsTest {
    private class Copy(val title: String)

    @Test
    fun `a language is its tag`() {
        val japanese: Lang = Lang("ja")
        assertEquals("ja", japanese.code)
        assertEquals(Lang("en"), Langs.EN)
    }

    @Test
    fun `a registered language resolves its own copy`() {
        val texts: Translations<Copy> = texts(
            Langs.EN to Copy("Overview"),
            Lang("ja") to Copy("概要"),
        )
        assertEquals("概要", texts[Lang("ja")].title)
    }

    @Test
    fun `an unregistered language falls back to the first declaration`() {
        val texts: Translations<Copy> = texts(
            Langs.EN to Copy("Overview"),
            Langs.ZH_HANS to Copy("概览"),
        )
        assertEquals("Overview", texts[Lang("de")].title)
        assertEquals(listOf(Langs.EN, Langs.ZH_HANS), texts.languages)
    }
}
