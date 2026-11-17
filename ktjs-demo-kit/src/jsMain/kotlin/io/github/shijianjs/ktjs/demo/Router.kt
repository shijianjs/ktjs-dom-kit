package io.github.shijianjs.ktjs.demo

import web.events.Event
import web.events.EventType
import web.events.EventHandler
import web.events.addEventListener
import web.location.location
import web.window.window

/**
 * Hash-based client-side router.
 *
 * A demo site is published as static files (GitHub Pages, or any plain file server for the end-to-end
 * suite), so routes live in the fragment: `#/grid`. That keeps relative asset URLs working on a
 * project page served from `/<repo>/demo/`, gives history navigation for free, and lets the
 * documentation link to one page of the demo directly.
 */
class Router(
    val defaultRoute: String = "overview",
    private val onRoute: (String) -> Unit,
) {
    /** Parses the route out of a `location.hash` value such as `#/text-field`. */
    fun parse(hash: String): String {
        val route = hash.removePrefix("#").removePrefix("/").trim()
        return route.ifEmpty { defaultRoute }
    }

    /** The route currently in the URL. */
    fun current(): String = parse(location.hash)

    /** Fragment of [route], shared by the router and the navigation items. */
    fun hashOf(route: String): String = "#/$route"

    /** Navigates to [route]; dispatches directly when the fragment would not change. */
    fun navigate(route: String) {
        val target = hashOf(route)
        if (location.hash == target) {
            onRoute(route)
        } else {
            location.hash = target
        }
    }

    /** Dispatches the route already in the URL and subscribes to fragment changes. */
    fun start() {
        window.addEventListener(EventType<Event>("hashchange"), EventHandler { onRoute(current()) })
        onRoute(current())
    }
}
