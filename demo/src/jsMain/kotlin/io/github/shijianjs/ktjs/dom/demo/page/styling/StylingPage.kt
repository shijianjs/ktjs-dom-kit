package io.github.shijianjs.ktjs.dom.demo.page.styling

import io.github.shijianjs.ktjs.demo.Lang
import io.github.shijianjs.ktjs.demo.Page
import io.github.shijianjs.ktjs.demo.codeText
import io.github.shijianjs.ktjs.demo.exampleBox
import io.github.shijianjs.ktjs.demo.newPage
import io.github.shijianjs.ktjs.demo.paragraph
import io.github.shijianjs.ktjs.demo.row
import io.github.shijianjs.ktjs.demo.sectionBlock
import io.github.shijianjs.ktjs.dom.demo.page.Binder

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
)

private val stylingEnglish: StylingTexts = StylingTexts(
    title = "Stylesheets",
    summary = "A component carries its own sheet and adopts it once; the page and the shell each carry theirs. " +
        "Nothing is adopted on document, and colours come through as inherited custom properties.",
    adoptedHeading = "A class, a sheet, one adopt",
    adoptedHint = "Two elements of the same kind share one parsed sheet: static rules are not rebuilt per instance.",
    adoptedNote = "The hover state lives in the sheet because an inline declaration would outrank the rule.",
    hoverLabel = "Hover me",
    ladderHeading = "Three levels, from the bottom up",
    inlineStep = "Inline: layout that never changes with state — display, gaps, padding, radius.",
    sheetStep = "Component sheet: :hover, :focus-visible, ::before, @media — what inline cannot express.",
    sharedStep = "Shared sheet: the reset (box-sizing, :host display) and the theme tokens every component needs.",
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
)

private fun stylingTexts(lang: Lang): StylingTexts = if (lang == Lang.EN) stylingEnglish else stylingChinese

/**
 * The one example where a class and a sheet are genuinely required: `:hover` cannot be inline, and a
 * per-instance `style` would outrank the hover rule.
 */
internal class StylingPage : Page {
    private val page = newPage()

    private val texts = Binder(::stylingTexts)

    private val hoverBox = newHoverBox()

    override val node get() = page

    init {
        val adopted = sectionBlock()
        texts.bind(adopted.title, { it.adoptedHeading })
        texts.bind(adopted.hint, { it.adoptedHint })
        adopted.body.append(
            exampleBox(null, hoverBox),
            texts.bind(paragraph(), { it.adoptedNote }),
            row(codeText("attachStyledShadow(RESET, BOX_SHEET)")),
        )

        val ladder = sectionBlock()
        texts.bind(ladder.title, { it.ladderHeading })
        ladder.body.append(
            exampleBox(null, texts.bind(paragraph(), { it.inlineStep })),
            exampleBox(null, texts.bind(paragraph(), { it.sheetStep })),
            exampleBox(null, texts.bind(paragraph(), { it.sharedStep })),
        )

        page.body.append(adopted.root, ladder.root)
    }

    override fun applyTranslation(lang: Lang) {
        val current = stylingTexts(lang)
        page.setTitle(current.title)
        page.setSummary(current.summary)
        texts.apply(lang)
        hoverBox.labelText = current.hoverLabel
    }
}
