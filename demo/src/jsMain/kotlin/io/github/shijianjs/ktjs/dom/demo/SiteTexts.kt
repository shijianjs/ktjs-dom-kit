package io.github.shijianjs.ktjs.dom.demo

import io.github.shijianjs.ktjs.demo.Lang
import io.github.shijianjs.ktjs.demo.SiteTexts

/**
 * Every string the shell and the navigation show, in English.
 *
 * One object per language, resolved through [siteTexts]; a new language is a new object here and an
 * enum entry in `Lang`, and no existing string is touched.
 */
private val englishSite: SiteTexts = SiteTexts(
    siteTitle = "ktjs-dom-kit",
    langLabel = "EN",
    apiLabel = "API reference",
    lightLabel = "Light",
    darkLabel = "Dark",
    groupTitles = mapOf(
        "overview" to "Getting started",
        "dom" to "DOM",
        "components" to "Custom elements",
    ),
    pageTitles = mapOf(
        "overview" to "Overview",
        "elements" to "Elements",
        "attributes" to "Attributes",
        "stylesheets" to "Stylesheets",
        "custom-elements" to "Custom elements",
    ),
)

private val chineseSite: SiteTexts = SiteTexts(
    siteTitle = "ktjs-dom-kit",
    langLabel = "中文",
    apiLabel = "API 文档",
    lightLabel = "亮色",
    darkLabel = "暗色",
    groupTitles = mapOf(
        "overview" to "开始",
        "dom" to "DOM",
        "components" to "自定义元素",
    ),
    pageTitles = mapOf(
        "overview" to "概览",
        "elements" to "元素创建",
        "attributes" to "属性绑定",
        "stylesheets" to "样式表",
        "custom-elements" to "自定义元素",
    ),
)

/** The shell's strings for [lang]. */
fun siteTexts(lang: Lang): SiteTexts = if (lang == Lang.EN) englishSite else chineseSite
