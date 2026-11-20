package io.github.shijianjs.ktjs.dom.demo.page.webcomponent

import io.github.shijianjs.ktjs.dom.element.invoke
import io.github.shijianjs.ktjs.demo.i18n.Binder
import io.github.shijianjs.ktjs.demo.i18n.Lang
import io.github.shijianjs.ktjs.demo.i18n.Langs
import io.github.shijianjs.ktjs.demo.page.DemoPage
import io.github.shijianjs.ktjs.dom.webcomponent.defineIfMissing
import io.github.shijianjs.ktjs.dom.webcomponent.CustomElementReferenceAdapter
import io.github.shijianjs.ktjs.demo.i18n.Translations
import io.github.shijianjs.ktjs.demo.page.Blocks.codeText
import io.github.shijianjs.ktjs.demo.page.Blocks.exampleBox
import io.github.shijianjs.ktjs.demo.page.Blocks.paragraph
import io.github.shijianjs.ktjs.demo.page.Blocks.row
import io.github.shijianjs.ktjs.demo.page.SectionBlock
import io.github.shijianjs.ktjs.demo.i18n.texts
import io.github.shijianjs.ktjs.vaadin.button.createButton
import web.dom.clickEvent
import web.events.addHandler
import web.html.HtmlTagName

/**
 * Shows custom elements as they are actually used: one tag, several instances, each with its own
 * state and a shared parsed sheet.
 */
internal class WebComponentPage : DemoPage() {

    private val texts = Binder(webComponentTexts)

    private val instanceHost = HtmlTagName.div()

    private val counters: MutableList<DemoCounter> = mutableListOf()

    private var incrementLabel: String = webComponentEnglish.incrementLabel

    init {
        val instances = SectionBlock.NAME { show() }
        texts.bind(instances.heading, { it.instancesHeading })
        texts.bind(instances.hint, { it.instancesHint })

        val addInstance = texts.bind(createButton(), { it.addInstanceLabel })
        addInstance.clickEvent.addHandler { appendCounter() }

        appendCounter()
        instances.body.append(
            exampleBox(null, addInstance, instanceHost, texts.bind(paragraph(), { it.instancesNote })),
        )

        val definition = SectionBlock.NAME { show() }
        texts.bind(definition.heading, { it.definitionHeading })
        texts.bind(definition.hint, { it.definitionHint })
        definition.body.append(
            row(codeText("js { compilerOptions { target = \"es2015\" } }")),
            row(codeText("defineIfMissing(NAME, this) // strips the bridges first")),
            texts.bind(paragraph(), { it.definitionNote }),
        )

        body.append(instances.root, definition.root)
    }

    override fun applyTranslation(lang: Lang) {
        val current = webComponentTexts[lang]
        setTitle(current.title)
        setSummary(current.summary)
        texts.apply(lang)

        incrementLabel = current.incrementLabel
        counters.forEach { it.incrementLabel = incrementLabel }
    }

    private fun appendCounter() {
        val counter = DemoCounter.NAME()
        counter.incrementLabel = incrementLabel
        counters += counter
        instanceHost.append(counter)
    }

    companion object : CustomElementReferenceAdapter<WebComponentPage>(WebComponentPage::class.js, "demo-web-component-page") {

        private val webComponentEnglish: WebComponentTexts = WebComponentTexts(
            title = "Custom elements",
            summary = "A Kotlin class becomes a tag through one adapter: the shadow tree is built in the constructor, " +
                "the tag is registered once, and the compiler's bridges are removed before defining it.",
            instancesHeading = "One tag, many instances",
            instancesHint = "Each instance has its own shadow tree; the sheet is parsed once and adopted by all.",
            addInstanceLabel = "Add instance",
            incrementLabel = "+",
            instancesNote = "Two instances never share state, and neither shares its sheet by rebuilding it.",
            definitionHeading = "What has to be in place",
            definitionHint = "Two conditions decide whether the tag upgrades at all.",
            definitionNote = "Without es2015 the compiler emits a function that cannot be constructed; without bridge " +
                "stripping the first DOM call recurses into itself.",
        )
        private val webComponentChinese: WebComponentTexts = WebComponentTexts(
            title = "自定义元素",
            summary = "一个 Kotlin 类通过一个适配基类变成一个标签：构造函数里建好 shadow 树，标签只注册一次，注册前剥掉编译器桥。",
            instancesHeading = "一个标签，多个实例",
            instancesHint = "每个实例有自己的 shadow 树；样式表只解析一次，所有实例共用。",
            addInstanceLabel = "新增实例",
            incrementLabel = "+",
            instancesNote = "两个实例从不共享状态，也不靠重建样式表来共享样式。",
            definitionHeading = "必须满足的两件事",
            definitionHint = "这两条决定标签到底会不会升级。",
            definitionNote = "没有 es2015，编译器会发射无法 new 的函数；不剥桥，第一次调用 DOM 方法就会自递归。",
        )
        private val webComponentTexts: Translations<WebComponentTexts> = texts(
            Langs.EN to webComponentEnglish,
            Langs.ZH_HANS to webComponentChinese,
        )
    }
}

/** Creates a defined `<demo-web-component-page>`. */
