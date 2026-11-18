package io.github.shijianjs.ktjs.dom.demo.page.overview

import io.github.shijianjs.ktjs.dom.element.invoke
import io.github.shijianjs.ktjs.dom.element.unaryPlus
import io.github.shijianjs.ktjs.demo.i18n.Binder
import io.github.shijianjs.ktjs.demo.i18n.Lang
import io.github.shijianjs.ktjs.demo.i18n.Langs
import io.github.shijianjs.ktjs.demo.nav.Page
import io.github.shijianjs.ktjs.demo.i18n.Translations
import io.github.shijianjs.ktjs.demo.page.anchor
import io.github.shijianjs.ktjs.demo.page.codeText
import io.github.shijianjs.ktjs.demo.page.newPage
import io.github.shijianjs.ktjs.demo.page.paragraph
import io.github.shijianjs.ktjs.demo.page.row
import io.github.shijianjs.ktjs.demo.page.sectionBlock
import io.github.shijianjs.ktjs.demo.i18n.texts
import io.github.shijianjs.ktjs.dom.demo.API_URL
import web.html.HtmlTagName

/** Every string this page shows, in one language. */
private class OverviewTexts(
    val title: String,
    val summary: String,
    val elementsLink: String,
    val customElementsLink: String,
    val apiLink: String,
    val purposeHeading: String,
    val body: String,
    val bullets: List<String>,
    val coordinatesHeading: String,
    val coordinatesNote: String,
    val sourceLabel: String,
    val nextHeading: String,
    val nextBody: String,
)

private val overviewEnglish: OverviewTexts = OverviewTexts(
    title = "ktjs-dom-kit overview",
    summary = "A small set of Kotlin/JS helpers for writing native DOM: typed tags, attribute-backed " +
        "properties, adopted stylesheets and a custom-element adapter.",
    elementsLink = "Elements",
    customElementsLink = "Custom elements",
    apiLink = "API reference",
    purposeHeading = "What it solves",
    body = "Kotlin/JS already exposes the DOM, but the plain wrappers leave three gaps: a tag name is a " +
        "string, so the created node stays Element; an attribute is text, so every numeric property parses " +
        "by hand; and a Kotlin class only becomes a working custom element when the module targets ES2015 " +
        "and the compiler's bridges are stripped before the tag is defined.",
    bullets = listOf(
        "HtmlTagName.p { } / +HtmlTagName.li { } — the tag decides the type, no magic strings.",
        "attr { } — a property backed by an HTML attribute, decoded by a codec.",
        "cssSheet { } — a typed stylesheet adopted by a shadow root.",
        "CustomElementAdapter + defineIfMissing — a class is a component, bridges removed.",
    ),
    coordinatesHeading = "Coordinates",
    coordinatesNote = "This site is built with the library itself, so the pages double as its integration tests.",
    sourceLabel = "Source repository",
    nextHeading = "Reading order",
    nextBody = "Typed elements first, then attributes, then stylesheets, then custom elements.",
)

private val overviewChinese: OverviewTexts = OverviewTexts(
    title = "ktjs-dom-kit 概览",
    summary = "用类型安全的 Kotlin/JS 写原生 DOM 的一套小工具：标签收窄、属性绑定、adopted 样式表与自定义元素基类。",
    elementsLink = "元素创建",
    customElementsLink = "自定义元素",
    apiLink = "API 文档",
    purposeHeading = "它解决什么问题",
    body = "Kotlin/JS 已经暴露了 DOM，但裸封装留下三个缺口：标签名是字符串，建出来的节点仍是 Element；属性是文本，" +
        "每个数值属性都要手工解析；而一个 Kotlin 类要真正成为可用的自定义元素，模块必须发射 ES2015，且注册前剥掉编译器桥。",
    bullets = listOf(
        "HtmlTagName.p { } / +HtmlTagName.li { } —— 标签决定类型，不写魔法字符串。",
        "attr { } —— 属性即属性，由 codec 解码。",
        "cssSheet { } —— 类型化样式表，交给 shadow root adopt。",
        "CustomElementAdapter + defineIfMissing —— 一个类就是一个组件，桥已在注册前剥掉。",
    ),
    coordinatesHeading = "坐标",
    coordinatesNote = "这个站点本身就用这个库搭建，页面同时充当它的集成测试。",
    sourceLabel = "源码仓库",
    nextHeading = "建议顺序",
    nextBody = "先看元素创建，再看属性绑定、样式表，最后是自定义元素。",
)

private val overviewTexts: Translations<OverviewTexts> = texts(
    Langs.EN to overviewEnglish,
    Langs.ZH_HANS to overviewChinese,
)

/**
 * Landing page: what the library is, how it is consumed, where the reference lives.
 *
 * The sections below are plain blocks, not components: they have nothing to swap at run time, so a
 * component per section would only add indirection.
 */
internal class OverviewPage : Page {
    private val page = newPage()

    private val texts = Binder(overviewTexts)

    override val node get() = page

    init {
        page.addLink(texts.bind(anchor("", "#/elements"), { it.elementsLink }))
        page.addLink(texts.bind(anchor("", "#/custom-elements"), { it.customElementsLink }))
        page.addLink(texts.bind(anchor("", API_URL, external = true), { it.apiLink }))

        val purpose = sectionBlock()
        texts.bind(purpose.title, { it.purposeHeading })

        val bullets = HtmlTagName.ul {
            List(4) { index ->
                texts.bind(+HtmlTagName.li()) { it.bullets.getOrNull(index).orEmpty() }
            }
        }
        purpose.body.append(texts.bind(paragraph(), { it.body }), bullets)

        val coordinates = sectionBlock()
        texts.bind(coordinates.title, { it.coordinatesHeading })
        texts.bind(coordinates.hint, { it.coordinatesNote })
        coordinates.body.append(
            row(codeText("implementation(\"io.github.shijianjs:ktjs-dom-kit:0.1.0\")")),
            row(texts.bind(anchor("", "https://github.com/shijianjs/ktjs-dom-kit", external = true), { it.sourceLabel })),
        )

        val next = sectionBlock()
        texts.bind(next.title, { it.nextHeading })
        next.body.append(texts.bind(paragraph(), { it.nextBody }))

        page.body.append(purpose.root, coordinates.root, next.root)
    }

    override fun applyTranslation(lang: Lang) {
        val current = overviewTexts[lang]
        page.setTitle(current.title)
        page.setSummary(current.summary)
        texts.apply(lang)
    }
}
