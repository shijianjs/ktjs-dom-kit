package io.github.shijianjs.ktjs.dom.demo.page.styling

import io.github.shijianjs.ktjs.demo.bilingual
import io.github.shijianjs.ktjs.demo.codeText
import io.github.shijianjs.ktjs.demo.exampleBox
import io.github.shijianjs.ktjs.demo.paragraph
import io.github.shijianjs.ktjs.demo.row
import io.github.shijianjs.ktjs.demo.sectionBlock
import io.github.shijianjs.ktjs.dom.demo.page.DemoTextPage

/**
 * `cssSheet { }`: styles written as typed Kotlin, adopted by a shadow root.
 *
 * The page itself adopts only the page sheet; the box below adopts its own sheet, which is where
 * `:hover` has to live.
 */
internal class StylingPage : DemoTextPage(
    bilingual("样式表", "Stylesheets"),
    bilingual(
        "cssSheet 用类型化的属性名写规则，结果是一份 CSSStyleSheet，由组件的 shadow root adopt。",
        "cssSheet writes rules with typed property names and yields a CSSStyleSheet that a component's " +
            "shadow root adopts.",
    ),
) {
    init {
        val adopted = sectionBlock()
        texts.bind(adopted.title, bilingual("组件自己的样式表", "A component's own sheet"))
        texts.bind(
            adopted.hint,
            bilingual(
                "下面这个盒子把 :hover 放在自己的 sheet 里；鼠标移上去颜色会变。",
                "The box below keeps its :hover rule in its own sheet; hover it and the colour changes.",
            ),
        )
        adopted.body.append(
            exampleBox(
                null,
                row(newHoverBox("Hover me")),
                row(codeText(".hover-box:hover { backgroundColor = Palette.surfaceRaised }")),
                paragraph(
                    "An inline background would win over the pseudo-class, which is why hover state and " +
                        "its base value live in the same rule set.",
                ),
            ),
        )

        val ladder = sectionBlock()
        texts.bind(ladder.title, bilingual("静态与动态的分界", "Static versus dynamic"))
        ladder.body.append(
            exampleBox(
                null,
                paragraph("Static layout belongs in the sheet, keyed by a semantic class."),
                paragraph("Only values that change per instance or per interaction are written inline."),
                row(codeText("inlineStyle.apply { backgroundColor = … }")),
            ),
        )

        page.body.append(adopted.root, ladder.root)
    }
}
