package io.github.shijianjs.ktjs.demo

import io.github.shijianjs.ktjs.dom.element.createElement
import io.github.shijianjs.ktjs.vaadin.card.CardElement
import io.github.shijianjs.ktjs.vaadin.card.createCard
import io.github.shijianjs.ktjs.vaadin.horizontallayout.HorizontalLayoutElement
import io.github.shijianjs.ktjs.vaadin.horizontallayout.createHorizontalLayout
import io.github.shijianjs.ktjs.vaadin.verticallayout.VerticalLayoutElement
import io.github.shijianjs.ktjs.vaadin.verticallayout.createVerticalLayout
import web.cssom.ClassName
import web.dom.Node
import web.dom.TagName
import web.html.HTMLAnchorElement
import web.html.HTMLHeadingElement
import web.html.HTMLElement
import web.html.Hidden
import web.html.HtmlTagName
import web.html.`false`
import web.html.`true`
import web.window.WindowTarget
import web.window._blank

/**
 * The blocks a page is built from.
 *
 * Boxes and groups are Vaadin's layout components (`vaadin-vertical-layout`,
 * `vaadin-horizontal-layout`, `vaadin-card`) with their `padding` / `spacing` theme variants: gaps,
 * padding and the card surface are the component library's business, so this site only styles
 * typography and the few colours that come from [Palette]. Nodes are created through dom-kit's typed
 * `createElement`, so no tag name is ever written as a string and no markup is parsed.
 *
 * A class constant is declared here only when this file and a sheet in another file both use it; a
 * class a single page introduces stays in that page.
 */

/** Creates an element of [tag], optionally with [className], and appends [children]. */
fun <T : HTMLElement> element(tag: TagName<T>, className: ClassName? = null, vararg children: Node): T =
    tag.createElement {
        if (className != null) classList.add(className)
        if (children.isNotEmpty()) append(*children)
    }

/** Creates a plain `<div>`; used only where a component has no suitable box of its own. */
fun div(vararg children: Node): HTMLElement = element(HtmlTagName.div, null, *children)

/** Creates a `<p>` with [text]. */
fun paragraph(text: String = ""): HTMLElement = element(HtmlTagName.p).apply { textContent = text }

/** Creates a `<span>` with [text]. */
fun span(text: String = ""): HTMLElement = element(HtmlTagName.span).apply { textContent = text }

/** Creates a heading of [level] (1-4 coerced). */
fun heading(level: Int, text: String = ""): HTMLElement {
    val tag = when (level.coerceIn(1, 4)) {
        1 -> HtmlTagName.h1
        2 -> HtmlTagName.h2
        3 -> HtmlTagName.h3
        else -> HtmlTagName.h4
    }
    val created: HTMLHeadingElement = tag.createElement()
    created.textContent = text
    return created
}

/** Creates an `<a>` pointing at [href]; external links open in a new tab. */
fun anchor(text: String, href: String, external: Boolean = false): HTMLAnchorElement {
    val created = element(HtmlTagName.a)
    created.textContent = text
    created.href = href
    if (external) {
        created.target = WindowTarget.Companion._blank
        created.rel = "noopener noreferrer"
    }
    return created
}

/** Creates an inline `<code>` fragment. */
fun codeText(text: String): HTMLElement = element(HtmlTagName.code, CODE).apply { textContent = text }

/** Creates a `<ul>` with one `<li>` per entry. */
fun bulletList(items: List<String>): HTMLElement {
    val list = element(HtmlTagName.ul)
    items.forEach { item -> list.append(element(HtmlTagName.li).apply { textContent = item }) }
    return list
}

/** Adds [className] and returns the element, so calls can chain. */
fun <T : HTMLElement> T.withClass(className: ClassName): T {
    classList.add(className)
    return this
}

/** Sets Vaadin's `theme` attribute from [variants]; no variants removes it. */
fun <T : HTMLElement> T.withTheme(vararg variants: String): T {
    if (variants.isEmpty()) {
        removeAttribute("theme")
    } else {
        setAttribute("theme", variants.joinToString(" "))
    }
    return this
}

/** Shows or hides the element with the native `hidden` property. */
fun HTMLElement.setHidden(hidden: Boolean) {
    this.hidden = if (hidden) Hidden.Companion.`true` else Hidden.Companion.`false`
}

/** A horizontal group: the Vaadin layout supplies the gap, the site sheet centres the items. */
fun row(vararg children: Node): HorizontalLayoutElement {
    val layout = createHorizontalLayout { withTheme("spacing") }
    layout.classList.add(ROW)
    if (children.isNotEmpty()) layout.append(*children)
    return layout
}

/** A vertical group: the Vaadin layout supplies the gap. */
fun col(vararg children: Node): VerticalLayoutElement {
    val layout = createVerticalLayout { withTheme("spacing") }
    if (children.isNotEmpty()) layout.append(*children)
    return layout
}

/** One example box: a Vaadin card, with an optional caption above the content. */
fun exampleBox(caption: String? = null, vararg children: Node): CardElement {
    val card = createCard()
    if (caption != null) card.append(element(HtmlTagName.p, EXAMPLE_CAPTION).apply { textContent = caption })
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
        this.hint.setHidden(hintText.isEmpty())
    }
}

/** Creates an empty section; the caller fills [SectionBlock.body]. */
fun sectionBlock(): SectionBlock {
    val title = element(HtmlTagName.h2, SECTION_TITLE)
    val hint = element(HtmlTagName.p, SECTION_HINT)
    val body = createVerticalLayout { withTheme("spacing") }
    // Title, hint and body sit tight; the sheet supplies their margins, the body keeps the component's
    // spacing between its paragraphs and examples.
    val root = createVerticalLayout()
    root.classList.add(SECTION)
    root.append(title, hint, body)
    return SectionBlock(root, title, hint, body)
}

/** Text that is updated in place to show the current state of an example. */
class Readout(initial: String = "") {
    val root: HTMLElement = element(HtmlTagName.p, READOUT).apply { textContent = initial }

    fun set(text: String) {
        root.textContent = text
    }
}
