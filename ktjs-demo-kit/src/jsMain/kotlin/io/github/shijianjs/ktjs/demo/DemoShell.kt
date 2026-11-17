package io.github.shijianjs.ktjs.demo

import io.github.shijianjs.ktjs.dom.element.createElement
import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementAdapter
import io.github.shijianjs.ktjs.dom.webcomponent.attachStyledShadow
import io.github.shijianjs.ktjs.dom.webcomponent.defineIfMissing
import io.github.shijianjs.ktjs.vaadin.button.ButtonElement
import io.github.shijianjs.ktjs.vaadin.button.createButton
import io.github.shijianjs.ktjs.vaadin.lumostyles.registerLumoIcons
import io.github.shijianjs.ktjs.vaadin.sidenav.SideNavItemElement
import io.github.shijianjs.ktjs.vaadin.sidenav.createSideNav
import io.github.shijianjs.ktjs.vaadin.sidenav.createSideNavItem
import web.components.CustomElementReference
import web.cssom.ClassName
import web.dom.TagName
import web.dom.clickEvent
import web.dom.document
import web.events.addHandler
import web.html.ButtonType
import web.html.button
import web.html.HTMLElement
import web.html.HtmlTagName

/** Identity of the site; the shell paints it and the router titles the document with it. */
class Chrome(
    val siteTitle: String,
    /** Shown under the title in the sidebar, e.g. `Kotlin/JS 2.4.21`. */
    val siteVersion: String = "",
    /** Dokka entry point of the wrapper this demo belongs to, when the site has one. */
    val apiUrl: String? = null,
)

private val API_LABEL: Bilingual = bilingual("API 文档", "API reference")

private val LIGHT_LABEL: Bilingual = bilingual("亮色", "Light")

private val DARK_LABEL: Bilingual = bilingual("暗色", "Dark")

private val BRAND_CLASS: ClassName = ClassName("shell__brand")

private val BRAND_NAME_CLASS: ClassName = ClassName("shell__brand-name")

private val BRAND_VERSION_CLASS: ClassName = ClassName("shell__brand-version")

private val NAV_CLASS: ClassName = ClassName("shell__nav")

private val GROUP_CLASS: ClassName = ClassName("shell__group")

private val GROUP_TITLE_CLASS: ClassName = ClassName("shell__group-title")

private val MAIN_CLASS: ClassName = ClassName("shell__main")

private val TOPBAR_CLASS: ClassName = ClassName("shell__topbar")

private val BREADCRUMB_CLASS: ClassName = ClassName("shell__breadcrumb")

private val ACTIONS_CLASS: ClassName = ClassName("shell__actions")

private val CONTENT_CLASS: ClassName = ClassName("shell__content")

private val LINK_CLASS: ClassName = ClassName("shell__link")

private val CURRENT_CLASS: ClassName = ClassName("nav-current")

/**
 * `<ktjs-demo-shell>`: the whole site frame — a sidebar built once from the catalog, a top bar with
 * the breadcrumb, the API link, the language switch and the light/dark switch, and the content area
 * that hosts the current page.
 *
 * The shell never rebuilds structure. Navigation swaps the single child of the content area and
 * keeps the built pages cached, so state inside a page (typed input, selected rows, opened panels)
 * survives a trip to another page and back. A language switch walks the mounted pages through
 * [Page.applyTranslation] and rewrites the chrome in place — it does not drop the cache.
 *
 * Configuration is handed over in [mount] rather than through a constructor argument, because the
 * browser creates custom elements itself; the element is configured while detached, before it is
 * appended.
 */
class DemoShell : CustomElementAdapter() {
    private val brandName: HTMLElement = HtmlTagName.span.createElement { classList.add(BRAND_NAME_CLASS) }

    private val brandVersion: HTMLElement = HtmlTagName.span.createElement { classList.add(BRAND_VERSION_CLASS) }

    private val groupHost: HTMLElement = div()

    private val navElement: HTMLElement = HtmlTagName.aside.createElement { classList.add(NAV_CLASS) }

    private val breadcrumb: HTMLElement = HtmlTagName.span.createElement { classList.add(BREADCRUMB_CLASS) }

    private val actions: HTMLElement = div().apply { classList.add(ACTIONS_CLASS) }

    private val contentHost: HTMLElement = div().apply { classList.add(CONTENT_CLASS) }

    private lateinit var catalog: Catalog

    private lateinit var language: LanguageState

    private lateinit var theme: Theme

    private lateinit var chrome: Chrome

    private lateinit var router: Router

    private val navItems: MutableMap<String, SideNavItemElement> = mutableMapOf()

    private val groupTitles: MutableMap<String, HTMLElement> = mutableMapOf()

    private val pages: MutableMap<String, Page> = mutableMapOf()

    private var shownRoute: String? = null

    private val apiLink: HTMLElement = HtmlTagName.a.createElement { classList.add(LINK_CLASS) }

    private val zhButton: ButtonElement = createButton { textContent = Lang.ZH.label }

    private val enButton: ButtonElement = createButton { textContent = Lang.EN.label }

    private val lightButton: ButtonElement = createButton()

    private val darkButton: ButtonElement = createButton()

    init {
        val brand = div().apply { classList.add(BRAND_CLASS) }
        brand.append(brandName, brandVersion)
        navElement.append(brand, groupHost)

        val topbar = HtmlTagName.header.createElement { classList.add(TOPBAR_CLASS) }
        topbar.append(breadcrumb, actions)

        val main = div().apply { classList.add(MAIN_CLASS) }
        main.append(topbar, contentHost)

        attachStyledShadow(RESET, SHELL_SHEET).append(navElement, main)
    }

    /**
     * Builds the sidebar, the top bar and the router, then shows the route already in the URL.
     *
     * Call this while the element is still detached; it reads [catalog] and starts listening.
     */
    fun mount(catalog: Catalog, language: LanguageState, theme: Theme, chrome: Chrome) {
        this.catalog = catalog
        this.language = language
        this.theme = theme
        this.chrome = chrome

        registerLumoIcons()

        brandName.textContent = chrome.siteTitle
        brandVersion.textContent = chrome.siteVersion
        brandVersion.setHidden(chrome.siteVersion.isEmpty())

        buildGroups()
        buildActions()

        language.onChange { applyLanguage() }

        val defaultRoute = catalog.entries.firstOrNull()?.id ?: "overview"
        router = Router(defaultRoute) { show(it) }
        applyLanguage()
        router.start()
    }

    /** Navigates to [route]. */
    fun navigate(route: String) {
        router.navigate(route)
    }

    private fun buildGroups() {
        catalog.groups.forEach { group ->
            val title = HtmlTagName.button.createElement { classList.add(GROUP_TITLE_CLASS) }
            title.type = ButtonType.Companion.button
            title.textContent = group.title.resolve(language.current)

            val list = createSideNav()
            catalog.entriesOf(group).forEach { entry ->
                val item = createSideNavItem { textContent = entry.title.resolve(language.current) }
                item.dataset["route"] = entry.id
                item.setAttribute("role", "link")
                item.tabIndex = 0
                item.clickEvent.addHandler { navigate(entry.id) }
                navItems[entry.id] = item
                list.append(item)
            }

            val groupBox = div().apply { classList.add(GROUP_CLASS) }
            groupBox.append(title, list)
            groupTitles[group.id] = title
            groupHost.append(groupBox)
        }
    }

    private fun buildActions() {
        val apiUrl = chrome.apiUrl
        if (apiUrl != null) {
            apiLink.textContent = API_LABEL.resolve(language.current)
            apiLink.setAttribute("href", apiUrl)
            apiLink.setAttribute("rel", "noopener noreferrer")
            actions.append(apiLink)
        }

        zhButton.clickEvent.addHandler { language.set(Lang.ZH) }
        enButton.clickEvent.addHandler { language.set(Lang.EN) }
        lightButton.clickEvent.addHandler { applyTheme(false) }
        darkButton.clickEvent.addHandler { applyTheme(true) }
        actions.append(zhButton, enButton, lightButton, darkButton)

        lightButton.textContent = LIGHT_LABEL.resolve(language.current)
        darkButton.textContent = DARK_LABEL.resolve(language.current)
        syncThemeButtons()
        syncLanguageButtons()
    }

    private fun applyTheme(dark: Boolean) {
        theme.apply(dark)
        syncThemeButtons()
    }

    private fun syncThemeButtons() {
        lightButton.withTheme(if (theme.isDark) "tertiary" else "primary")
        darkButton.withTheme(if (theme.isDark) "primary" else "tertiary")
    }

    private fun syncLanguageButtons() {
        zhButton.withTheme(if (language.current == Lang.ZH) "primary" else "tertiary")
        enButton.withTheme(if (language.current == Lang.EN) "primary" else "tertiary")
    }

    private fun show(route: String) {
        val entry = catalog.entry(route) ?: catalog.entries.firstOrNull() ?: return
        val page = pages.getOrPut(entry.id) {
            entry.create().also { it.applyTranslation(language.current) }
        }

        if (shownRoute != entry.id) {
            shownRoute = entry.id
            navItems.forEach { (id, item) -> item.classList.toggle(CURRENT_CLASS, id == entry.id) }
            document.title = "${entry.title.resolve(language.current)} · ${chrome.siteTitle}"
        }
        setBreadcrumb(entry)

        if (contentHost.firstElementChild !== page.node) {
            contentHost.replaceChildren(page.node)
        }
    }

    private fun setBreadcrumb(entry: Entry) {
        breadcrumb.textContent =
            "${entry.group.title.resolve(language.current)} / ${entry.title.resolve(language.current)}"
    }

    private fun applyLanguage() {
        catalog.groups.forEach { group -> groupTitles[group.id]?.textContent = group.title.resolve(language.current) }
        catalog.entries.forEach { entry ->
            navItems[entry.id]?.textContent = entry.title.resolve(language.current)
        }
        if (chrome.apiUrl != null) apiLink.textContent = API_LABEL.resolve(language.current)
        lightButton.textContent = LIGHT_LABEL.resolve(language.current)
        darkButton.textContent = DARK_LABEL.resolve(language.current)
        syncThemeButtons()
        syncLanguageButtons()

        pages.values.forEach { it.applyTranslation(language.current) }
        shownRoute?.let { route -> catalog.entry(route)?.let { setBreadcrumb(it) } }
    }

    companion object : CustomElementReference<DemoShell>(DemoShell::class.js) {
        val NAME: TagName<DemoShell> = TagName("ktjs-demo-shell")

        fun define() {
            defineIfMissing(NAME, this)
        }
    }
}

/** Creates a detached shell; configure it with [DemoShell.mount] and then append it to the body. */
fun newShell(): DemoShell {
    DemoShell.define()
    return DemoShell.NAME.createElement()
}
