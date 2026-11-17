package io.github.shijianjs.ktjs.demo

import web.html.HTMLElement

/**
 * One page of a demo site.
 *
 * The page owns its own text: [applyTranslation] rewrites the nodes the page holds, it never
 * rebuilds the tree, so a language switch keeps scroll position and whatever the user typed or
 * opened. Text that is derived from data (a member signature, a resource path) is not translated.
 */
interface Page {
    /** Root node the shell mounts. */
    val node: HTMLElement

    /** Rewrites every string this page owns to [lang]. */
    fun applyTranslation(lang: Lang)
}

/**
 * One sidebar group; [id] is stable across languages, its title comes from [SiteTexts.groupTitles].
 *
 * [icon] is an Iconify name (for example `lucide:layers`) drawn before the group title; `null` keeps
 * the group text-only. The wrapper is supplied by the application, so the frame carries no icon set.
 */
class Group(val id: String, val icon: String? = null)

/**
 * One page of the site.
 *
 * [create] builds the page once; the shell caches the result per route, so component state survives
 * a trip to another page and back. [apiUrl] is the Dokka page of the wrapper behind this page, when
 * the site has one. [icon] is an Iconify name drawn before the page title in the sidebar.
 */
class Entry(
    val id: String,
    val group: Group,
    val icon: String? = null,
    val apiUrl: (Lang) -> String? = { null },
    val create: () -> Page,
)

/**
 * The whole site: entries in navigation order. Groups are derived from them, so adding a page means
 * adding one entry and nothing else.
 */
class Catalog(val entries: List<Entry>) {
    val groups: List<Group> = entries.map { it.group }.distinct()

    fun entry(id: String): Entry? = entries.firstOrNull { it.id == id }

    fun entriesOf(group: Group): List<Entry> = entries.filter { it.group == group }
}
