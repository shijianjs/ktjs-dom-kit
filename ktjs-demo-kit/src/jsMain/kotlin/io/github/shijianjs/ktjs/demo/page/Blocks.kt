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
 * **A block that composes nodes is an element; a block that is one node is a factory.** That is the
 * whole rule, and this file is the vocabulary it produces:
 *
 * - `SectionBlock` (`<ktjs-demo-section>`), `Grid` (`<ktjs-demo-grid>`), `Readout`
 *   (`<ktjs-demo-readout>`) and `EventLog` (`<ktjs-demo-event-log>`) are custom elements: each owns
 *   several nodes or its own state, so a function building them would be DOM assembled outside a class.
 *   They live in the light DOM, not in a shadow root: their children are the page's own cards and grids,
 *   and those have to keep receiving the page's sheet.
 * - `paragraph` / `heading` / `anchor` / `codeText` / `bulletList` / `icon` are factories, because what
 *   they return is **one native tag or one component**: `<p>`, `<h2>`, `<a>`, `<code>`, `<ul>`,
 *   `<iconify-icon>`. That is the equivalent of the JSX a component renders — wrapping `<h2>` in a
 *   custom element would cost the document outline, wrapping `<a>` the link semantics. `row` / `col` /
 *   `exampleBox` return `vaadin-horizontal-layout` / `vaadin-vertical-layout` / `vaadin-card` with a
 *   theme variant, which is the "must be that component, because it reads its own children or slots"
 *   case (`vaadin-card` only styles direct `::slotted` children).
 *
 * **Field names never collide with the built-ins of `HTMLElement`.** The living standard owns a lot of
 * short names — `title`, `lang`, `dir`, `hidden`, `id`, `slot`, `style`, `children`, `textContent` —
 * and an element inherits every one of them, so a field with that name cannot be declared at all
 * (`'title' hides member of supertype 'HTMLElement'`). The section's heading node is therefore
 * `heading`, not `title`: renaming a field is cheap, bending the architecture around a name is not.
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
 * One titled section, **as an element**: heading, optional hint, and the [body] its examples go into.
 *
 * The element owns its three nodes, so nothing is assembled outside a class. Two naming rules show up
 * here, and both are the reason the fields are what they are:
 *
 * - The heading node is [heading], not `title`. `HTMLElement.title` is the browser's own tooltip
 *   property (a `String`), so a node called `title` cannot be declared on an element at all — a name
 *   is cheaper to rename than an architecture is to bend around it.
 * - A field a page binds through `Binder` is one word here on purpose (`hint`); anything that would
 *   trip over a built-in `HTMLElement` member gets a second word instead.
 *
 * It stays in the light DOM: its children are the page's own cards and grids, and they have to keep
 * receiving the page's sheet, which a shadow root would cut off. [show] appends the nodes instead of
 * the constructor doing it, because a custom element's constructor may not add children to itself.
 */
class SectionBlock : CustomElementAdapter() {
    /** The node a page binds its title text to. */
    val heading: HTMLElement = HtmlTagName.h2().apply { classList.add(SECTION_TITLE) }

    /** The node a page binds its hint text to; empty text hides it. */
    val hint: HTMLElement = HtmlTagName.p().apply { classList.add(SECTION_HINT) }

    /** The vertical group the examples go into; it keeps the component's gap between them. */
    val body: VerticalLayoutElement = createVerticalLayout { addTheme(VerticalLayoutVariants.spacing) }

    /** The section nodes themselves are this element; a page appends it to its body. */
    val root: HTMLElement get() = this

    /** Fills the section: the width, the three nodes and their text. */
    fun show(heading: String, hintText: String = "") {
        classList.add(SECTION)
        style.setProperty("width", "100%")
        // A vertical layout sizes its children to their content and aligns them to the start, so a
        // section would otherwise be as wide as its widest child — a grid or a row would then pick its
        // column count from that width instead of the page's. Sections always span the page.
        body.style.setProperty("width", "100%")
        append(this.heading, this.hint, body)
        setText(heading, hintText)
    }

    /** Writes the heading and the hint; an empty [hintText] hides the hint line. */
    fun setText(heading: String, hintText: String = "") {
        this.heading.textContent = heading
        this.hint.textContent = hintText
        this.hint.toggleAttribute("hidden", hintText.isEmpty())
    }

    companion object : CustomElementReference<SectionBlock>(SectionBlock::class.js) {
        val NAME: TagName<SectionBlock> = TagName("ktjs-demo-section")

        fun define() {
            defineIfMissing(NAME, this)
        }
    }
}

/** Creates a section that already carries [heading] and, when given, [hint]. */
fun sectionBlock(heading: String? = null, hint: String? = null): SectionBlock {
    SectionBlock.define()
    return SectionBlock.NAME().apply { show(heading ?: "", hint ?: "") }
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

/**
 * Text that is updated in place to show the current state of an example, **as an element**.
 *
 * The element is the readout: `root` is itself, so a page appends it wherever the text belongs, and
 * `set` rewrites its own text. A page that wants a different look wraps it — that is the inheritance
 * and composition case, not a reason to keep this a bare class.
 *
 * The constructor takes no child: a custom element may not add children to itself, so the text and
 * the class arrive through [newReadout]'s [show]. `Readout(initial)` is the same factory by another
 * name, kept because a hundred call sites spell it that way.
 */
class Readout : CustomElementAdapter() {
    /** The readout is its own root node. */
    val root: HTMLElement get() = this

    /** Writes the class and the first text; called once, by the factory. */
    fun show(initial: String = "") {
        classList.add(READOUT)
        textContent = initial
    }

    /** Rewrites the text; the node itself stays put. */
    fun set(text: String) {
        textContent = text
    }

    companion object : CustomElementReference<Readout>(Readout::class.js) {
        val NAME: TagName<Readout> = TagName("ktjs-demo-readout")

        fun define() {
            defineIfMissing(NAME, this)
        }
    }
}

/** Creates a defined `<ktjs-demo-readout>` showing [initial]. */
fun newReadout(initial: String = ""): Readout {
    Readout.define()
    return Readout.NAME().apply { show(initial) }
}

/** `newReadout(initial)` spelled the way the pages have always spelled it. */
fun Readout(initial: String = ""): Readout = newReadout(initial)

/**
 * An append-only log of what an example did, newest line last — **as an element**.
 *
 * The examples fire events faster than a reader can follow, so the box keeps the [capacity] newest
 * lines only: older lines are removed, not hidden. The scroll container is the log itself, so a long
 * page never grows a second scrollbar because of the log.
 */
class EventLog : CustomElementAdapter() {
    /** The log is its own root node. */
    val root: HTMLElement get() = this

    private var capacity: Int = 6

    /** Drawn until the first line arrives, so an untouched log reads as "nothing yet". */
    private var placeholder: HTMLElement? = null

    private var built = false

    /** Sets the box up once: the class, the capacity and the optional placeholder line. */
    fun show(capacity: Int = 6, placeholder: String? = null) {
        if (built) return
        built = true
        this.capacity = capacity
        classList.add(EVENT_LOG)
        if (placeholder != null) {
            val line = HtmlTagName.div { classList.add(EVENT_LINE); textContent = placeholder }
            this.placeholder = line
            append(line)
        }
    }

    /** Appends one line, dropping the oldest ones past the capacity. */
    fun add(line: String) {
        placeholder?.remove()
        placeholder = null
        append(HtmlTagName.div { classList.add(EVENT_LINE); textContent = line })
        while (childElementCount > capacity) {
            firstElementChild?.remove()
        }
        scrollTop = scrollHeight.toDouble()
    }

    /** Drops every line. */
    fun clear() {
        placeholder = null
        replaceChildren()
    }

    companion object : CustomElementReference<EventLog>(EventLog::class.js) {
        val NAME: TagName<EventLog> = TagName("ktjs-demo-event-log")

        fun define() {
            defineIfMissing(NAME, this)
        }
    }
}

/** Creates a defined `<ktjs-demo-event-log>` with [capacity] lines and an optional [placeholder]. */
fun newEventLog(capacity: Int = 6, placeholder: String? = null): EventLog {
    EventLog.define()
    return EventLog.NAME().apply { show(capacity, placeholder) }
}

/** `newEventLog(capacity, placeholder)` spelled the way the pages have always spelled it. */
fun EventLog(capacity: Int = 6, placeholder: String? = null): EventLog =
    newEventLog(capacity, placeholder)
