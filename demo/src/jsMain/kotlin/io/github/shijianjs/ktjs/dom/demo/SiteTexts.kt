package io.github.shijianjs.ktjs.dom.demo

import io.github.shijianjs.ktjs.demo.i18n.Langs
import io.github.shijianjs.ktjs.demo.i18n.SiteTexts
import io.github.shijianjs.ktjs.demo.i18n.Translations
import io.github.shijianjs.ktjs.demo.i18n.texts

/**
 * Every string the shell and the navigation show, in English.
 *
 * One object per language, declared once in [siteTexts]; adding a language is one more entry there
 * plus one in `LanguageState.languages`, and no existing string is touched.
 */
private val englishSite: SiteTexts = SiteTexts(
    siteTitle = "ktjs-dom-kit",
    langLabel = "English",
    languageLabel = "Language",
    themeLabel = "Theme",
    docsLabel = "Docs",
    siteDocsLabel = "Documentation",
    apiLabel = "API reference",
    sourceLabel = "GitHub",
    lightLabel = "Light",
    darkLabel = "Dark",
    systemLabel = "Follow browser",
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
    languageLabel = "语言",
    themeLabel = "外观",
    docsLabel = "文档",
    siteDocsLabel = "使用文档",
    apiLabel = "API 文档",
    sourceLabel = "GitHub 仓库",
    lightLabel = "亮色",
    darkLabel = "暗色",
    systemLabel = "跟随浏览器",
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

/** The shell's strings, one entry per registered language. */
val siteTexts: Translations<SiteTexts> = texts(
    Langs.EN to englishSite,
    Langs.ZH_HANS to chineseSite,
)
