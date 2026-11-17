package io.github.shijianjs.ktjs.demo

/**
 * Every string the shell and the navigation show, in one language.
 *
 * The site keeps one instance per language (`englishSite` / `chineseSite` … in the application) and
 * hands the shell a `(Lang) -> SiteTexts` function, so a new language is a new instance and nothing
 * else. Body copy of a page is not here: each page owns its own text class, the same way — one
 * instance per language, resolved through a function of the page.
 *
 * [groupTitles] and [pageTitles] are keyed by the stable ids of [Group] and [Entry]; an id missing
 * from the map falls back to the id itself, so a new page shows up in the navigation before its
 * translation lands.
 */
class SiteTexts(
    val siteTitle: String,
    val langLabel: String,
    val apiLabel: String,
    val lightLabel: String,
    val darkLabel: String,
    val groupTitles: Map<String, String>,
    val pageTitles: Map<String, String>,
)
