package io.github.shijianjs.ktjs.demo.i18n

/**
 * Every string the layout and the navigation show, in one language.
 *
 * The site keeps one instance per language (`englishSite` / `chineseSite` … in the application) and
 * declares the table with [texts]; a new language is one more entry there plus the same in each page's
 * text class, and no existing string is touched. Body copy of a page is not here: each page owns its
 * own text class the same way.
 *
 * The menu bars use fixed nouns for their buttons ([languageLabel], [themeLabel], [docsLabel]) and put
 * the current value in the menu itself, so opening and choosing never resizes the navbar.
 *
 * [groupTitles] and [pageTitles] are keyed by the stable ids of [Group] and [Entry]; an id missing
 * from the map falls back to the id itself, so a new page shows up in the navigation before its
 * translation lands.
 */
class SiteTexts(
    val siteTitle: String,
    /** Label of this language, shown as the entry in the language menu. */
    val langLabel: String,
    /** Button that opens the language menu. */
    val languageLabel: String,
    /** Button that opens the theme menu. */
    val themeLabel: String,
    /** Button that opens the link menu. */
    val docsLabel: String,
    /** Entry pointing at the documentation site. */
    val siteDocsLabel: String,
    /** Entry pointing at the API reference. */
    val apiLabel: String,
    /** Entry pointing at the source repository. */
    val sourceLabel: String,
    val lightLabel: String,
    val darkLabel: String,
    /** Theme entry that follows the browser's `prefers-color-scheme`. */
    val systemLabel: String,
    val groupTitles: Map<String, String>,
    val pageTitles: Map<String, String>,
)
