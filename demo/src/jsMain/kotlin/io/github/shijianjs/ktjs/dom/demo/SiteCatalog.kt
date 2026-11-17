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

/** The documentation site of this kit; its guide pages describe the very demo next to it. */
const val DOCS_URL: String = "https://shijianjs.github.io/ktjs-dom-kit/"

/** Source repository, linked from the navbar's link menu. */
const val SOURCE_URL: String = "https://github.com/shijianjs/ktjs-dom-kit"

private val OVERVIEW: Group = Group("overview", "lucide:home")

private val DOM: Group = Group("dom", "lucide:layers")

private val COMPONENTS: Group = Group("components", "lucide:package")

/**
 * The site: one entry per page. Adding a page means adding one entry here and its titles in
 * [siteTexts] — the sidebar, the breadcrumb, the router and the page cache all derive from this list.
 *
 * The icon is an Iconify name drawn in the sidebar row; the group's icon is drawn on the collapsible
 * parent row. Pages without a natural icon simply omit it.
 */
fun siteCatalog(): Catalog = Catalog(
    listOf(
        Entry("overview", OVERVIEW, "lucide:sparkles") { OverviewPage() },
        Entry("elements", DOM, "lucide:code-xml") { ElementsPage() },
        Entry("attributes", DOM, "lucide:tags") { AttributePage() },
        Entry("stylesheets", DOM, "lucide:paintbrush") { StylingPage() },
        Entry("custom-elements", COMPONENTS, "lucide:puzzle") { WebComponentPage() },
    ),
)
