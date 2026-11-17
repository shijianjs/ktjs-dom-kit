package io.github.shijianjs.ktjs.dom.demo

import io.github.shijianjs.ktjs.demo.Catalog
import io.github.shijianjs.ktjs.demo.Entry
import io.github.shijianjs.ktjs.demo.Group
import io.github.shijianjs.ktjs.dom.demo.page.attribute.AttributePage
import io.github.shijianjs.ktjs.dom.demo.page.elements.ElementsPage
import io.github.shijianjs.ktjs.dom.demo.page.overview.OverviewPage
import io.github.shijianjs.ktjs.dom.demo.page.styling.StylingPage
import io.github.shijianjs.ktjs.dom.demo.page.webcomponent.WebComponentPage

/** Version the page reports; keep it the module's `version`. */
const val KIT_VERSION: String = "0.1.0"

/** Dokka entry point of the API reference, deployed next to the docs site. */
const val API_URL: String = "https://shijianjs.github.io/ktjs-dom-kit/api/"

private val OVERVIEW: Group = Group("overview")

private val DOM: Group = Group("dom")

private val COMPONENTS: Group = Group("components")

/**
 * The site: one entry per page. Adding a page means adding one entry here and its titles in
 * [siteTexts] — the sidebar, the breadcrumb, the router and the page cache all derive from this list.
 */
fun siteCatalog(): Catalog = Catalog(
    listOf(
        Entry("overview", OVERVIEW) { OverviewPage() },
        Entry("elements", DOM) { ElementsPage() },
        Entry("attributes", DOM) { AttributePage() },
        Entry("stylesheets", DOM) { StylingPage() },
        Entry("custom-elements", COMPONENTS) { WebComponentPage() },
    ),
)
