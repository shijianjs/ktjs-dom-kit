package io.github.shijianjs.ktjs.demo.page

import io.github.shijianjs.ktjs.demo.Palette
import io.github.shijianjs.ktjs.dom.element.invoke
import io.github.shijianjs.ktjs.dom.element.unaryPlus
import io.github.shijianjs.ktjs.vaadin.card.CardElement
import io.github.shijianjs.ktjs.vaadin.card.createCard
import io.github.shijianjs.ktjs.vaadin.horizontallayout.HorizontalLayoutElement
import io.github.shijianjs.ktjs.vaadin.horizontallayout.createHorizontalLayout
import io.github.shijianjs.ktjs.vaadin.themablemixin.addTheme
import io.github.shijianjs.ktjs.vaadin.horizontallayout.HorizontalLayoutVariants
import io.github.shijianjs.ktjs.vaadin.verticallayout.VerticalLayoutVariants
import io.github.shijianjs.ktjs.vaadin.verticallayout.VerticalLayoutElement
import io.github.shijianjs.ktjs.vaadin.verticallayout.createVerticalLayout
import web.dom.Node
import web.html.HTMLAnchorElement
import web.html.HTMLHeadingElement
import web.html.HTMLElement
import web.html.HtmlTagName
import web.window.WindowTarget
import web.window._blank

/**
 * The blocks a page is built from.
 *
 * Boxes and groups are Vaadin's layout components (`vaadin-vertical-layout`,
 * `vaadin-horizontal-layout`, `vaadin-card`) with their `padding` / `spacing` theme variants: gaps,
 * padding and the card surface are the component library's business, so this site only styles
 * typography and the few colours that come from [Palette]. Nodes are created through dom-kit's typed
 * helpers — `HtmlTagName.p { … }` for a standalone node, `+HtmlTagName.li { … }` for a child inside
 * its parent's block — so no tag name is ever written as a string and no markup is parsed.
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

/** One example box: a Vaadin card, with an optional caption above the content. */
fun exampleBox(caption: String? = null, vararg children: Node): CardElement {
    val card = createCard()
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

/** One titled section: title, optional hint, and the body its examples go into. */
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

/** Creates an empty section; the caller fills [SectionBlock.body]. */
fun sectionBlock(): SectionBlock {
    val title = HtmlTagName.h2 { classList.add(SECTION_TITLE) }
    val hint = HtmlTagName.p { classList.add(SECTION_HINT) }
    val body = createVerticalLayout { addTheme(VerticalLayoutVariants.spacing) }
    // Title, hint and body sit tight; the sheet supplies their margins, the body keeps the component's
    // spacing between its paragraphs and examples.
    val root = createVerticalLayout()
    root.classList.add(SECTION)
    root.append(title, hint, body)
    return SectionBlock(root, title, hint, body)
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
