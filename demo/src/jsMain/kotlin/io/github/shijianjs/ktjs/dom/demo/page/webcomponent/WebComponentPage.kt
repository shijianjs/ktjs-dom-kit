package io.github.shijianjs.ktjs.dom.demo.page.webcomponent

import io.github.shijianjs.ktjs.demo.bilingual
import io.github.shijianjs.ktjs.demo.codeText
import io.github.shijianjs.ktjs.demo.element
import io.github.shijianjs.ktjs.demo.exampleBox
import io.github.shijianjs.ktjs.demo.paragraph
import io.github.shijianjs.ktjs.demo.row
import io.github.shijianjs.ktjs.demo.sectionBlock
import io.github.shijianjs.ktjs.dom.demo.page.DemoTextPage
import web.html.HtmlTagName

/**
 * `CustomElementAdapter` + `defineIfMissing`: a class that is the component, defined once.
 *
 * Two instances are shown side by side: they share the class-level sheet, and each keeps its own
 * count — state is per element, rules are per class.
 */
internal class WebComponentPage : DemoTextPage(
    bilingual("自定义元素", "Custom elements"),
    bilingual(
        "dom-kit 的基类把生命周期钩子声明成方法、注册前剥掉编译器桥，于是 Kotlin 类就是一个 Web Component。",
        "dom-kit's base class declares the lifecycle hooks as methods and strips the compiler's bridges " +
            "before defining the tag, so a Kotlin class simply is a Web Component.",
    ),
) {
    init {
        val instances = sectionBlock()
        texts.bind(instances.title, bilingual("两个实例", "Two instances"))
        texts.bind(
            instances.hint,
            bilingual(
                "左边每次 +1，右边每次 +3（step 属性）；计数各自独立，样式表一份。",
                "The left one adds 1 per click, the right one adds 3 (the step attribute); counts are " +
                    "independent, the sheet is shared.",
            ),
        )
        instances.body.append(
            exampleBox(
                null,
                row(newCounter(), newCounter(step = 3)),
                paragraph("Click either button: the label it owns changes, nothing else is touched."),
            ),
        )

        val definition = sectionBlock()
        texts.bind(definition.title, bilingual("定义与注册", "Defining and registering"))
        definition.body.append(
            exampleBox(
                null,
                row(codeText("class DemoCounter : CustomElementAdapter() { … }")),
                row(codeText("defineIfMissing(TagName(\"demo-counter\"), DemoCounter)")),
                element(HtmlTagName.p).apply {
                    textContent =
                        "ES2015 output is required: at ES5 a Kotlin class compiles to a function that calls " +
                            "HTMLElement as a function, and the tag never upgrades."
                },
            ),
        )

        page.body.append(instances.root, definition.root)
    }
}
