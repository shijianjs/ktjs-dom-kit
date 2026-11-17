package io.github.shijianjs.ktjs.dom.demo.page.attribute

import io.github.shijianjs.ktjs.demo.Lang
import io.github.shijianjs.ktjs.demo.Page
import io.github.shijianjs.ktjs.demo.Readout
import io.github.shijianjs.ktjs.demo.codeText
import io.github.shijianjs.ktjs.demo.exampleBox
import io.github.shijianjs.ktjs.demo.newPage
import io.github.shijianjs.ktjs.demo.paragraph
import io.github.shijianjs.ktjs.demo.row
import io.github.shijianjs.ktjs.demo.sectionBlock
import io.github.shijianjs.ktjs.demo.withTheme
import io.github.shijianjs.ktjs.dom.demo.page.Binder
import io.github.shijianjs.ktjs.vaadin.button.createButton
import web.dom.clickEvent
import web.events.addHandler

private class AttributeTexts(
    val title: String,
    val summary: String,
    val demoHeading: String,
    val demoHint: String,
    val incrementLabel: String,
    val externalLabel: String,
    val demoNote: String,
    val declaredHeading: String,
    val declaredHint: String,
    val declaredNote: String,
)

private val attributeEnglish: AttributeTexts = AttributeTexts(
    title = "Attribute-backed properties",
    summary = "attr turns an HTML attribute into a typed Kotlin property: the getter decodes, the setter " +
        "encodes, and the change handler runs on both paths.",
    demoHeading = "One attribute, three ways in",
    demoHint = "The value on screen is the attribute, not a copy kept in a field.",
    incrementLabel = "step + 1",
    externalLabel = "setAttribute from outside",
    demoNote = "Both buttons end in the same place: the attribute changes, the handler repaints, the getter reads back.",
    declaredHeading = "Declared once",
    declaredHint = "The default, the codec and the reaction to a change all live in one delegation.",
    declaredNote = "A different codec changes the property type, not the attribute name; nothing else in the component moves.",
)

private val attributeChinese: AttributeTexts = AttributeTexts(
    title = "属性绑定",
    summary = "attr 把 HTML 属性变成类型化的 Kotlin 属性：getter 解码、setter 编码，变化回调两条路径都会触发。",
    demoHeading = "一个属性，三种入口",
    demoHint = "界面上的值是属性本身，不是字段里另存的一份副本。",
    incrementLabel = "step + 1",
    externalLabel = "外部 setAttribute",
    demoNote = "两个按钮殊途同归：属性变化、回调重绘、getter 读回同一个值。",
    declaredHeading = "只声明一处",
    declaredHint = "默认值、codec 和变化反应都在同一个委托里。",
    declaredNote = "换一个 codec 只改变属性类型，不改变属性名，组件里其他地方一行都不用动。",
)

private fun attributeTexts(lang: Lang): AttributeTexts = if (lang == Lang.EN) attributeEnglish else attributeChinese

/**
 * `attr { }`: a property backed by an HTML attribute. The demo drives the same attribute through two
 * paths — the property setter and a raw `setAttribute` — and shows that both land in one handler.
 */
internal class AttributePage : Page {
    private val page = newPage()

    private val texts = Binder(::attributeTexts)

    private val badge = newStepBadge()

    private val readout = Readout()

    override val node get() = page

    init {
        val demo = sectionBlock()
        texts.bind(demo.title, { it.demoHeading })
        texts.bind(demo.hint, { it.demoHint })

        val increment = texts.bind(createButton(), { it.incrementLabel }).withTheme("primary")
        increment.clickEvent.addHandler {
            badge.step += 1
            showValue()
        }

        val external = texts.bind(createButton(), { it.externalLabel }).withTheme("tertiary")
        external.clickEvent.addHandler {
            badge.setAttribute("step", "10")
            showValue()
        }

        demo.body.append(
            exampleBox(
                null,
                row(badge, increment, external),
                texts.bind(paragraph(), { it.demoNote }),
                readout.root,
            ),
        )

        val declared = sectionBlock()
        texts.bind(declared.title, { it.declaredHeading })
        texts.bind(declared.hint, { it.declaredHint })
        declared.body.append(
            exampleBox(null, row(codeText("var step: Int by attr(default = 1) { change -> /* repaint */ }")), texts.bind(paragraph(), { it.declaredNote })),
        )

        page.body.append(demo.root, declared.root)
        showValue()
    }

    override fun applyTranslation(lang: Lang) {
        val current = attributeTexts(lang)
        page.setTitle(current.title)
        page.setSummary(current.summary)
        texts.apply(lang)
    }

    private fun showValue() {
        readout.set("getter reads step = ${badge.step}, attribute says \"${badge.getAttribute("step")}\"")
    }
}
