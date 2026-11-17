package io.github.shijianjs.ktjs.demo

import io.github.shijianjs.ktjs.dom.element.createElement
import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementAdapter
import io.github.shijianjs.ktjs.dom.webcomponent.attachStyledShadow
import io.github.shijianjs.ktjs.dom.webcomponent.defineIfMissing
import io.github.shijianjs.ktjs.iconify.IconifyIconElement
import io.github.shijianjs.ktjs.iconify.createIconifyIcon
import io.github.shijianjs.ktjs.vaadin.applayout.AppLayoutPrimarySections
import io.github.shijianjs.ktjs.vaadin.applayout.createAppLayout
import io.github.shijianjs.ktjs.vaadin.applayout.createDrawerToggle
import io.github.shijianjs.ktjs.vaadin.breadcrumbs.BreadcrumbsItemElement
import io.github.shijianjs.ktjs.vaadin.breadcrumbs.createBreadcrumbs
import io.github.shijianjs.ktjs.vaadin.breadcrumbs.createBreadcrumbsItem
import io.github.shijianjs.ktjs.vaadin.button.ButtonElement
import io.github.shijianjs.ktjs.vaadin.button.createButton
import io.github.shijianjs.ktjs.vaadin.horizontallayout.HorizontalLayoutElement
import io.github.shijianjs.ktjs.vaadin.horizontallayout.createHorizontalLayout
import io.github.shijianjs.ktjs.vaadin.lumostyles.registerLumoIcons
import io.github.shijianjs.ktjs.vaadin.lumoutility.LumoUtility
import io.github.shijianjs.ktjs.vaadin.sidenav.SideNavItemElement
import io.github.shijianjs.ktjs.vaadin.sidenav.createSideNav
import io.github.shijianjs.ktjs.vaadin.sidenav.createSideNavItem
import io.github.shijianjs.ktjs.vaadin.verticallayout.VerticalLayoutElement
import io.github.shijianjs.ktjs.vaadin.verticallayout.createVerticalLayout
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
    /** Shown next to the title in the navbar, e.g. `Kotlin/JS 2.4.21`; empty hides the line. */
    val siteVersion: String = "",
    /** Dokka entry point of the wrapper this demo belongs to, when the site has one. */
    val apiUrl: String? = null,
)

/**
 * `<ktjs-demo-shell>`: the site frame, built from Vaadin's app layout.
 *
 * The layout owns the geometry that a hand-rolled shell always gets wrong: the navbar spans the whole
 * width on top, the drawer sits under it and scrolls on its own, and the content column scrolls inside
 * the layout — so the page never grows a second scrollbar next to the navigation, and no panel has to
 * fake a sticky position. The drawer width and both bar backgrounds are app-layout custom properties
 * set once in [SHELL_SHEET].
 *
 * Navigation swaps the single child of the content column and keeps the built pages cached, so state
 * inside a page (typed input, selected rows, opened panels) survives a trip to another page and back.
 * A language switch resolves one [SiteTexts] per language and walks the mounted pages through
 * [Page.applyTranslation] — it does not drop the cache.
 *
 * Configuration is handed over in [mount] rather than through a constructor argument, because the
 * browser creates custom elements itself; the element is configured while detached, before it is
 * appended.
 */
class DemoShell : CustomElementAdapter() {
    private val appLayout = createAppLayout()

    private val drawerToggle = createDrawerToggle { ariaLabelProperty = "Toggle navigation" }

    /**
     * The site title, in the navbar the way every app-layout example puts it: an `h1` that is a direct
     * child of the navbar, not a box stacked back into the drawer.
     */
    private val siteTitle: HTMLElement = HtmlTagName.h1.createElement { classList.add(SHELL_TITLE) }

    private val titleVersion: HTMLElement = HtmlTagName.span.createElement { classList.add(SHELL_VERSION) }

    /** Left half of the navbar: toggle, title, version and breadcrumbs, centred on one line. */
    private val navbarStart: HorizontalLayoutElement = createHorizontalLayout {
        withTheme("spacing")
        classList.add(SHELL_TOPBAR_START, LumoUtility.AlignItems.CENTER)
    }

    private val navbar: HorizontalLayoutElement = createHorizontalLayout {
        withTheme("spacing")
        classList.add(SHELL_TOPBAR, LumoUtility.AlignItems.CENTER, LumoUtility.JustifyContent.BETWEEN)
        setAttribute("slot", "navbar")
    }

    private val breadcrumbs = createBreadcrumbs()

    private val crumbGroup: BreadcrumbsItemElement = createBreadcrumbsItem()

    private val crumbPage: BreadcrumbsItemElement = createBreadcrumbsItem()

    private val actions: HorizontalLayoutElement = createHorizontalLayout {
        withTheme("spacing")
        classList.add(SHELL_ACTIONS, LumoUtility.AlignItems.CENTER)
    }

    /**
     * The whole left column is one `vaadin-side-nav`, the component Vaadin builds for a drawer:
     * groups are parent items (no `path`, so a click only expands them) and pages are their
     * `slot="children"` items.
     */
    private val sideNav = createSideNav()

    private val content: VerticalLayoutElement = createVerticalLayout { withTheme("padding", "spacing") }

    private lateinit var catalog: Catalog

    private lateinit var language: LanguageState

    private lateinit var theme: Theme

    private lateinit var chrome: Chrome

    private lateinit var texts: (Lang) -> SiteTexts

    private lateinit var router: Router

    private val navItems: MutableMap<String, SideNavItemElement> = mutableMapOf()

    private val navLabels: MutableMap<String, HTMLElement> = mutableMapOf()

    private val groupLabels: MutableMap<String, HTMLElement> = mutableMapOf()

    private val languageButtons: MutableMap<Lang, ButtonElement> = mutableMapOf()

    private val pages: MutableMap<String, Page> = mutableMapOf()

    private var shownRoute: String? = null

    private val apiLink: HTMLElement = HtmlTagName.a.createElement { classList.add(SHELL_LINK) }

    private val lightButton: ButtonElement = createButton { classList.add(SHELL_ACTION) }

    private val darkButton: ButtonElement = createButton { classList.add(SHELL_ACTION) }

    init {
        appLayout.primarySection = AppLayoutPrimarySections.navbar

        sideNav.setAttribute("slot", "drawer")

        breadcrumbs.append(crumbGroup, crumbPage)
        navbarStart.append(drawerToggle, siteTitle, titleVersion, breadcrumbs)
        navbar.append(navbarStart, actions)

        appLayout.append(navbar, sideNav, content)
        attachStyledShadow(RESET, SHELL_SHEET).append(appLayout)
    }

    /**
     * Builds the drawer, the navbar and the router, then shows the route already in the URL.
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

        titleVersion.textContent = chrome.siteVersion
        titleVersion.setHidden(chrome.siteVersion.isEmpty())

        buildGroups()
        buildActions()

        language.onChange { applyLanguage() }

        val defaultRoute = catalog.entries.firstOrNull()?.id ?: "overview"
        router = Router(defaultRoute) { show(it) }
        appLayout.drawerOpened = true
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
            // A group is a parent item without a path: Vaadin renders it as a collapsible section, and
            // a click toggles the children instead of navigating. It starts expanded so every page
            // stays one click away, the same as the old always-visible group heading.
            val groupItem = createSideNavItem { expanded = true }
            val groupLabel = HtmlTagName.span.createElement()
            groupLabel.textContent = current.groupTitles[group.id] ?: group.id
            group.icon?.let { groupItem.append(navIcon(it)) }
            groupItem.append(groupLabel)

            catalog.entriesOf(group).forEach { entry ->
                // No `path`: the component matches `location.pathname`, which cannot see a `#/…` hash,
                // and a `#/…` path would make *every* item look current. The row is therefore a plain
                // item that the frame turns into a link and highlights itself.
                val item = createSideNavItem()
                val label = HtmlTagName.span.createElement { textContent = titleOf(entry, language.current) }
                entry.icon?.let { item.append(navIcon(it)) }
                item.append(label)
                item.dataset["route"] = entry.id
                item.setAttribute("slot", "children")
                item.setAttribute("role", "link")
                item.tabIndex = 0
                item.clickEvent.addHandler { navigate(entry.id) }
                navItems[entry.id] = item
                navLabels[entry.id] = label
                groupItem.append(item)
            }

            groupLabels[group.id] = groupLabel
            sideNav.append(groupItem)
        }
    }

    /**
     * A sidebar icon: `<iconify-icon>` in the item's `prefix` slot.
     *
     * The icon name is an Iconify id (`lucide:layers`), so the site carries no SVG of its own; the size
     * is [ICON_SIZE] via the frame's sheet and never a `width`/`height` attribute (the element renders
     * an `<svg width="1em">`, which those attributes would only stretch).
     */
    private fun navIcon(name: String): IconifyIconElement =
        createIconifyIcon(name) {
            setAttribute("slot", "prefix")
            classList.add(SHELL_NAV_ICON)
            setAttribute("aria-hidden", "true")
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
            val button = createButton {
                textContent = textsFor(lang).langLabel
                classList.add(SHELL_ACTION)
            }
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
        setActive(lightButton, !theme.isDark)
        setActive(darkButton, theme.isDark)
    }

    private fun syncLanguageButtons() {
        languageButtons.forEach { (lang, button) -> setActive(button, language.current == lang) }
    }

    /**
     * Marks the selected variant of a toggle with Lumo's `primary` theme; the other one carries no
     * theme at all.
     *
     * `primary` and the default variant have the same box (padding and min-width); `tertiary`, which
     * this used to switch to, drops `min-width` and halves the padding, so every toggle resized the
     * action row. The fixed width in [SHELL_ACTION] then keeps even a longer label from moving the
     * navbar's layout.
     */
    private fun setActive(button: ButtonElement, active: Boolean) {
        if (active) button.withTheme("primary") else button.withTheme()
    }

    private fun show(route: String) {
        val entry = catalog.entry(route) ?: catalog.entries.firstOrNull() ?: return
        val page = pages.getOrPut(entry.id) {
            entry.create().also { it.applyTranslation(language.current) }
        }

        if (shownRoute != entry.id) {
            shownRoute = entry.id
            // Vaadin paints the item carrying its `current` state attribute; hash routes are invisible
            // to the component's own URL matching (it compares `location.pathname`), so the frame sets
            // that state on every route change and the component does the highlighting.
            navItems.forEach { (id, item) ->
                if (id == entry.id) item.setAttribute("current", "") else item.removeAttribute("current")
            }
            document.title = "${titleOf(entry, language.current)} · ${textsFor(language.current).siteTitle}"
        }
        setBreadcrumb(entry)

        if (content.firstElementChild !== page.node) {
            content.replaceChildren(page.node)
        }
    }

    private fun setBreadcrumb(entry: Entry) {
        val current = textsFor(language.current)
        crumbGroup.textContent = current.groupTitles[entry.group.id] ?: entry.group.id
        // The group is a real destination: the first page of that group. The page itself is the
        // current item, rendered without a path so breadcrumbs marks it `aria-current="page"`.
        catalog.entriesOf(entry.group).firstOrNull()?.let { crumbGroup.path = "#/${it.id}" }
        crumbPage.textContent = titleOf(entry, language.current)
        crumbPage.removeAttribute("path")
    }

    private fun applyLanguage() {
        val current = textsFor(language.current)
        siteTitle.textContent = current.siteTitle

        catalog.groups.forEach { group ->
            groupLabels[group.id]?.textContent = current.groupTitles[group.id] ?: group.id
        }
        catalog.entries.forEach { entry ->
            navLabels[entry.id]?.textContent = titleOf(entry, language.current)
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
