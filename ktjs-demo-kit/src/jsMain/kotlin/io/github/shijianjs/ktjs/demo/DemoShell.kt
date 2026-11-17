package io.github.shijianjs.ktjs.demo

import io.github.shijianjs.ktjs.dom.element.createElement
import io.github.shijianjs.ktjs.dom.element.inlineStyle
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
import io.github.shijianjs.ktjs.vaadin.horizontallayout.HorizontalLayoutElement
import io.github.shijianjs.ktjs.vaadin.horizontallayout.createHorizontalLayout
import io.github.shijianjs.ktjs.vaadin.lumostyles.registerLumoIcons
import io.github.shijianjs.ktjs.vaadin.lumoutility.LumoUtility
import io.github.shijianjs.ktjs.vaadin.menubar.HTMLElementOrString
import io.github.shijianjs.ktjs.vaadin.menubar.MenuBarElement
import io.github.shijianjs.ktjs.vaadin.menubar.MenuBarItem
import io.github.shijianjs.ktjs.vaadin.menubar.SubMenuItem
import io.github.shijianjs.ktjs.vaadin.menubar.createMenuBar
import io.github.shijianjs.ktjs.vaadin.sidenav.SideNavItemElement
import io.github.shijianjs.ktjs.vaadin.sidenav.createSideNav
import io.github.shijianjs.ktjs.vaadin.sidenav.createSideNavItem
import io.github.shijianjs.ktjs.vaadin.verticallayout.VerticalLayoutElement
import io.github.shijianjs.ktjs.vaadin.verticallayout.createVerticalLayout
import js.objects.unsafeJso
import js.reflect.unsafeCast
import web.components.CustomElementReference
import web.cssom.Color
import web.cssom.integer
import web.cssom.px
import web.dom.TagName
import web.dom.clickEvent
import web.dom.document
import web.events.Event
import web.events.EventHandler
import web.events.EventType
import web.events.addHandler
import web.events.addEventListener
import web.html.HTMLElement
import web.html.HtmlTagName
import web.window.WindowTarget
import web.window._blank
import web.window.window

/**
 * The site's identity and destination URLs — everything the frame needs that is **not** a translated
 * string.
 *
 * The site title itself lives in [SiteTexts], because it changes with the language; the version tag
 * and the targets do not. Every URL is optional: a site with only a repository gets a menu with one
 * entry, and a site with none hides the menu entirely. [logoSrc] is a plain image URL, so a site
 * reuses the very file its documentation and README show instead of inventing a second mark.
 */
class ShellConfig(
    /** Shown next to the title in the navbar, e.g. `Kotlin/JS 2.4.21`; empty hides the line. */
    val siteVersion: String = "",
    /** Documentation site, usually the root the demo is published under. */
    val docsUrl: String? = null,
    /** Dokka entry point of the wrapper this demo belongs to. */
    val apiUrl: String? = null,
    /** Source repository. */
    val sourceUrl: String? = null,
    /** Site mark, drawn immediately left of the title; `null` leaves the title to stand alone. */
    val logoSrc: String? = null,
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

    /** Left half of the navbar: toggle, optional mark, title, version and breadcrumbs on one line. */
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
     * Links are a menu, not a row of anchors: the site's own pages, the API reference and the
     * repository are three destinations of the same kind.
     */
    private val docsMenu: MenuBarElement = createMenuBar { withTheme("dropdown-indicators") }

    /** One entry per [Lang]; the label of a language is always that language's own name. */
    private val languageMenu: MenuBarElement = createMenuBar { withTheme("dropdown-indicators") }

    /** Light / dark / follow-the-browser. */
    private val themeMenu: MenuBarElement = createMenuBar { withTheme("dropdown-indicators") }

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

    private lateinit var config: ShellConfig

    private lateinit var texts: Translations<SiteTexts>

    private lateinit var router: Router

    private val navItems: MutableMap<String, SideNavItemElement> = mutableMapOf()

    private val navLabels: MutableMap<String, HTMLElement> = mutableMapOf()

    private val groupLabels: MutableMap<String, HTMLElement> = mutableMapOf()

    private val pages: MutableMap<String, Page> = mutableMapOf()

    private var shownRoute: String? = null

    /**
     * What a menu entry does, keyed by the action id carried on the entry's own element.
     *
     * A `MenuBarItem` is a plain JS object and the menu keeps the very instance it was given, but the
     * *element* is easier to read back from the `item-selected` event: it travels with the item into
     * the overlay, and its `data-action` is also what the end-to-end test clicks.
     */
    private val menuActions: MutableMap<String, () -> Unit> = mutableMapOf()

    init {
        appLayout.primarySection = AppLayoutPrimarySections.navbar

        sideNav.setAttribute("slot", "drawer")

        breadcrumbs.append(crumbGroup, crumbPage)
        navbarStart.append(drawerToggle, siteTitle, titleVersion, breadcrumbs)
        navbar.append(navbarStart, actions)

        docsMenu.dataset["menu"] = "docs"
        languageMenu.dataset["menu"] = "lang"
        themeMenu.dataset["menu"] = "theme"
        attachSelection(docsMenu)
        attachSelection(languageMenu)
        attachSelection(themeMenu)

        appLayout.append(navbar, sideNav, content)
        attachStyledShadow(RESET, SHELL_SHEET).append(appLayout)
    }

    /**
     * Builds the drawer, the navbar and the router, then shows the route already in the URL.
     *
     * [texts] is the application's translation table: one [SiteTexts] per language, declared with
     * `texts(Langs.EN to …, Langs.ZH_HANS to …)`. The shell carries no copy of its own and lists the
     * language menu from [LanguageState.languages], so registering a language is entirely the
     * application's call.
     */
    fun mount(
        catalog: Catalog,
        language: LanguageState,
        theme: Theme,
        config: ShellConfig,
        texts: Translations<SiteTexts>,
    ) {
        this.catalog = catalog
        this.language = language
        this.theme = theme
        this.config = config
        this.texts = texts

        registerLumoIcons()
        buildLogo()

        titleVersion.textContent = config.siteVersion
        titleVersion.setHidden(config.siteVersion.isEmpty())

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

    private fun textsFor(lang: Lang): SiteTexts = texts[lang]

    private fun titleOf(entry: Entry, lang: Lang): String = textsFor(lang).pageTitles[entry.id] ?: entry.id

    /** The mark sits immediately left of the name, so it reads as one lock-up with the title. */
    private fun buildLogo() {
        val src = config.logoSrc ?: return
        val logo = HtmlTagName.img.createElement {
            classList.add(SHELL_LOGO)
            setAttribute("src", src)
            setAttribute("alt", "")
            setAttribute("aria-hidden", "true")
        }
        navbarStart.insertBefore(logo, siteTitle)
    }

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
     * is [SHELL_NAV_ICON] via the frame's sheet and never a `width`/`height` attribute (the element
     * renders an `<svg width="1em">`, which those attributes would only stretch).
     */
    private fun navIcon(name: String): IconifyIconElement =
        createIconifyIcon(name) {
            setAttribute("slot", "prefix")
            classList.add(SHELL_NAV_ICON)
            setAttribute("aria-hidden", "true")
        }

    private fun buildActions() {
        actions.append(docsMenu, languageMenu, themeMenu)
    }

    /**
     * A menu entry's content: a `vaadin-menu-bar-item` holding an Iconify icon and the label; the
     * current entry of a menu has its label emphasised.
     *
     * Everything here is inline on purpose: an entry is teleported into the menu's overlay, which
     * lives in the menu-bar's own shadow tree, so a class declared in [SHELL_SHEET] cannot reach it.
     * The inline declarations travel with the element; this is the documented exception for nodes
     * that leave their owner's tree.
     */
    private fun menuItemNode(action: String, icon: String, label: String, checked: Boolean): HTMLElement =
        TagName<HTMLElement>("vaadin-menu-bar-item").createElement {
            if (action.isNotEmpty()) dataset["action"] = action
            append(menuIcon(icon))
            val text = HtmlTagName.span.createElement { textContent = label }
            if (checked) {
                text.inlineStyle.apply {
                    fontWeight = integer(600)
                    color = Color("var(--lumo-primary-text-color)")
                }
            }
            append(text)
        }

    private fun menuIcon(name: String): IconifyIconElement =
        createIconifyIcon(name) {
            setAttribute("aria-hidden", "true")
            inlineStyle.apply {
                fontSize = 18.px
                marginRight = 8.px
            }
        }

    /**
     * A button of a menu bar; [children] make it a drop-down instead of an action.
     *
     * The button carries the [menu] id as well, so the end-to-end test opens a menu through the element
     * the menu itself renders — the top-level entry *is* our component, there is no internal button
     * element to reach for.
     */
    private fun menuButton(
        menu: String,
        icon: String,
        label: String,
        children: Array<SubMenuItem>,
    ): MenuBarItem =
        unsafeJso<MenuBarItem> {
            component = HTMLElementOrString(
                menuItemNode("", icon, label, false).also { it.dataset["menu"] = menu },
            )
            this.children = children
        }

    private fun menuEntry(
        action: String,
        icon: String,
        label: String,
        checked: Boolean = false,
        onClick: () -> Unit,
    ): SubMenuItem {
        menuActions[action] = onClick
        return unsafeJso<SubMenuItem> { component = HTMLElementOrString(menuItemNode(action, icon, label, checked)) }
    }

    /**
     * Routes a selection to its entry. `item-selected` carries the very item object the menu was built
     * with; the element that travelled into the overlay is reachable through `value.component`, and its
     * `data-action` is what the map above is keyed by.
     */
    private fun attachSelection(menu: MenuBarElement) {
        menu.addEventListener(EventType<Event>("item-selected"), EventHandler { event ->
            val component = event.unsafeCast<dynamic>().detail.value.component
            val action = (component as? HTMLElement)?.dataset?.get("action")
            action?.let { menuActions[it]?.invoke() }
        })
    }

    private fun rebuildMenus() {
        rebuildDocsMenu()
        rebuildLanguageMenu()
        rebuildThemeMenu()
    }

    private fun rebuildDocsMenu() {
        val current = textsFor(language.current)
        val entries = mutableListOf<SubMenuItem>()

        config.docsUrl?.let { url ->
            entries += menuEntry("docs:site", "lucide:book", current.siteDocsLabel) { openLink(url) }
        }
        config.apiUrl?.let { url ->
            entries += menuEntry("docs:api", "lucide:file-code", current.apiLabel) { openLink(url) }
        }
        config.sourceUrl?.let { url ->
            entries += menuEntry("docs:source", "lucide:github", current.sourceLabel) { openLink(url) }
        }

        docsMenu.setHidden(entries.isEmpty())
        if (entries.isEmpty()) return

        docsMenu.items = arrayOf(
            menuButton("docs", "lucide:book-open", current.docsLabel, entries.toTypedArray()),
        )
    }

    private fun rebuildLanguageMenu() {
        val current = textsFor(language.current)
        val entries = language.languages.map { lang ->
            menuEntry("lang:${lang.code}", "lucide:languages", textsFor(lang).langLabel, lang == language.current) {
                language.set(lang)
            }
        }.toTypedArray()
        languageMenu.items = arrayOf(
            menuButton("lang", "lucide:languages", current.languageLabel, entries),
        )
    }

    private fun rebuildThemeMenu() {
        val current = textsFor(language.current)
        val topIcon = when (theme.mode) {
            ThemeMode.LIGHT -> "lucide:sun"
            ThemeMode.DARK -> "lucide:moon"
            ThemeMode.SYSTEM -> "lucide:monitor"
        }
        val entries = arrayOf(
            menuEntry("theme:light", "lucide:sun", current.lightLabel, theme.mode == ThemeMode.LIGHT) {
                applyTheme(ThemeMode.LIGHT)
            },
            menuEntry("theme:dark", "lucide:moon", current.darkLabel, theme.mode == ThemeMode.DARK) {
                applyTheme(ThemeMode.DARK)
            },
            menuEntry("theme:system", "lucide:monitor", current.systemLabel, theme.mode == ThemeMode.SYSTEM) {
                applyTheme(ThemeMode.SYSTEM)
            },
        )
        themeMenu.items = arrayOf(menuButton("theme", topIcon, current.themeLabel, entries))
    }

    private fun applyTheme(mode: ThemeMode) {
        theme.apply(mode)
        rebuildThemeMenu()
    }

    /**
     * Destinations open in a new tab: the demo is a place to try things out, and a reader following a
     * link to the API reference or the repository should not lose their place in it.
     */
    private fun openLink(url: String) {
        window.open(url, WindowTarget._blank)
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

        rebuildMenus()
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
