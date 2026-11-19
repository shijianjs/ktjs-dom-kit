package io.github.shijianjs.ktjs.demo.page

import io.github.shijianjs.ktjs.demo.style.Palette
import io.github.shijianjs.ktjs.dom.element.inlineStyle
import io.github.shijianjs.ktjs.dom.element.invoke
import io.github.shijianjs.ktjs.dom.element.unaryPlus
import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementAdapter
import io.github.shijianjs.ktjs.dom.webcomponent.defineIfMissing
import io.github.shijianjs.ktjs.iconify.createIconifyIcon
import io.github.shijianjs.ktjs.vaadin.card.CardElement
import io.github.shijianjs.ktjs.vaadin.card.CardVariants
import io.github.shijianjs.ktjs.vaadin.card.createCard
import io.github.shijianjs.ktjs.vaadin.horizontallayout.HorizontalLayoutElement
import io.github.shijianjs.ktjs.vaadin.horizontallayout.createHorizontalLayout
import io.github.shijianjs.ktjs.vaadin.themablemixin.addTheme
import io.github.shijianjs.ktjs.vaadin.horizontallayout.HorizontalLayoutVariants
import io.github.shijianjs.ktjs.vaadin.verticallayout.VerticalLayoutVariants
import io.github.shijianjs.ktjs.vaadin.verticallayout.VerticalLayoutElement
import io.github.shijianjs.ktjs.vaadin.verticallayout.createVerticalLayout
import web.components.CustomElementReference
import web.dom.Node
import web.dom.TagName
import web.html.HTMLAnchorElement
import web.html.HTMLHeadingElement
import web.html.HTMLElement
import web.html.HtmlTagName
import web.window.WindowTarget
import web.window._blank
import web.cssom.px

/**
 * The blocks a page is built from.
 *
 * **A block that composes several nodes is one of two things: a registered element when its API allows
 * it, a plain class when it cannot be.** This file therefore has:
 *
 * - `Grid` is a custom element (`<ktjs-demo-grid>`): the host is the grid, children are appended
 *   straight into it, and it stays in the light DOM so its cells keep the page's sheet.
 * - `SectionBlock` is a class, **not** a tag: a custom element inherits `HTMLElement.title` (the native
 *   tooltip string), so a node named `title` could not be declared on the element at all — and twenty
 *   demos bind `section.title` / `section.hint`. The class owns the title, the hint and the body and
 *   hands out a `vaadin-vertical-layout` as their root, so the nodes are still assembled by a class.
 * - `paragraph` / `heading` / `anchor` / `codeText` / `bulletList` / `icon` are factories, because what
 *   they return is **one native tag or one component**: `<p>`, `<h2>`, `<a>`, `<code>`, `<ul>`,
 *   `<iconify-icon>`. That is the equivalent of the JSX a component renders — wrapping `<h2>` in a
 *   custom element would cost the document outline, wrapping `<a>` the link semantics. `row` / `col` /
 *   `exampleBox` return `vaadin-horizontal-layout` / `vaadin-vertical-layout` / `vaadin-card` with a
 *   theme variant, which is the "must be that component, because it reads its own children or slots"
 *   case (`vaadin-card` only styles direct `::slotted` children).
 *
 * Boxes and groups come from Vaadin's layout components, so gaps, padding and the card surface are the
 * component library's business; this site only styles typography and the few colours from [Palette].
 * Nodes are created through dom-kit's typed helpers — `HtmlTagName.p { … }` for a standalone node,
 * `+HtmlTagName.li { … }` for a child inside its parent's block — so no tag name is ever written as a
 * string and no markup is parsed.
 *
 * A class constant is declared here only when this file and a sheet in another file both use it; a
 * class a single page introduces stays in that page.
 */

/** Creates a `<p>` with [text]. */
fun paragraph(text: String = ""): HTMLElement = HtmlTagName.p { textContent = text }

/** Creates a heading of [level] (1-4 coerced). */
fun heading(level: Int, text: String = ""): HTMLHeadingElement {
    val tag = when (level.coerceIn(1, 4)) {
        1 -> HtmlTagName.h1
        2 -> HtmlTagName.h2
        3 -> HtmlTagName.h3
        else -> HtmlTagName.h4
    }
    return tag { textContent = text }
}

/** Creates an `<a>` pointing at [href]; external links open in a new tab. */
fun anchor(text: String, href: String, external: Boolean = false): HTMLAnchorElement =
    HtmlTagName.a {
        this.textContent = text
        this.href = href
        if (external) {
            target = WindowTarget.Companion._blank
            rel = "noopener noreferrer"
        }
    }

/** Creates an inline `<code>` fragment. */
fun codeText(text: String): HTMLElement = HtmlTagName.code {
    classList.add(CODE)
    textContent = text
}

/** Creates a `<ul>` with one `<li>` per entry; each item is created and appended by `+`. */
fun bulletList(items: List<String>): HTMLElement = HtmlTagName.ul {
    items.forEach { item -> +HtmlTagName.li { textContent = item } }
}

/** A horizontal group: the Vaadin layout supplies the gap, the site sheet centres the items. */
fun row(vararg children: Node): HorizontalLayoutElement {
    val layout = createHorizontalLayout { addTheme(HorizontalLayoutVariants.spacing) }
    layout.classList.add(ROW)
    if (children.isNotEmpty()) layout.append(*children)
    return layout
}

/** A vertical group: the Vaadin layout supplies the gap. */
fun col(vararg children: Node): VerticalLayoutElement {
    val layout = createVerticalLayout { addTheme(VerticalLayoutVariants.spacing) }
    if (children.isNotEmpty()) layout.append(*children)
    return layout
}

/**
 * One example box: a Vaadin card, with an optional caption above the content.
 *
 * The `outlined` variant draws the hairline and the surface stays the page background, which is what
 * both Ant Design and Vaadin's own samples do: an example reads as a cut-out on the page instead of
 * another grey block on a grey page. Both come from the component — no border, radius or padding of
 * this site's own is added here.
 */
fun exampleBox(caption: String? = null, vararg children: Node): CardElement {
    val card = createCard()
    card.addTheme(CardVariants.outlined)
    card.inlineStyle.apply { backgroundColor = Palette.background }
    if (caption != null) {
        card.append(
            HtmlTagName.p {
                classList.add(EXAMPLE_CAPTION)
                textContent = caption
            },
        )
    }
    if (children.isNotEmpty()) card.append(*children)
    return card
}

/**
 * A responsive grid of its children, **as an element**.
 *
 * The host is the grid: children are appended straight into it, so they stay in the page's light DOM
 * and keep the page's sheet. The track function (`repeat(auto-fill, minmax(…)`) has no typed
 * counterpart in the CSS vocabulary, so it stays one declaration; the rest of the block is the
 * `.grid` rule in [PAGE_SHEET].
 *
 * The block spans its container. A section body is a vertical layout, which sizes children to their
 * content and aligns them to the start; without the width a grid would pick its column count from
 * its own narrowest width rather than from the page.
 */
class Grid : CustomElementAdapter() {
    /** Sizes the tracks; the children are already the element's own children. */
    fun show(minColumnWidth: Int) {
        classList.add(GRID)
        style.setProperty("grid-template-columns", "repeat(auto-fill, minmax(${minColumnWidth}px, 1fr))")
        style.setProperty("gap", "12px")
        style.setProperty("width", "100%")
    }

    /** Adds one cell; the element keeps the order they arrive in. */
    fun add(vararg children: Node) {
        if (children.isNotEmpty()) append(*children)
    }

    companion object : CustomElementReference<Grid>(Grid::class.js) {
        val NAME: TagName<Grid> = TagName("ktjs-demo-grid")

        fun define() {
            defineIfMissing(NAME, this)
        }
    }
}

/** Creates a grid of [children]: as many columns of at least [minColumnWidth] pixels as fit the page. */
fun grid(minColumnWidth: Int = 260, vararg children: Node): Grid {
    Grid.define()
    return Grid.NAME().apply {
        show(minColumnWidth)
        add(*children)
    }
}

/**
 * An `<iconify-icon>` as a plain node, for marking a heading the way the drawer marks its group.
 *
 * The element type stays inside this module: pages get a node they can append, not the icon set.
 * Size is `font-size` — the element renders an `<svg width="1em">`, which `width`/`height` would only
 * stretch — and `noobserver` keeps the glyph rendered wherever it is scrolled or folded away.
 */
fun icon(name: String, size: Int = 18): HTMLElement =
    createIconifyIcon(name) {
        setAttribute("aria-hidden", "true")
        setAttribute("noobserver", "")
        inlineStyle.apply { fontSize = size.px }
    }

/**
 * One titled section: title, optional hint, and the body its examples go into.
 *
 * The class owns all three nodes, so nothing here is assembled outside a class. It is **not** a
 * registered tag, and the reason is structural rather than stylistic: a custom element inherits
 * `HTMLElement.title` (the native tooltip attribute, a `String`), so a node named `title` on the
 * element would shadow the browser's own member and could not be declared at all. Twenty demos read
 * `section.title` / `section.hint` (a `Binder` binds them), so the API is the class's, and the class
 * builds a `vaadin-vertical-layout` as its root instead.
 */
class SectionBlock(
    val root: VerticalLayoutElement,
    val title: HTMLElement,
    val hint: HTMLElement,
    val body: VerticalLayoutElement,
) {
    /** Writes [title] and [hint]; an empty [hintText] hides the hint line. */
    fun setText(title: String, hintText: String = "") {
        this.title.textContent = title
        this.hint.textContent = hintText
        this.hint.toggleAttribute("hidden", hintText.isEmpty())
    }
}

/** Creates a section that already carries [title] and, when given, [hint]. */
fun sectionBlock(title: String? = null, hint: String? = null): SectionBlock {
    val titleNode = HtmlTagName.h2 { classList.add(SECTION_TITLE) }
    val hintNode = HtmlTagName.p { classList.add(SECTION_HINT) }
    val body = createVerticalLayout { addTheme(VerticalLayoutVariants.spacing) }
    // Title, hint and body sit tight; the sheet supplies their margins, the body keeps the component's
    // spacing between its paragraphs and examples.
    val root = createVerticalLayout()
    root.classList.add(SECTION)
    // A vertical layout sizes its children to their content and aligns them to the start, so a section
    // would otherwise be as wide as its widest child — a grid or a row would then pick its column count
    // from that width instead of the page's. Sections always span the page.
    root.style.setProperty("width", "100%")
    body.style.setProperty("width", "100%")
    root.append(titleNode, hintNode, body)
    return SectionBlock(root, titleNode, hintNode, body).also { it.setText(title ?: "", hint ?: "") }
}

/**
 * A caption line for an example box, as its own node.
 *
 * Use it when the caption has to be translated: bind it to the page's copy and pass it as one of the
 * box's children, instead of giving [exampleBox] a caption string that is fixed at construction time.
 */
fun exampleCaption(text: String = ""): HTMLElement = HtmlTagName.p {
    classList.add(EXAMPLE_CAPTION)
    textContent = text
}

/** Text that is updated in place to show the current state of an example. */
class Readout(initial: String = "") {
    val root: HTMLElement = HtmlTagName.p {
        classList.add(READOUT)
        textContent = initial
    }

    fun set(text: String) {
        root.textContent = text
    }
}

/**
 * An append-only log of what an example did, newest line last.
 *
 * The examples fire events faster than a reader can follow, so the box keeps the [capacity] newest
 * lines only: older lines are removed, not hidden. The scroll container is part of the box, so a long
 * page never grows a second scrollbar because of the log.
 */
class EventLog(private val capacity: Int = 6, placeholder: String? = null) {
    val root: HTMLElement = HtmlTagName.div { classList.add(EVENT_LOG) }

    /** Drawn until the first line arrives, so an untouched log reads as "nothing yet" and not as an empty bar. */
    private val placeholder: HTMLElement? = placeholder?.let { text ->
        HtmlTagName.div { classList.add(EVENT_LINE); textContent = text }
    }

    init {
        if (placeholder != null) root.append(placeholder)
    }

    /** Appends one line, dropping the oldest ones past [capacity]. */
    fun add(line: String) {
        placeholder?.remove()
        root.append(HtmlTagName.div { classList.add(EVENT_LINE); textContent = line })
        while (root.childElementCount > capacity) {
            root.firstElementChild?.remove()
        }
        root.scrollTop = root.scrollHeight.toDouble()
    }

    /** Drops every line. */
    fun clear() {
        root.replaceChildren()
    }
}
