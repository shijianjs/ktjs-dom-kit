package io.github.shijianjs.ktjs.dom.demo.page.elements

import io.github.shijianjs.ktjs.demo.bilingual
import io.github.shijianjs.ktjs.demo.codeText
import io.github.shijianjs.ktjs.demo.element
import io.github.shijianjs.ktjs.demo.exampleBox
import io.github.shijianjs.ktjs.demo.heading
import io.github.shijianjs.ktjs.demo.paragraph
import io.github.shijianjs.ktjs.demo.row
import io.github.shijianjs.ktjs.demo.sectionBlock
import io.github.shijianjs.ktjs.demo.withTheme
import io.github.shijianjs.ktjs.dom.demo.page.DemoTextPage
import io.github.shijianjs.ktjs.dom.element.createElement
import io.github.shijianjs.ktjs.vaadin.button.createButton
import web.dom.clickEvent
import web.events.addHandler
import web.html.HTMLElement
import web.html.HtmlTagName

/**
 * `TagName<T>.createElement { }`: the tag carries the element type, so the created node needs no cast
 * and no `querySelector`.
 */
internal class ElementsPage : DemoTextPage(
    bilingual("元素创建", "Element creation"),
    bilingual(
        "每个 HTML 标签在 kotlin-browser 里都有类型化的常量；dom-kit 的 createElement 让这个类型一路传到创建结果。",
        "Every HTML tag has a typed constant in kotlin-browser; dom-kit's createElement carries that type " +
            "through to the created node.",
    ),
) {
    private val list: HTMLElement = element(HtmlTagName.ul)

    private var entries = 0

    init {
        val typed = sectionBlock()
        texts.bind(typed.title, bilingual("标签决定类型", "The tag decides the type"))
        texts.bind(
            typed.hint,
            bilingual(
                "下面每个节点都是用 createElement 建的，没有一个字符串标签。",
                "Every node below was built with createElement — not one string tag name.",
            ),
        )
        typed.body.append(
            exampleBox(
                "HtmlTagName.h2, HtmlTagName.p, HtmlTagName.a, HtmlTagName.code",
            ),
            exampleBox(
                null,
                heading(3, "Created with a typed tag"),
                paragraph("No cast, no querySelector: the block already is an HTMLElement."),
                row(codeText("HtmlTagName.anchor.createElement { href = … }")),
            ),
        )

        val collection = sectionBlock()
        texts.bind(collection.title, bilingual("局部集合更新", "Updating a collection in place"))
        texts.bind(
            collection.hint,
            bilingual(
                "新增条目只 append，不清空、不重建页面。",
                "Adding an entry appends; nothing is cleared and the page is not rebuilt.",
            ),
        )

        val addButton = createButton { textContent = "Add entry" }.withTheme("primary")
        addButton.clickEvent.addHandler { appendEntry() }

        collection.body.append(
            exampleBox(null, addButton, list),
        )

        page.body.append(typed.root, collection.root)
    }

    private fun appendEntry() {
        entries += 1
        list.append(element(HtmlTagName.li).apply { textContent = "entry $entries" })
    }
}
