package io.github.shijianjs.ktjs.dom.demo.page.overview

import io.github.shijianjs.ktjs.demo.anchor
import io.github.shijianjs.ktjs.demo.bilingual
import io.github.shijianjs.ktjs.demo.bulletList
import io.github.shijianjs.ktjs.demo.codeText
import io.github.shijianjs.ktjs.demo.paragraph
import io.github.shijianjs.ktjs.demo.row
import io.github.shijianjs.ktjs.demo.sectionBlock
import io.github.shijianjs.ktjs.dom.demo.API_URL
import io.github.shijianjs.ktjs.dom.demo.page.DemoTextPage

/**
 * Landing page: what the library is, how it is consumed, where the reference lives.
 *
 * The sections below are plain blocks, not components: they have nothing to swap at run time, so a
 * component per section would only add indirection.
 */
internal class OverviewPage : DemoTextPage(
    bilingual("ktjs-dom-kit 概览", "ktjs-dom-kit overview"),
    bilingual(
        "用类型安全的 Kotlin/JS 写原生 DOM 的一套小工具：标签收窄、属性绑定、adopted 样式表与自定义元素基类。",
        "A small set of Kotlin/JS helpers for writing native DOM: typed tags, attribute-backed properties, " +
            "adopted stylesheets and a custom-element adapter.",
    ),
) {
    init {
        page.addLink(texts.bind(anchor("", "#/elements"), bilingual("元素创建", "Elements")))
        page.addLink(texts.bind(anchor("", "#/custom-elements"), bilingual("自定义元素", "Custom elements")))
        page.addLink(texts.bind(anchor("", API_URL, external = true), bilingual("API 参考", "API reference")))

        val purpose = sectionBlock()
        texts.bind(purpose.title, bilingual("它解决什么问题", "What it solves"))
        purpose.body.append(
            paragraph(
                "Kotlin/JS already exposes the DOM, but the plain wrappers leave three gaps: a tag name is a " +
                    "string, so the created node stays `Element`; an attribute is text, so every numeric " +
                    "property parses by hand; and `class X : HTMLElement()` only upgrades if the module " +
                    "targets ES2015 and the compiler's bridges are stripped before defining the tag.",
            ),
            bulletList(
                listOf(
                    "TagName<T>.createElement { } — the tag decides the type, no magic strings.",
                    "attr { } — a property backed by an HTML attribute, decoded by a codec.",
                    "cssSheet { } — a typed stylesheet adopted by a shadow root.",
                    "CustomElementAdapter + defineIfMissing — a class is a component, bridges removed.",
                ),
            ),
        )

        val coordinates = sectionBlock()
        texts.bind(coordinates.title, bilingual("坐标", "Coordinates"))
        coordinates.body.append(
            row(codeText("implementation(\"io.github.shijianjs:ktjs-dom-kit:0.1.0\")")),
            row(
                texts.bind(
                    anchor("", "https://github.com/shijianjs/ktjs-dom-kit", external = true),
                    bilingual("源码仓库", "Source repository"),
                ),
            ),
            paragraph(
                "This site is built with the library itself, so the pages double as its integration tests.",
            ),
        )

        val next = sectionBlock()
        texts.bind(next.title, bilingual("建议顺序", "Reading order"))
        next.body.append(
            paragraph("Typed elements first, then attributes, then stylesheets, then custom elements."),
        )

        page.body.append(purpose.root, coordinates.root, next.root)
    }
}
