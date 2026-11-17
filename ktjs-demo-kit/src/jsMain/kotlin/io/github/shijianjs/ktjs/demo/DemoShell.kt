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
import web.dom.TagName
import web.dom.clickEvent
import web.dom.document
import web.events.addHandler
import web.html.HTMLElement
import web.html.HtmlTagName

/**
 * Identity of the site that is not a translated string.
 *
 * The site title itself lives in [SiteTexts], because it changes with the language; the version tag
 * and the API reference entry point do not.
 */
class Chrome(
    /** Shown under the title in the sidebar, e.g. `Kotlin/JS 2.4.21`; empty hides the line. */
    val siteVersion: String = "",
    /** Dokka entry point of the wrapper this demo belongs to, when the site has one. */
    val apiUrl: String? = null,
)

/**
 * `<ktjs-demo-shell>`: the whole site frame — a sidebar built once from the catalog, a top bar with
 * the breadcrumb, the API link, the language switch and the light/dark switch, and the content area
 * that hosts the current page.
 *
 * The shell never rebuilds structure. Navigation swaps the single child of the content area and keeps
 * the built pages cached, so state inside a page (typed input, selected rows, opened panels) survives
 * a trip to another page and back. A language switch resolves one [SiteTexts] per language and walks
 * the mounted pages through [Page.applyTranslation] — it does not drop the cache.
 *
 * Configuration is handed over in [mount] rather than through a constructor argument, because the
 * browser creates custom elements itself; the element is configured while detached, before it is
 * appended.
 */
class DemoShell : CustomElementAdapter() {
    private val brandName: HTMLElement = HtmlTagName.span.createElement { classList.add(SHELL_BRAND_NAME) }

    private val brandVersion: HTMLElement = HtmlTagName.span.createElement { classList.add(SHELL_BRAND_VERSION) }

    private val groupHost: HTMLElement = div()

    private val navElement: HTMLElement = HtmlTagName.aside.createElement { classList.add(SHELL_NAV) }

    private val breadcrumb: HTMLElement = HtmlTagName.span.createElement { classList.add(SHELL_BREADCRUMB) }

    private val actions: HTMLElement = div().apply { classList.add(SHELL_ACTIONS) }

    private val contentHost: HTMLElement = div().apply { classList.add(SHELL_CONTENT) }

    private lateinit var catalog: Catalog

    private lateinit var language: LanguageState

    private lateinit var theme: Theme

    private lateinit var chrome: Chrome

    private lateinit var texts: (Lang) -> SiteTexts

    private lateinit var router: Router

    private val navItems: MutableMap<String, SideNavItemElement> = mutableMapOf()

    private val groupTitles: MutableMap<String, HTMLElement> = mutableMapOf()

    private val languageButtons: MutableMap<Lang, ButtonElement> = mutableMapOf()

    private val pages: MutableMap<String, Page> = mutableMapOf()

    private var shownRoute: String? = null

    private val apiLink: HTMLElement = HtmlTagName.a.createElement { classList.add(SHELL_LINK) }

    private val lightButton: ButtonElement = createButton()

    private val darkButton: ButtonElement = createButton()

    init {
        val brand = div().apply { classList.add(SHELL_BRAND) }
        brand.append(brandName, brandVersion)
        navElement.append(brand, groupHost)

        val topbar = HtmlTagName.header.createElement { classList.add(SHELL_TOPBAR) }
        topbar.append(breadcrumb, actions)

        val main = div().apply { classList.add(SHELL_MAIN) }
        main.append(topbar, contentHost)

        attachStyledShadow(RESET, SHELL_SHEET).append(navElement, main)
    }

    /**
     * Builds the sidebar, the top bar and the router, then shows the route already in the URL.
     *
     * [texts] resolves the shell's strings for a language: the application owns one [SiteTexts]
     * instance per language and hands this function over, so the shell itself carries no copy.
     */
    fun mount(
        catalog: Catalog,
        language: LanguageState,
        theme: Theme,
        chrome: Chrome,
        texts: (Lang) -> SiteTexts,
    ) {
        this.catalog = catalog
        this.language = language
        this.theme = theme
        this.chrome = chrome
        this.texts = texts

        registerLumoIcons()

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

    private fun textsFor(lang: Lang): SiteTexts = texts(lang)

    private fun titleOf(entry: Entry, lang: Lang): String = textsFor(lang).pageTitles[entry.id] ?: entry.id

    private fun buildGroups() {
        val current = textsFor(language.current)
        catalog.groups.forEach { group ->
            val title = HtmlTagName.div.createElement { classList.add(SHELL_GROUP_TITLE) }
            title.textContent = current.groupTitles[group.id] ?: group.id

            val list = createSideNav()
            catalog.entriesOf(group).forEach { entry ->
                val item = createSideNavItem { textContent = titleOf(entry, language.current) }
                item.dataset["route"] = entry.id
                item.setAttribute("role", "link")
                item.tabIndex = 0
                item.clickEvent.addHandler { navigate(entry.id) }
                navItems[entry.id] = item
                list.append(item)
            }

            val groupBox = div().apply { classList.add(SHELL_GROUP) }
            groupBox.append(title, list)
            groupTitles[group.id] = title
            groupHost.append(groupBox)
        }
    }

    private fun buildActions() {
        val current = textsFor(language.current)
        val apiUrl = chrome.apiUrl
        if (apiUrl != null) {
            apiLink.textContent = current.apiLabel
            apiLink.setAttribute("href", apiUrl)
            apiLink.setAttribute("rel", "noopener noreferrer")
            actions.append(apiLink)
        }

        // One button per language the site ships, each labelled in the language it switches to: a
        // third language only needs an enum entry and its SiteTexts instance.
        Lang.entries.forEach { lang ->
            val button = createButton { textContent = textsFor(lang).langLabel }
            button.clickEvent.addHandler { language.set(lang) }
            languageButtons[lang] = button
            actions.append(button)
        }

        lightButton.clickEvent.addHandler { applyTheme(false) }
        darkButton.clickEvent.addHandler { applyTheme(true) }
        actions.append(lightButton, darkButton)

        lightButton.textContent = current.lightLabel
        darkButton.textContent = current.darkLabel
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
        languageButtons.forEach { (lang, button) ->
            button.withTheme(if (language.current == lang) "primary" else "tertiary")
        }
    }

    private fun show(route: String) {
        val entry = catalog.entry(route) ?: catalog.entries.firstOrNull() ?: return
        val page = pages.getOrPut(entry.id) {
            entry.create().also { it.applyTranslation(language.current) }
        }

        if (shownRoute != entry.id) {
            shownRoute = entry.id
            navItems.forEach { (id, item) -> item.classList.toggle(NAV_CURRENT, id == entry.id) }
            document.title = "${titleOf(entry, language.current)} · ${textsFor(language.current).siteTitle}"
        }
        setBreadcrumb(entry)

        if (contentHost.firstElementChild !== page.node) {
            contentHost.replaceChildren(page.node)
        }
    }

    private fun setBreadcrumb(entry: Entry) {
        val current = textsFor(language.current)
        val groupTitle = current.groupTitles[entry.group.id] ?: entry.group.id
        breadcrumb.textContent = "$groupTitle / ${titleOf(entry, language.current)}"
    }

    private fun applyLanguage() {
        val current = textsFor(language.current)
        brandName.textContent = current.siteTitle

        catalog.groups.forEach { group ->
            groupTitles[group.id]?.textContent = current.groupTitles[group.id] ?: group.id
        }
        catalog.entries.forEach { entry ->
            navItems[entry.id]?.textContent = titleOf(entry, language.current)
        }

        if (chrome.apiUrl != null) apiLink.textContent = current.apiLabel
        languageButtons.forEach { (lang, button) -> button.textContent = textsFor(lang).langLabel }
        lightButton.textContent = current.lightLabel
        darkButton.textContent = current.darkLabel

        syncThemeButtons()
        syncLanguageButtons()

        pages.values.forEach { it.applyTranslation(language.current) }

        val entry = shownRoute?.let { catalog.entry(it) }
        if (entry != null) {
            setBreadcrumb(entry)
            document.title = "${titleOf(entry, language.current)} · ${current.siteTitle}"
        } else {
            document.title = current.siteTitle
        }
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
