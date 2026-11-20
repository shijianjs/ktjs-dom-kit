package io.github.shijianjs.ktjs.demo.nav

/**
 * One page of a demo site.
 *
 * The page owns its own text: [applyTranslation] rewrites the nodes the page holds, it never
 * rebuilds the tree, so a language switch keeps scroll position and whatever the user typed or
 * opened. Text that is derived from data (a member signature, a resource path) is not translated.
 */

/**
 * One sidebar group; [id] is stable across languages, its title comes from [SiteTexts.groupTitles].
 *
 * [icon] is an Iconify name (for example `lucide:layers`) drawn before the group title; `null` keeps
 * the group text-only. The wrapper is supplied by the application, so the frame carries no icon set.
 */

/**
 * One page of the site.
 *
 * [create] builds the page once; the layout caches the result per route, so component state survives
 * a trip to another page and back. [icon] is an Iconify name drawn before the page title in the
 * sidebar.
 */

/**
 * The whole site: entries in navigation order. Groups are derived from them, so adding a page means
 * adding one entry and nothing else.
 */
class DemoMenu(val entries: List<Entry>) {
    val groups: List<Group> = entries.map { it.group }.distinct()

    fun entry(id: String): Entry? = entries.firstOrNull { it.id == id }

    fun entriesOf(group: Group): List<Entry> = entries.filter { it.group == group }
}
