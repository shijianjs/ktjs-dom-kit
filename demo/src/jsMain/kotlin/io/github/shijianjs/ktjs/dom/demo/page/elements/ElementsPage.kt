package io.github.shijianjs.ktjs.dom.demo.page.elements

import io.github.shijianjs.ktjs.dom.element.createElement
import io.github.shijianjs.ktjs.demo.i18n.Binder
import io.github.shijianjs.ktjs.demo.i18n.Lang
import io.github.shijianjs.ktjs.demo.i18n.Langs
import io.github.shijianjs.ktjs.demo.nav.Page
import io.github.shijianjs.ktjs.demo.i18n.Translations
import io.github.shijianjs.ktjs.demo.page.codeText
import io.github.shijianjs.ktjs.demo.page.exampleBox
import io.github.shijianjs.ktjs.demo.page.heading
import io.github.shijianjs.ktjs.demo.page.newPage
import io.github.shijianjs.ktjs.demo.page.paragraph
import io.github.shijianjs.ktjs.demo.page.row
import io.github.shijianjs.ktjs.demo.page.sectionBlock
import io.github.shijianjs.ktjs.demo.i18n.texts
import io.github.shijianjs.ktjs.vaadin.button.createButton
import io.github.shijianjs.ktjs.vaadin.themablemixin.addTheme
import io.github.shijianjs.ktjs.vaadin.theme.ButtonVariants
import web.dom.clickEvent
import web.events.addHandler
import web.html.HTMLElement
import web.html.HtmlTagName

private class ElementsTexts(
    val title: String,
    val summary: String,
    val typedHeading: String,
    val typedHint: String,
    val typedExample: String,
    val typedNode: String,
    val typedNote: String,
    val collectionHeading: String,
    val collectionHint: String,
    val addLabel: String,
    val collectionNote: String,
)

private val elementsEnglish: ElementsTexts = ElementsTexts(
    title = "Element creation",
    summary = "Every HTML tag has a typed constant in kotlin-browser; dom-kit's createElement carries that " +
        "type through to the created node.",
    typedHeading = "The tag decides the type",
    typedHint = "Every node in this example was built with createElement — not one string tag name.",
    typedExample = "HtmlTagName.h2 / HtmlTagName.p / HtmlTagName.a / HtmlTagName.code",
    typedNode = "Created with a typed tag",
    typedNote = "No cast and no querySelector: the created node already is the element type.",
    collectionHeading = "Updating a collection in place",
    collectionHint = "Adding an entry appends one item; nothing is cleared and the page is not rebuilt.",
    addLabel = "Add entry",
    collectionNote = "The list keeps its own nodes: a new entry is one more li, the rest are untouched.",
)

private val elementsChinese: ElementsTexts = ElementsTexts(
    title = "元素创建",
    summary = "kotlin-browser 给每个 HTML 标签都提供了类型化常量，dom-kit 的 createElement 把这个类型一直带到创建结果。",
    typedHeading = "标签决定类型",
    typedHint = "下面每个节点都是用 createElement 建的，没有一个字符串标签。",
    typedExample = "HtmlTagName.h2 / HtmlTagName.p / HtmlTagName.a / HtmlTagName.code",
    typedNode = "用类型化标签建出来的节点",
    typedNote = "不需要强转，也不需要 querySelector：建出来的就是那个元素类型。",
    collectionHeading = "就地更新集合",
    collectionHint = "新增条目只 append 一项，不清空，也不重建页面。",
    addLabel = "新增一条",
    collectionNote = "列表自己持有节点：新增一条只是多一个 li，其余节点不动。",
)

private val elementsTexts: Translations<ElementsTexts> = texts(
    Langs.EN to elementsEnglish,
    Langs.ZH_HANS to elementsChinese,
)

/**
 * `TagName<T>.createElement { }`: the tag carries the element type, so the created node needs no cast
 * and no `querySelector`, and a growing list appends instead of rebuilding.
 */
internal class ElementsPage : Page {
    private val page = newPage()

    private val texts = Binder(elementsTexts)

    private val list: HTMLElement = HtmlTagName.ul.createElement()

    private var entries = 0

    override val node get() = page

    init {
        val typed = sectionBlock()
        texts.bind(typed.title, { it.typedHeading })
        texts.bind(typed.hint, { it.typedHint })
        typed.body.append(
            exampleBox(null, texts.bind(paragraph(), { it.typedExample })),
            exampleBox(
                null,
                texts.bind(heading(3), { it.typedNode }),
                texts.bind(paragraph(), { it.typedNote }),
                row(codeText("HtmlTagName.a.createElement { href = … }")),
            ),
        )

        val collection = sectionBlock()
        texts.bind(collection.title, { it.collectionHeading })
        texts.bind(collection.hint, { it.collectionHint })

        val addButton = texts.bind(createButton(), { it.addLabel }).addTheme(ButtonVariants.primary)
        addButton.clickEvent.addHandler { appendEntry() }

        collection.body.append(exampleBox(null, addButton, list, texts.bind(paragraph(), { it.collectionNote })))

        page.body.append(typed.root, collection.root)
    }

    override fun applyTranslation(lang: Lang) {
        val current = elementsTexts[lang]
        page.setTitle(current.title)
        page.setSummary(current.summary)
        texts.apply(lang)
    }

    private fun appendEntry() {
        entries += 1
        list.append(HtmlTagName.li.createElement { textContent = "entry $entries" })
    }
}
