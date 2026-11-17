package io.github.shijianjs.ktjs.dom.demo.page.attribute

import io.github.shijianjs.ktjs.demo.Readout
import io.github.shijianjs.ktjs.demo.bilingual
import io.github.shijianjs.ktjs.demo.codeText
import io.github.shijianjs.ktjs.demo.exampleBox
import io.github.shijianjs.ktjs.demo.paragraph
import io.github.shijianjs.ktjs.demo.row
import io.github.shijianjs.ktjs.demo.sectionBlock
import io.github.shijianjs.ktjs.demo.withTheme
import io.github.shijianjs.ktjs.dom.demo.page.DemoTextPage
import io.github.shijianjs.ktjs.vaadin.button.createButton
import web.dom.clickEvent
import web.events.addHandler

/**
 * `attr { }`: a property that *is* an HTML attribute.
 *
 * The demo drives the badge through the Kotlin property and through `setAttribute` alike, and reads
 * it back through the property — three paths, one value.
 */
internal class AttributePage : DemoTextPage(
    bilingual("属性绑定", "Attribute-backed properties"),
    bilingual(
        "attr 让属性成为唯一状态：Kotlin 侧写、外部 setAttribute 写、读回，三条路看到同一个值。",
        "attr makes the attribute the only state: a Kotlin write, an external setAttribute and a read-back " +
            "all see the same value.",
    ),
) {
    private val badge: StepBadge = newStepBadge()

    private val readout = Readout()

    init {
        val value = sectionBlock()
        texts.bind(value.title, bilingual("一个值，三个入口", "One value, three entry points"))
        texts.bind(
            value.hint,
            bilingual(
                "按钮分别走 Kotlin 属性、external setAttribute，readout 用属性 getter 读回。",
                "The buttons use the Kotlin property and an external setAttribute; the readout reads back " +
                    "through the property getter.",
            ),
        )

        val increment = createButton { textContent = "+1" }.withTheme("primary")
        increment.clickEvent.addHandler {
            badge.step = badge.step + 1
            readout.set("getter: ${badge.step}")
        }

        val external = createButton { textContent = "setAttribute(\"step\", \"10\")" }
        external.clickEvent.addHandler {
            badge.setAttribute("step", "10")
            readout.set("getter: ${badge.step}")
        }

        value.body.append(
            exampleBox(null, row(badge), row(increment, external), readout.root),
        )

        val declared = sectionBlock()
        texts.bind(declared.title, bilingual("声明的形状", "How it is declared"))
        declared.body.append(
            exampleBox(
                null,
                row(codeText("var step: Int by attr(default = 1) { change -> label.textContent = … }")),
                paragraph(
                    "The default is what the getter returns while the attribute is absent; it is never " +
                        "written into the markup, and removing the attribute goes back to it.",
                ),
            ),
        )

        page.body.append(value.root, declared.root)
    }
}
