package io.github.shijianjs.ktjs.dom.demo.page.elements

import io.github.shijianjs.ktjs.dom.element.invoke
import io.github.shijianjs.ktjs.dom.element.unaryPlus
import io.github.shijianjs.ktjs.demo.i18n.Binder
import io.github.shijianjs.ktjs.demo.i18n.Lang
import io.github.shijianjs.ktjs.demo.i18n.Langs
import io.github.shijianjs.ktjs.demo.page.DemoPage
import io.github.shijianjs.ktjs.dom.webcomponent.defineIfMissing
import web.components.CustomElementReference
import web.dom.TagName
import io.github.shijianjs.ktjs.demo.i18n.Translations
import io.github.shijianjs.ktjs.demo.page.codeText
import io.github.shijianjs.ktjs.demo.page.exampleBox
import io.github.shijianjs.ktjs.demo.page.heading
import io.github.shijianjs.ktjs.demo.page.paragraph
import io.github.shijianjs.ktjs.demo.page.row
import io.github.shijianjs.ktjs.demo.page.sectionBlock
import io.github.shijianjs.ktjs.demo.i18n.texts
import io.github.shijianjs.ktjs.vaadin.button.createButton
import io.github.shijianjs.ktjs.vaadin.themablemixin.addTheme
import io.github.shijianjs.ktjs.vaadin.button.ButtonVariants
import web.dom.clickEvent
import web.cssom.ClassName
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
    val subtreeHeading: String,
    val subtreeHint: String,
    val subtreeCode: String,
    val subtreeNote: String,
)

private val elementsEnglish: ElementsTexts = ElementsTexts(
    title = "Element creation",
    summary = "Every HTML tag has a typed constant in kotlin-browser; dom-kit's createElement carries that " +
        "type through to the created node.",
    typedHeading = "The tag decides the type",
    typedHint = "Every node in this example was built from a typed tag — not one string tag name in sight.",
    typedExample = "HtmlTagName.h2 / HtmlTagName.p / HtmlTagName.a / HtmlTagName.code",
    typedNode = "Created with a typed tag",
    typedNote = "No cast and no querySelector: the created node already is the element type.",
    collectionHeading = "Updating a collection in place",
    collectionHint = "Adding an entry appends one item; nothing is cleared and the page is not rebuilt.",
    addLabel = "Add entry",
    collectionNote = "The list keeps its own nodes: a new entry is one more li, the rest are untouched.",
    subtreeHeading = "One expression, one subtree",
    subtreeHint = "The operator creates the child and appends it to the element being built; its value is " +
        "the child, so the same call works when the builder needs the node later.",
    subtreeCode = "+HtmlTagName.li { textContent = line }",
    subtreeNote = "The tree below is one expression: article, heading, paragraph, list — and every list " +
        "item again built with the same operator.",
)

private val elementsChinese: ElementsTexts = ElementsTexts(
    title = "元素创建",
    summary = "kotlin-browser 给每个 HTML 标签都提供了类型化常量，dom-kit 的 createElement 把这个类型一直带到创建结果。",
    typedHeading = "标签决定类型",
    typedHint = "下面每个节点都是从类型化标签建出来的，一个字符串标签都没有。",
    typedExample = "HtmlTagName.h2 / HtmlTagName.p / HtmlTagName.a / HtmlTagName.code",
    typedNode = "用类型化标签建出来的节点",
    typedNote = "不需要强转，也不需要 querySelector：建出来的就是那个元素类型。",
    collectionHeading = "就地更新集合",
    collectionHint = "新增条目只 append 一项，不清空，也不重建页面。",
    addLabel = "新增一条",
    collectionNote = "列表自己持有节点：新增一条只是多一个 li，其余节点不动。",
    subtreeHeading = "一个表达式，一棵子树",
    subtreeHint = "这个运算符建出子节点并挂进正在构建的元素，表达式的值就是子节点，所以构建者需要留住它时" +
        "也是同一个写法。",
    subtreeCode = "+HtmlTagName.li { textContent = line }",
    subtreeNote = "下面这棵树就是一个表达式：article、标题、段落、列表，列表的每一项同样用这个运算符建。",
)

private val elementsTexts: Translations<ElementsTexts> = texts(
    Langs.EN to elementsEnglish,
    Langs.ZH_HANS to elementsChinese,
)

private val SUBTREE: ClassName = ClassName("elements__subtree")

/** The lines the built subtree shows, and the code the page quotes next to it. */
private val SUBTREE_LINES: List<String> = listOf(
    "+HtmlTagName.h3 { textContent = \"…\" }",
    "texts.bind(+HtmlTagName.p { }, { it.subtreeNote })",
    "+HtmlTagName.ul { lines.forEach { +HtmlTagName.li { +codeText(it) } } }",
)

/**
 * `TagName<T>` decides the element type, so a created node needs no cast and no `querySelector`, and a
 * growing list appends instead of rebuilding.
 *
 * The page shows the three spellings: `HtmlTagName.a { … }` for a node whose parent is decided later,
 * `+HtmlTagName.li { … }` for a child created inside the element being built, and the growing list,
 * where `appendChild` is the honest call because the entry is created outside its parent's block.
 */
internal class ElementsPage : DemoPage() {

    private val texts = Binder(elementsTexts)

    private val list: HTMLElement = HtmlTagName.ul()

    private var entries = 0


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
                row(codeText("HtmlTagName.a { href = … }")),
            ),
        )

        val collection = sectionBlock()
        texts.bind(collection.title, { it.collectionHeading })
        texts.bind(collection.hint, { it.collectionHint })

        val addButton = texts.bind(createButton(), { it.addLabel }).addTheme(ButtonVariants.primary)
        addButton.clickEvent.addHandler { appendEntry() }

        collection.body.append(exampleBox(null, addButton, list, texts.bind(paragraph(), { it.collectionNote })))

        val subtree = sectionBlock()
        texts.bind(subtree.title, { it.subtreeHeading })
        texts.bind(subtree.hint, { it.subtreeHint })

        val built = HtmlTagName.article {
            classList.add(SUBTREE)
            +HtmlTagName.h3 { textContent = "article > h3" }
            texts.bind(+HtmlTagName.p {}, { it.subtreeNote })
            +HtmlTagName.ul {
                SUBTREE_LINES.forEach { line -> +HtmlTagName.li { +codeText(line) } }
            }
        }

        subtree.body.append(
            exampleBox(null, built, row(texts.bind(codeText(""), { it.subtreeCode }))),
        )

        body.append(typed.root, collection.root, subtree.root)
    }

    override fun applyTranslation(lang: Lang) {
        val current = elementsTexts[lang]
        setTitle(current.title)
        setSummary(current.summary)
        texts.apply(lang)
    }

    private fun appendEntry() {
        entries += 1
        list.append(HtmlTagName.li { textContent = "entry $entries" })
    }

    companion object : CustomElementReference<ElementsPage>(ElementsPage::class.js) {
        val NAME: TagName<ElementsPage> = TagName("demo-elements-page")

        fun define() {
            defineIfMissing(NAME, this)
        }
    }
}

/** Creates a defined `<demo-elements-page>`. */
internal fun newElementsPage(): ElementsPage {
    ElementsPage.define()
    return ElementsPage.NAME()
}
