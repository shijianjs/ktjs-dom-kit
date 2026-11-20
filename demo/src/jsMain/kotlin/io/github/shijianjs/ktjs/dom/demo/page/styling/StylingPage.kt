package io.github.shijianjs.ktjs.dom.demo.page.styling

import io.github.shijianjs.ktjs.demo.i18n.Binder
import io.github.shijianjs.ktjs.demo.i18n.Lang
import io.github.shijianjs.ktjs.demo.i18n.Langs
import io.github.shijianjs.ktjs.demo.page.DemoPage
import io.github.shijianjs.ktjs.dom.element.invoke
import io.github.shijianjs.ktjs.dom.webcomponent.defineIfMissing
import web.components.CustomElementReference
import web.dom.TagName
import io.github.shijianjs.ktjs.demo.i18n.Translations
import io.github.shijianjs.ktjs.demo.page.codeText
import io.github.shijianjs.ktjs.demo.page.exampleBox
import io.github.shijianjs.ktjs.demo.page.paragraph
import io.github.shijianjs.ktjs.demo.page.row
import io.github.shijianjs.ktjs.demo.page.sectionBlock
import io.github.shijianjs.ktjs.demo.i18n.texts

private class StylingTexts(
    val title: String,
    val summary: String,
    val adoptedHeading: String,
    val adoptedHint: String,
    val adoptedNote: String,
    val hoverLabel: String,
    val ladderHeading: String,
    val inlineStep: String,
    val sheetStep: String,
    val sharedStep: String,
    val selectorHeading: String,
    val selectorHint: String,
    val selectorNote: String,
    val chipPlain: String,
    val chipSelected: String,
)

private val stylingEnglish: StylingTexts = StylingTexts(
    title = "Stylesheets",
    summary = "A component carries its own sheet and adopts it once; the page and the layout each carry theirs. " +
        "Nothing is adopted on document, and colours come through as inherited custom properties.",
    adoptedHeading = "A class, a sheet, one adopt",
    adoptedHint = "Two elements of the same kind share one parsed sheet: static rules are not rebuilt per instance.",
    adoptedNote = "The hover state lives in the sheet because an inline declaration would outrank the rule.",
    hoverLabel = "Hover me",
    ladderHeading = "Three levels, from the bottom up",
    inlineStep = "Inline: layout that never changes with state — display, gaps, padding, radius.",
    sheetStep = "Component sheet: :hover, :focus-visible, ::before, @media — what inline cannot express.",
    sharedStep = "Shared sheet: the reset (box-sizing, :host display) and the theme tokens every component needs.",
    selectorHeading = "A selector you build",
    selectorHint = "The middle chip is painted by `.selector-chip.is-selected` and by the row's `[aria-current]` " +
        "rule — both keys came from parts, not from a pasted string.",
    selectorNote = "A class name that is spelled wrong — or renamed later — breaks the build here instead of " +
        "producing a rule that silently never matches.",
    chipPlain = "plain",
    chipSelected = "selected",
)

private val stylingChinese: StylingTexts = StylingTexts(
    title = "样式表",
    summary = "组件自带一份样式表、adopt 一次；页面和外壳各带一份。document 上不 adopt 任何样式表，" +
        "颜色通过继承的自定义属性穿过 shadow 边界。",
    adoptedHeading = "一个类，一份样式表，一次 adopt",
    adoptedHint = "同类元素共用同一份已解析的样式表：静态规则不会按实例重建。",
    adoptedNote = "hover 状态必须写在样式表里，写在行内会压过规则、永远不生效。",
    hoverLabel = "把鼠标移上来",
    ladderHeading = "三级阶梯，从下往上",
    inlineStep = "行内：不随状态变化的布局——display、间距、内边距、圆角。",
    sheetStep = "组件样式表：:hover、:focus-visible、::before、@media，这些行内表达不了。",
    sharedStep = "共享样式表：reset（box-sizing、:host display）与所有组件都要用的主题 token。",
    selectorHeading = "选择器是拼出来的",
    selectorHint = "中间那枚 chip 由 `.selector-chip.is-selected` 和整行的 `[aria-current]` 两条规则共同上色——" +
        "两个键都是用零件拼的，不是贴过来的字符串。",
    selectorNote = "class 名拼错、或者以后改了名，会在这里编译失败；不会变成一条悄悄永不匹配的规则。",
    chipPlain = "普通",
    chipSelected = "选中",
)

private val stylingTexts: Translations<StylingTexts> = texts(
    Langs.EN to stylingEnglish,
    Langs.ZH_HANS to stylingChinese,
)

/**
 * The one example where a class and a sheet are genuinely required: `:hover` cannot be inline, and a
 * per-instance `style` would outrank the hover rule.
 */
internal class StylingPage : DemoPage() {

    private val texts = Binder(stylingTexts)

    private val hoverBox = newHoverBox()

    private val selectorStrip = newSelectorStrip()


    init {
        val adopted = sectionBlock()
        texts.bind(adopted.heading, { it.adoptedHeading })
        texts.bind(adopted.hint, { it.adoptedHint })
        adopted.body.append(
            exampleBox(null, hoverBox),
            texts.bind(paragraph(), { it.adoptedNote }),
            row(codeText("attachStyledShadow(RESET, BOX_SHEET)")),
        )

        val ladder = sectionBlock()
        texts.bind(ladder.heading, { it.ladderHeading })
        ladder.body.append(
            exampleBox(null, texts.bind(paragraph(), { it.inlineStep })),
            exampleBox(null, texts.bind(paragraph(), { it.sheetStep })),
            exampleBox(null, texts.bind(paragraph(), { it.sharedStep })),
        )

        val selector = sectionBlock()
        texts.bind(selector.heading, { it.selectorHeading })
        texts.bind(selector.hint, { it.selectorHint })
        selector.body.append(
            exampleBox(null, selectorStrip),
            texts.bind(paragraph(), { it.selectorNote }),
            row(codeText("compound(className(CHIP), className(SELECTED))")),
            row(codeText("className(ROW) child attributeSelector(\"aria-current\")")),
        )

        body.append(adopted.root, ladder.root, selector.root)
    }

    override fun applyTranslation(lang: Lang) {
        val current = stylingTexts[lang]
        setTitle(current.title)
        setSummary(current.summary)
        texts.apply(lang)
        hoverBox.labelText = current.hoverLabel
        selectorStrip.setLabels(current.chipPlain, current.chipSelected)
    }

    companion object : CustomElementReference<StylingPage>(StylingPage::class.js) {
        val NAME: TagName<StylingPage> = TagName("demo-styling-page")

        fun define() {
            defineIfMissing(NAME, this)
        }
    }
}

/** Creates a defined `<demo-styling-page>`. */
internal fun newStylingPage(): StylingPage {
    StylingPage.define()
    return StylingPage.NAME()
}
