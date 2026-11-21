package io.github.shijianjs.ktjs.dom.demo.page.webcomponent

import io.github.shijianjs.ktjs.dom.element.invoke
import io.github.shijianjs.ktjs.demo.i18n.Binder
import io.github.shijianjs.ktjs.demo.i18n.Lang
import io.github.shijianjs.ktjs.demo.i18n.Langs
import io.github.shijianjs.ktjs.demo.page.DemoPage
import io.github.shijianjs.ktjs.demo.i18n.Translations
import io.github.shijianjs.ktjs.demo.page.Blocks.codeText
import io.github.shijianjs.ktjs.demo.page.Blocks.exampleBox
import io.github.shijianjs.ktjs.demo.page.Blocks.paragraph
import io.github.shijianjs.ktjs.demo.page.Blocks.row
import io.github.shijianjs.ktjs.demo.page.SectionBlock
import io.github.shijianjs.ktjs.demo.i18n.texts
import io.github.shijianjs.ktjs.dom.demo.page.DemoCustomElements
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
            row(codeText("companion object : DemoCounterDefaultReference() // generated: demo-counter")),
            row(codeText("val counter: DemoCounter = DemoCounter.NAME() // registers, then creates")),
            texts.bind(paragraph(), { it.definitionNote }),
        )

        body.append(instances.root, definition.root, registrySection().root)
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

    /**
     * The list `ktjs-dom-kit-ksp` writes after its last round: every element of this module, in one
     * object. Reading it initialises all of their references, and a reference defines its tag in
     * `init` — so this page shows every tag the demo ships, not only the ones it has touched.
     */
    private fun registrySection(): SectionBlock {
        val section = SectionBlock.NAME { show() }
        texts.bind(section.heading, { it.registryHeading })
        texts.bind(section.hint, { it.registryHint })

        section.body.append(
            row(codeText("DemoCustomElements.TAGS.size // ${DemoCustomElements.TAGS.size}")),
            row(codeText(DemoCustomElements.TAGS.joinToString(" ") { "<$it>" })),
            texts.bind(paragraph(), { it.registryNote }),
        )
        return section
    }

    companion object : WebComponentPageDefaultReference() {

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
            registryHeading = "What this module ships",
            registryHint = "One generated object lists every element, so nothing has to be registered by hand.",
            registryNote = "Reading the list initialises each element's reference, and a reference defines its tag " +
                "in its init: one line defines everything the module ships, pages it has never visited included.",
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
            registryHeading = "这个模块都发什么",
            registryHint = "生成的一个对象列全了所有元素，一个都不用手工注册。",
            registryNote = "读这个列表会初始化每个元素的 reference，而 reference 在自己的 init 里定义标签：" +
                "一行就定义了模块里的全部标签，包括这个站点还没访问过的页面。",
        )
        private val webComponentTexts: Translations<WebComponentTexts> = texts(
            Langs.EN to webComponentEnglish,
            Langs.ZH_HANS to webComponentChinese,
        )
    }
}
