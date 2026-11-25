package io.github.shijianjs.ktjs.demo.page

import io.github.shijianjs.ktjs.demo.style.Palette
import io.github.shijianjs.ktjs.dom.element.inlineStyle
import io.github.shijianjs.ktjs.dom.element.invoke
import io.github.shijianjs.ktjs.dom.element.unaryPlus
import io.github.shijianjs.ktjs.iconify.createDecorativeIcon
import io.github.shijianjs.ktjs.vaadin.card.CardElement
import io.github.shijianjs.ktjs.vaadin.card.CardVariants
import io.github.shijianjs.ktjs.vaadin.card.createCard
import io.github.shijianjs.ktjs.vaadin.horizontallayout.HorizontalLayoutElement
import io.github.shijianjs.ktjs.vaadin.horizontallayout.HorizontalLayoutVariants
import io.github.shijianjs.ktjs.vaadin.horizontallayout.createHorizontalLayout
import io.github.shijianjs.ktjs.vaadin.themablemixin.addTheme
import io.github.shijianjs.ktjs.vaadin.verticallayout.VerticalLayoutElement
import io.github.shijianjs.ktjs.vaadin.verticallayout.VerticalLayoutVariants
import io.github.shijianjs.ktjs.vaadin.verticallayout.createVerticalLayout
import web.cssom.px
import web.dom.Node
import web.html.HTMLAnchorElement
import web.html.HTMLHeadingElement
import web.html.HTMLElement
import web.html.HtmlTagName
import web.window.WindowTarget
import web.window._blank

/**
 * The one-node vocabulary a page is built from, as static members of one class.
 *
 * **A block that composes nodes is an element; a block that is one node is a factory.** The elements
 * of this vocabulary are classes of their own — [SectionBlock], [Grid], [Readout], [EventLog] — and
 * are created through their own tag (`SectionBlock.NAME { show(…) }`), never through a wrapper
 * function here: a wrapper would only restate what the tag already does. What is left for this file
 * is the case where the thing really is **one native tag or one component**:
 *
 * - [paragraph] / [heading] / [anchor] / [codeText] / [bulletList] / [exampleCaption] / [icon] return
 *   `<p>`, `<h2>`, `<a>`, `<code>`, `<ul>`, a caption `<p>` or `<iconify-icon>`. Wrapping `<h2>` in a
 *   custom element would cost the document outline, wrapping `<a>` the link semantics.
 * - [row] / [col] / [exampleBox] return `vaadin-horizontal-layout` / `vaadin-vertical-layout` /
 *   `vaadin-card` with a theme variant, which is the "must be that component, because it reads its own
 *   children or slots" case (`vaadin-card` only styles direct `::slotted` children).
 *
 * Members are importable one by one (`import …page.Blocks.paragraph`), so a page writes `paragraph(…)`
 * while the declaration still belongs to a class rather than to the package.
 *
 * Boxes and groups come from Vaadin's layout components, so gaps, padding and the card surface are the
 * component library's business; this site only styles typography and the few colours from [Palette].
 * Nodes are created through dom-kit's typed helpers — `HtmlTagName.p { … }` for a standalone node,
 * `+HtmlTagName.li { … }` for a child inside its parent's block — so no tag name is ever written as a
 * string and no markup is parsed.
 */
object Blocks {

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
     * The `outlined` variant draws the hairline and the surface stays [Palette.background] — the card the
     * example sits on — which is what both Ant Design and Vaadin's own samples do: an example reads as a
     * cut-out on the page instead of another grey block on a grey page. Both come from the component — no
     * border, radius or padding of this site's own is added here.
     */
    fun exampleBox(caption: String? = null, vararg children: Node): CardElement {
        val card = createCard()
        card.addTheme(CardVariants.outlined)
        card.inlineStyle.apply { backgroundColor = Palette.background }
        if (caption != null) card.append(exampleCaption(caption))
        if (children.isNotEmpty()) card.append(*children)
        return card
    }

    /**
     * A caption line for an example box, as its own node.
     *
     * Use it when the caption has to be translated: bind it to the page's copy and pass it as one of
     * the box's children, instead of giving [exampleBox] a caption string that is fixed at construction
     * time.
     */
    fun exampleCaption(text: String = ""): HTMLElement = HtmlTagName.p {
        classList.add(EXAMPLE_CAPTION)
        textContent = text
    }

    /**
     * An `<iconify-icon>` as a plain node, for marking a heading the way the drawer marks its group.
     *
     * The element type stays inside this module: pages get a node they can append, not the icon set.
     * Size is `font-size` — the element renders an `<svg width="1em">`, which `width`/`height` would
     * only stretch. The aria/observer presets come from [createDecorativeIcon]: this icon repeats
     * the heading next to it, so it stays out of the accessibility tree, and its glyph must survive
     * being scrolled or folded away.
     */
    fun icon(name: String, size: Int = 18): HTMLElement =
        createDecorativeIcon(name) {
            inlineStyle.apply { fontSize = size.px }
        }
}
