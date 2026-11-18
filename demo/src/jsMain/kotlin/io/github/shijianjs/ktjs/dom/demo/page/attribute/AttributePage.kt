package io.github.shijianjs.ktjs.dom.demo.page.attribute

import io.github.shijianjs.ktjs.dom.attribute.Attribute
import io.github.shijianjs.ktjs.dom.attribute.AttributeWatch
import io.github.shijianjs.ktjs.dom.attribute.attribute
import io.github.shijianjs.ktjs.dom.attribute.literalAttribute
import io.github.shijianjs.ktjs.demo.i18n.Binder
import io.github.shijianjs.ktjs.demo.i18n.Lang
import io.github.shijianjs.ktjs.demo.i18n.Langs
import io.github.shijianjs.ktjs.demo.i18n.Translations
import io.github.shijianjs.ktjs.demo.i18n.texts
import io.github.shijianjs.ktjs.demo.nav.Page
import io.github.shijianjs.ktjs.demo.page.Readout
import io.github.shijianjs.ktjs.demo.page.SectionBlock
import io.github.shijianjs.ktjs.demo.page.codeText
import io.github.shijianjs.ktjs.demo.page.exampleBox
import io.github.shijianjs.ktjs.demo.page.newPage
import io.github.shijianjs.ktjs.demo.page.paragraph
import io.github.shijianjs.ktjs.demo.page.row
import io.github.shijianjs.ktjs.demo.page.sectionBlock
import io.github.shijianjs.ktjs.dom.demo.wrapper.themeAttr
import io.github.shijianjs.ktjs.vaadin.button.createButton
import io.github.shijianjs.ktjs.vaadin.themablemixin.ThemePropertyMixin
import io.github.shijianjs.ktjs.vaadin.themablemixin.addTheme
import io.github.shijianjs.ktjs.vaadin.button.ButtonVariants
import io.github.shijianjs.ktjs.vaadin.theme.name
import web.dom.clickEvent
import web.events.addHandler
import web.html.HTMLElement
import js.reflect.unsafeCast

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
    val wrapperHeading: String,
    val wrapperHint: String,
    val wrapperToggleLabel: String,
    val wrapperRawLabel: String,
    val wrapperNote: String,
    val typedHeading: String,
    val typedHint: String,
    val labelSetLabel: String,
    val labelClearLabel: String,
    val densityLabel: String,
    val typedNote: String,
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
    wrapperHeading = "Someone else's element",
    wrapperHint = "A wrapper element is an external class: give it an extension property that hands out the " +
        "attribute object, then operate on that object.",
    wrapperToggleLabel = "toggle primary",
    wrapperRawLabel = "write messy text",
    wrapperNote = "The attribute is the state. Write it from anywhere — here with a deliberately messy " +
        "setAttribute — and the next read already sees the normalised list.",
    typedHeading = "Values a wrapper actually needs",
    typedHint = "An absent attribute is not an empty one, and a closed vocabulary beats a bare string.",
    labelSetLabel = "label: Close",
    labelClearLabel = "clear label",
    densityLabel = "density: comfortable",
    typedNote = "aria-label absent reads as null while \"\" stays \"\"; the density vocabulary comes " +
        "from a table, so a value outside it cannot be compiled; and observe fires for the click and " +
        "for any raw setAttribute alike.",
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
    wrapperHeading = "别人的元素",
    wrapperHint = "wrapper 元素是 external 类：给它一个扩展属性、把属性对象交出来，然后在这个对象上操作。",
    wrapperToggleLabel = "切换 primary",
    wrapperRawLabel = "写入脏文本",
    wrapperNote = "属性就是状态。谁都能写——这里故意用 setAttribute 写一段脏文本，下一次读取已经是规整的列表。",
    typedHeading = "wrapper 真正需要的取值能力",
    typedHint = "属性缺失不等于空串，封闭词表也比裸字符串靠谱。",
    labelSetLabel = "设置 label",
    labelClearLabel = "清除 label",
    densityLabel = "密度：comfortable",
    typedNote = "aria-label 缺失读作 null，而 \"\" 仍然是 \"\"；密度词表来自一张表，表外取值根本编译不过；" +
        "observe 对点击和任何裸 setAttribute 一视同仁。",
)

private val attributeTexts: Translations<AttributeTexts> = texts(
    Langs.EN to attributeEnglish,
    Langs.ZH_HANS to attributeChinese,
)

/** A wrapper's vocabulary: upstream literals are `sealed external interface` values, not strings. */
private sealed external interface Density

private fun Density(token: String): Density = token.unsafeCast<Density>()

private object Densities {
    val compact: Density = Density("compact")
    val comfortable: Density = Density("comfortable")
    val all: List<Density> = listOf(compact, comfortable)
}

private val Density.token: String get() = unsafeCast<String>()

/** `aria-label` is optional on every wrapper element: absent must stay distinguishable from `""`. */
private val HTMLElement.ariaLabelAttr: Attribute<String> get() = attribute("aria-label")

/** The typed form of a literal attribute: `literalAttribute` rejects anything outside [Densities.all]. */
private val HTMLElement.densityAttr: Attribute<Density>
    get() = literalAttribute("data-density", Densities.all) { it.token }

/**
 * `attr { }`: a property backed by an HTML attribute. The demo drives the same attribute through two
 * paths — the property setter and a raw `setAttribute` — and shows that both land in one handler.
 */
internal class AttributePage : Page {
    private val page = newPage()

    private val texts = Binder(attributeTexts)

    private val badge = newStepBadge()

    private val readout = Readout()

    private val wrapperReadout = Readout()

    private val typedReadout = Readout()

    /**
     * The `aria-label` subscription of the typed section. It lives as long as the page, which is the
     * lifetime of the element it watches, so it is held here instead of being disconnected.
     */
    private var typedWatch: AttributeWatch? = null

    override val node get() = page

    init {
        val demo = sectionBlock()
        texts.bind(demo.title, { it.demoHeading })
        texts.bind(demo.hint, { it.demoHint })

        val increment = texts.bind(createButton(), { it.incrementLabel }).addTheme(ButtonVariants.primary)
        increment.clickEvent.addHandler {
            badge.step += 1
            showValue()
        }

        val external = texts.bind(createButton(), { it.externalLabel }).addTheme(ButtonVariants.tertiary)
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

        val wrapper = wrapperSection()

        val typed = typedSection()

        page.body.append(demo.root, declared.root, wrapper.root, typed.root)
        showValue()
    }

    override fun applyTranslation(lang: Lang) {
        val current = attributeTexts[lang]
        page.setTitle(current.title)
        page.setSummary(current.summary)
        texts.apply(lang)
    }

    private fun showValue() {
        val raw = badge.getAttribute("step")
        readout.set("getter reads step = ${badge.step}, attribute says ${if (raw == null) "missing" else "\"$raw\""}")
    }

    /**
     * A `vaadin-button` from the wrapper repository: an `external` class the kit has no say over.
     * The attribute is still the whole state — one button toggles a token through
     * [io.github.shijianjs.ktjs.dom.attribute.TokenListAttribute], the other writes messy text with a
     * raw `setAttribute`, and both land in the same readout.
     */
    private fun wrapperSection(): SectionBlock {
        val section = sectionBlock()
        texts.bind(section.title, { it.wrapperHeading })
        texts.bind(section.hint, { it.wrapperHint })

        val target = createButton().apply { textContent = "vaadin-button" }

        val toggle = texts.bind(createButton(), { it.wrapperToggleLabel }).addTheme(ButtonVariants.primary)
        toggle.clickEvent.addHandler {
            target.themeAttr.toggle(ButtonVariants.primary.name)
            showWrapper(target)
        }

        val raw = texts.bind(createButton(), { it.wrapperRawLabel }).addTheme(ButtonVariants.tertiary)
        raw.clickEvent.addHandler {
            target.setAttribute("theme", "  primary   small primary ")
            showWrapper(target)
        }

        section.body.append(
            exampleBox(
                null,
                row(target, toggle, raw),
                texts.bind(paragraph(), { it.wrapperNote }),
                wrapperReadout.root,
            ),
        )
        showWrapper(target)
        return section
    }

    private fun showWrapper(target: ThemePropertyMixin) {
        val theme = target.themeAttr
        val raw = theme.raw
        wrapperReadout.set("theme = ${theme.values}, attribute = ${if (raw == null) "missing" else "\"$raw\""}")
    }

    /**
     * The three things a wrapper needs on top of "read and write a string": an optional attribute that
     * distinguishes absent from empty, a literal attribute whose values come from a table, and a
     * subscription that reports writes the component did not make.
     *
     * The readout is repainted from the subscription itself, so the count only moves when the
     * browser really delivered a change: `observe` is a `MutationObserver`, not a hook on the setter.
     */
    private fun typedSection(): SectionBlock {
        val section = sectionBlock()
        texts.bind(section.title, { it.typedHeading })
        texts.bind(section.hint, { it.typedHint })

        val target = createButton().apply { textContent = "vaadin-button" }
        var observed = 0

        typedWatch = target.ariaLabelAttr.observe {
            observed += 1
            showTyped(target, observed)
        }

        val setLabel = texts.bind(createButton(), { it.labelSetLabel }).addTheme(ButtonVariants.primary)
        setLabel.clickEvent.addHandler { target.ariaLabelAttr.value = "Close" }

        val clearLabel = texts.bind(createButton(), { it.labelClearLabel }).addTheme(ButtonVariants.tertiary)
        clearLabel.clickEvent.addHandler { target.ariaLabelAttr.setOrClear(null) }

        val density = texts.bind(createButton(), { it.densityLabel }).addTheme(ButtonVariants.tertiary)
        density.clickEvent.addHandler {
            target.densityAttr.value = Densities.comfortable
            showTyped(target, observed)
        }

        section.body.append(
            exampleBox(
                null,
                row(target, setLabel, clearLabel, density),
                texts.bind(paragraph(), { it.typedNote }),
                typedReadout.root,
            ),
        )
        showTyped(target, observed)
        return section
    }

    private fun showTyped(target: HTMLElement, observed: Int) {
        val label = target.ariaLabelAttr.valueOrNull?.let { "\"$it\"" } ?: "null"
        typedReadout.set(
            "aria-label = $label, data-density = ${target.densityAttr.value.token}, observed = $observed",
        )
    }
}
