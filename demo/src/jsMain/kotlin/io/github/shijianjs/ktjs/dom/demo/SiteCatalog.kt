package io.github.shijianjs.ktjs.dom.demo

import io.github.shijianjs.ktjs.demo.Catalog
import io.github.shijianjs.ktjs.demo.Entry
import io.github.shijianjs.ktjs.demo.Group
import io.github.shijianjs.ktjs.demo.bilingual
import io.github.shijianjs.ktjs.dom.demo.page.attribute.AttributePage
import io.github.shijianjs.ktjs.dom.demo.page.elements.ElementsPage
import io.github.shijianjs.ktjs.dom.demo.page.overview.OverviewPage
import io.github.shijianjs.ktjs.dom.demo.page.styling.StylingPage
import io.github.shijianjs.ktjs.dom.demo.page.webcomponent.WebComponentPage

/** Product name shown in the shell brand. */
const val SITE_TITLE: String = "ktjs-dom-kit"

/** Version the page reports; keep it the module's `version`. */
const val KIT_VERSION: String = "0.1.0"

/** Dokka entry point of the API reference, deployed next to the docs site. */
const val API_URL: String = "https://shijianjs.github.io/ktjs-dom-kit/api/"

private val OVERVIEW: Group = Group("overview", bilingual("开始", "Getting started"))

private val DOM: Group = Group("dom", bilingual("DOM", "DOM"))

private val COMPONENTS: Group = Group("components", bilingual("自定义元素", "Custom elements"))

/**
 * The site: one entry per page. Adding a page means adding one entry here — the sidebar, the
 * breadcrumb, the router and the page cache all derive from this list.
 */
fun siteCatalog(): Catalog = Catalog(
    listOf(
        Entry("overview", OVERVIEW, bilingual("概览", "Overview")) { OverviewPage() },
        Entry("elements", DOM, bilingual("元素创建", "Elements")) { ElementsPage() },
        Entry("attributes", DOM, bilingual("属性绑定", "Attributes")) { AttributePage() },
        Entry("stylesheets", DOM, bilingual("样式表", "Stylesheets")) { StylingPage() },
        Entry("custom-elements", COMPONENTS, bilingual("自定义元素", "Custom elements")) { WebComponentPage() },
    ),
)
