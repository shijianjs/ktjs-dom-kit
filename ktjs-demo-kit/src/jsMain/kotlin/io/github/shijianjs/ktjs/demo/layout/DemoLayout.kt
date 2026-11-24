package io.github.shijianjs.ktjs.demo.layout

import io.github.shijianjs.ktjs.demo.style.Palette
import io.github.shijianjs.ktjs.demo.style.RESET
import io.github.shijianjs.ktjs.demo.style.Theme
import io.github.shijianjs.ktjs.demo.style.ThemeMode
import io.github.shijianjs.ktjs.vaadin.themablemixin.addTheme
import io.github.shijianjs.ktjs.vaadin.themablemixin.setTheme
import io.github.shijianjs.ktjs.vaadin.horizontallayout.HorizontalLayoutVariants
import io.github.shijianjs.ktjs.vaadin.menubar.MenuBarVariants
import io.github.shijianjs.ktjs.vaadin.verticallayout.VerticalLayoutVariants
import io.github.shijianjs.ktjs.demo.i18n.Lang
import io.github.shijianjs.ktjs.demo.i18n.LanguageState
import io.github.shijianjs.ktjs.demo.i18n.SiteTexts
import io.github.shijianjs.ktjs.demo.i18n.Translations
import io.github.shijianjs.ktjs.demo.i18n.code
import io.github.shijianjs.ktjs.demo.nav.DemoMenu
import io.github.shijianjs.ktjs.demo.nav.Entry
import io.github.shijianjs.ktjs.demo.nav.Page
import io.github.shijianjs.ktjs.demo.nav.Router
import io.github.shijianjs.ktjs.dom.element.invoke
import io.github.shijianjs.ktjs.dom.element.unaryPlus
import io.github.shijianjs.ktjs.dom.element.inlineStyle
import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementAdapter
import io.github.shijianjs.ktjs.dom.webcomponent.attachStyledShadow
import io.github.shijianjs.ktjs.iconify.IconifyIconElement
import io.github.shijianjs.ktjs.iconify.createIconifyIcon
import io.github.shijianjs.ktjs.vaadin.applayout.AppLayoutPrimarySections
import io.github.shijianjs.ktjs.vaadin.applayout.createAppLayout
import io.github.shijianjs.ktjs.vaadin.applayout.createDrawerToggle
import io.github.shijianjs.ktjs.vaadin.breadcrumbs.BreadcrumbsItemElement
import io.github.shijianjs.ktjs.vaadin.breadcrumbs.createBreadcrumbs
import io.github.shijianjs.ktjs.vaadin.breadcrumbs.createBreadcrumbsItem
import io.github.shijianjs.ktjs.vaadin.button.ButtonVariants
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
import web.cssom.Color
import web.cssom.integer
import web.cssom.px
import web.dom.clickEvent
import web.dom.document
import web.dom.TagName
import web.events.Event
import web.events.EventHandler
import web.events.EventType
import web.events.addHandler
import web.events.addEventListener
import web.html.HTMLAnchorElement
import web.html.HTMLElement
import web.html.HtmlTagName
import web.window.WindowTarget
import web.window._blank
import web.window.window

/**
 * `<ktjs-demo-layout>`: the site frame, built from Vaadin's app layout.
 *
 * The layout owns the geometry that a hand-rolled layout always gets wrong: the navbar spans the whole
 * width on top, the drawer sits under it and scrolls on its own, and the content column scrolls inside
 * the layout — so the page never grows a second scrollbar next to the navigation, and no panel has to
 * fake a sticky position. What the three regions *are* — a card on the page's canvas, the gutter around
 * it, the gap to its neighbour, the width of the drawer — is [LAYOUT_SHEET]'s decision, not a style this
 * class puts on an element.
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
class DemoLayout : CustomElementAdapter() {
    private val appLayout = createAppLayout()

    /**
     * The drawer toggle, styled as an icon button: Lumo's default button padding leaves ~10px of blank
     * box on each side of a 24px glyph, which reads as a gap between the toggle and the wordmark rather
     * than as part of it. `theme="icon"` is the button's own variant for exactly one glyph, and
     * [LAYOUT_TOGGLE] removes the box the variant still keeps around it.
     */
    private val drawerToggle = createDrawerToggle {
        ariaLabelProperty = "Toggle navigation"
        addTheme(ButtonVariants.icon)
        classList.add(LAYOUT_TOGGLE)
    }

    /**
     * The site title: an `h1` in the navbar the way every app-layout example puts it, not a box
     * stacked back into the drawer.
     */
    private val siteTitle: HTMLElement = HtmlTagName.h1 { classList.add(LAYOUT_TITLE) }

    /**
     * The mark and the title together are the site's home link.
     *
     * A real `<a href="#/">` rather than a click handler on the `h1`: the browser keeps link behaviour
     * (middle-click, "copy link address", keyboard activation), and the fragment change is exactly what
     * [Router] listens for. `#/` parses to the router's default route, so the link means "home" for any
     * site without knowing which page that is.
     */
    private val siteLink: HTMLAnchorElement = HtmlTagName.a {
        classList.add(LAYOUT_TITLE_LINK)
        setAttribute("href", "#/")
        append(siteTitle)
    }

    private val titleVersion: HTMLElement = HtmlTagName.span { classList.add(LAYOUT_VERSION) }

    /** Left half of the navbar: toggle, optional mark, title, version and breadcrumbs on one line. */
    private val navbarStart: HorizontalLayoutElement = createHorizontalLayout {
        addTheme(HorizontalLayoutVariants.spacing)
        classList.add(LAYOUT_TOPBAR_START, LumoUtility.AlignItems.CENTER)
    }

    /**
     * The navbar's card: the bar keeps no surface of its own and only pads this row, so the row is the
     * white, rounded surface the reader sees, inset by the gutter ([LAYOUT_SHEET]).
     */
    private val navbar: HorizontalLayoutElement = createHorizontalLayout {
        addTheme(HorizontalLayoutVariants.spacing)
        classList.add(LAYOUT_TOPBAR, LAYOUT_CARD, LumoUtility.AlignItems.CENTER, LumoUtility.JustifyContent.BETWEEN)
        setAttribute("slot", "navbar")
    }

    private val breadcrumbs = createBreadcrumbs()

    private val crumbGroup: BreadcrumbsItemElement = createBreadcrumbsItem()

    private val crumbPage: BreadcrumbsItemElement = createBreadcrumbsItem()

    private val actions: HorizontalLayoutElement = createHorizontalLayout {
        addTheme(HorizontalLayoutVariants.spacing)
        classList.add(LAYOUT_ACTIONS, LumoUtility.AlignItems.CENTER)
    }

    /**
     * Links are a menu, not a row of anchors: the site's own pages, the API reference and the
     * repository are three destinations of the same kind.
     */
    private val docsMenu: MenuBarElement = createMenuBar { addTheme(MenuBarVariants.dropdownIndicators) }

    /** One entry per [Lang]; the label of a language is always that language's own name. */
    private val languageMenu: MenuBarElement = createMenuBar { addTheme(MenuBarVariants.dropdownIndicators) }

    /** Light / dark / follow-the-browser. */
    private val themeMenu: MenuBarElement = createMenuBar { addTheme(MenuBarVariants.dropdownIndicators) }

    /**
     * The whole left column is one `vaadin-side-nav`, the component Vaadin builds for a drawer:
     * groups are parent items (no `path`, so a click only expands them) and pages are their
     * `slot="children"` items.
     *
     * It carries the drawer's card class as well: the column itself stays transparent, so the nav *is*
     * the surface the reader sees, inset by the column's padding and scrolling inside it.
     */
    private val sideNav = createSideNav().apply { classList.add(LAYOUT_CARD, LAYOUT_DRAWER) }

    /**
     * The content column: the page's surface. Like the drawer it is the card, and the column around it
     * is only the gutter — the app layout supplies the scrolling *box*, the card supplies the sheet the
     * reader sees ([LAYOUT_SHEET]).
     */
    private val content: VerticalLayoutElement = createVerticalLayout {
        setTheme(VerticalLayoutVariants.padding, VerticalLayoutVariants.spacing)
        classList.add(LAYOUT_CARD, LAYOUT_CONTENT)
    }

    private lateinit var menu: DemoMenu

    private lateinit var language: LanguageState

    private lateinit var theme: Theme

    private lateinit var config: LayoutConfig

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
        navbarStart.append(drawerToggle, siteLink, titleVersion, breadcrumbs)
        navbar.append(navbarStart, actions)

        docsMenu.dataset["menu"] = "docs"
        languageMenu.dataset["menu"] = "lang"
        themeMenu.dataset["menu"] = "theme"
        attachSelection(docsMenu)
        attachSelection(languageMenu)
        attachSelection(themeMenu)

        appLayout.append(navbar, sideNav, content)
        attachStyledShadow(RESET, LAYOUT_SHEET).append(appLayout)
    }

    /**
     * Builds the drawer, the navbar and the router, then shows the route already in the URL.
     *
     * The single argument is a [LayoutConfig], so this method stays one line no matter how many knobs
     * the layout grows. `texts` inside it is the application's translation table: one [SiteTexts] per
     * language, declared with `texts(Langs.EN to …, Langs.ZH_HANS to …)`. The layout carries no copy of
     * its own and lists the language menu from [LanguageState.languages], so registering a language is
     * entirely the application's call.
     */
    fun mount(config: LayoutConfig) {
        this.menu = config.menu
        this.language = config.language
        this.theme = config.theme
        this.config = config
        this.texts = config.texts

        registerLumoIcons()
        buildLogo()

        // The shell fills the viewport, so the document must not keep the default 8px body margin:
        // those 8px push the shell past the viewport and the page grows a second scrollbar next to
        // the app layout's own content scroller. A document-level reset, not a component style - the
        // shell is the thing that owns the document it fills.
        document.body.style.margin = "0"

        titleVersion.textContent = config.siteVersion
        titleVersion.toggleAttribute("hidden", config.siteVersion.isEmpty())

        buildGroups()
        buildActions()

        language.onChange { applyLanguage() }

        val defaultRoute = menu.entries.firstOrNull()?.id ?: "overview"
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

    /** The mark sits immediately left of the name inside the home link, so it reads as one lock-up. */
    private fun buildLogo() {
        val src = config.logoSrc ?: return
        val logo = HtmlTagName.img {
            classList.add(LAYOUT_LOGO)
            setAttribute("src", src)
            setAttribute("alt", "")
            setAttribute("aria-hidden", "true")
        }
        siteLink.insertBefore(logo, siteTitle)
    }

    private fun buildGroups() {
        val current = textsFor(language.current)
        menu.groups.forEach { group ->
            val entries = menu.entriesOf(group)

            // A group with one page has nothing to fold: the parent row would exist only to be opened
            // again, and it would carry the same mark as the page under it. The page itself moves up
            // to the top level and keeps the group's mark, so the drawer loses a level without losing
            // the section its icon identifies.
            if (entries.size == 1) {
                sideNav.append(pageRow(entries.single(), nested = false, icon = group.icon))
                return@forEach
            }

            // A group is a parent item without a path: Vaadin renders it as a collapsible section, and
            // a click toggles the children instead of navigating. It starts expanded so every page
            // stays one click away, the same as the old always-visible group heading.
            val groupItem = createSideNavItem { expanded = true }
            val groupLabel = HtmlTagName.span()
            groupLabel.textContent = current.groupTitles[group.id] ?: group.id
            group.icon?.let { groupItem.append(navIcon(it)) }
            groupItem.append(groupLabel)

            entries.forEach { entry -> groupItem.append(pageRow(entry, nested = true)) }

            groupLabels[group.id] = groupLabel
            sideNav.append(groupItem)
        }
    }

    /**
     * One page row of the drawer.
     *
     * No `path`: the component matches `location.pathname`, which cannot see a `#/…` hash, and a
     * `#/…` path would make *every* item look current. The row is therefore a plain item that the frame
     * turns into a link and highlights itself.
     *
     * [nested] decides the slot. A group's children have to declare `slot=children` to be projected
     * into the group's collapsible body; a top-level row must not, or it would disappear into the
     * previous group.
     *
     * [nested] also decides the mark: children of a group carry no icon. The group row above them already
     * names the section, and repeating the same mark on every child would say nothing new — only top-level
     * rows (and the pages a one-page group lifts up) are marked.
     */
    private fun pageRow(entry: Entry, nested: Boolean, icon: String? = null): SideNavItemElement {
        val item = createSideNavItem()
        val label = HtmlTagName.span { textContent = titleOf(entry, language.current) }
        (icon ?: entry.icon.takeIf { !nested })?.let { item.append(navIcon(it)) }
        item.append(label)
        item.dataset["route"] = entry.id
        if (nested) item.setAttribute("slot", "children")
        item.setAttribute("role", "link")
        item.tabIndex = 0
        item.clickEvent.addHandler { navigate(entry.id) }
        navItems[entry.id] = item
        navLabels[entry.id] = label
        return item
    }

    /**
     * A sidebar icon: `<iconify-icon>` in the item's `prefix` slot.
     *
     * The icon name is an Iconify id (`lucide:layers`), so the site carries no SVG of its own; the size
     * is [LAYOUT_NAV_ICON] via the frame's sheet and never a `width`/`height` attribute (the element
     * renders an `<svg width="1em">`, which those attributes would only stretch).
     *
     * `noobserver` is not about size, it is what keeps the glyph alive: `iconify-icon` drops its own
     * `<svg>` as soon as an IntersectionObserver reports the element as not visible, yet leaves
     * `status === "rendered"` behind. For a dropdown entry that happens every time the menu closes —
     * the entry is teleported into the overlay and hidden. On the next open the observer reports
     * "visible again", but the element reset `_visible` to `true` when it disconnected, so nothing
     * sees a change and nothing re-renders: the icon is gone from the second open on. The same trap
     * applies to sidebar icons inside a collapsed group. `noobserver` is iconify-icon's own opt-out;
     * every icon in this layout lives as long as its owner, so it should stay rendered.
     */
    private fun navIcon(name: String): IconifyIconElement =
        createIconifyIcon(name) {
            setAttribute("slot", "prefix")
            classList.add(LAYOUT_NAV_ICON)
            setAttribute("aria-hidden", "true")
            setAttribute("noobserver", "")
        }

    private fun buildActions() {
        actions.append(docsMenu, languageMenu, themeMenu)
    }

    /**
     * A menu entry's content: a `vaadin-menu-bar-item` holding an Iconify icon and the label; the
     * current entry of a menu has its label emphasised.
     *
     * Everything here is inline on purpose: an entry is teleported into the menu's overlay, which
     * lives in the menu-bar's own shadow tree, so a class declared in [LAYOUT_SHEET] cannot reach it.
     * The inline declarations travel with the element; this is the documented exception for nodes
     * that leave their owner's tree.
     */
    private fun menuItemNode(action: String, icon: String, label: String, checked: Boolean): HTMLElement {
        // The tag is held separately: `TagName<HTMLElement>("…") { }` would read as a trailing lambda on
        // the factory function, not as a call on the tag it returns.
        val itemTag: TagName<HTMLElement> = TagName("vaadin-menu-bar-item")
        return itemTag {
            if (action.isNotEmpty()) dataset["action"] = action
            +menuIcon(icon)
            val text = +HtmlTagName.span { textContent = label }
            if (checked) {
                text.inlineStyle.apply {
                    fontWeight = integer(600)
                    color = Color("var(--lumo-primary-text-color)")
                }
            }
        }
    }

    private fun menuIcon(name: String): IconifyIconElement =
        createIconifyIcon(name) {
            setAttribute("aria-hidden", "true")
            inlineStyle.apply {
                fontSize = 18.px
                marginRight = 8.px
            }
            // Not for size — see the note above [navIcon].
            setAttribute("noobserver", "")
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

        docsMenu.toggleAttribute("hidden", entries.isEmpty())
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
        val entry = menu.entry(route) ?: menu.entries.firstOrNull() ?: return
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

        // On a narrow screen the drawer is an overlay; a reader who just picked a page expects it to
        // get out of the way, so opening a route closes it. Wide screens keep it open — there it is a
        // standing column, and `drawerOpened = false` would hide the navigation altogether. The 800px
        // threshold is Vaadin app layout's own drawer-overlay breakpoint.
        if (window.innerWidth < 800) {
            appLayout.drawerOpened = false
        }

        if (content.firstElementChild !== page.node) {
            content.replaceChildren(page.node)
        }
    }

    private fun setBreadcrumb(entry: Entry) {
        val current = textsFor(language.current)
        crumbGroup.textContent = current.groupTitles[entry.group.id] ?: entry.group.id
        // The group is a real destination: the first page of that group. The page itself is the
        // current item, rendered without a path so breadcrumbs marks it `aria-current="page"`.
        menu.entriesOf(entry.group).firstOrNull()?.let { crumbGroup.path = "#/${it.id}" }
        crumbPage.textContent = titleOf(entry, language.current)
        crumbPage.removeAttribute("path")
    }

    private fun applyLanguage() {
        val current = textsFor(language.current)
        siteTitle.textContent = current.siteTitle

        menu.groups.forEach { group ->
            groupLabels[group.id]?.textContent = current.groupTitles[group.id] ?: group.id
        }
        menu.entries.forEach { entry ->
            navLabels[entry.id]?.textContent = titleOf(entry, language.current)
        }

        rebuildMenus()
        pages.values.forEach { it.applyTranslation(language.current) }

        val entry = shownRoute?.let { menu.entry(it) }
        if (entry != null) {
            setBreadcrumb(entry)
            document.title = "${titleOf(entry, language.current)} · ${current.siteTitle}"
        } else {
            document.title = current.siteTitle
        }
    }

    companion object : DemoLayoutDefaultReference()
}
