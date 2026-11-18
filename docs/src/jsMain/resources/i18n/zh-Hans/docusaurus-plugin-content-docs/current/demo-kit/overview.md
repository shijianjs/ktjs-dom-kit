---
sidebar_position: 1
---

# Demo kit

`ktjs-demo-kit` 是**可运行演示站**的外壳 —— 那种既当文档、又当集成测试的站点。它不是 UI 框架，也不是
页面模板：它提供外壳（app layout、抽屉、导航栏、菜单）、菜单背后的页面目录、语言与主题状态，以及页面
拼装用的区块。

`ktjs-dom-kit` 的演示就是它的一个实例：你正在读这些代码示例的那些页面，本身就是一个 `Catalog` 里的
`Entry`，挂进同一个 `DemoShell`。

## 坐标

```kotlin
implementation("io.github.shijianjs:ktjs-demo-kit:0.1.0")
```

它以 `api` 依赖带来 `ktjs-dom-kit`、`ktjs-vaadin-web-components`、`ktjs-iconify-icon`（外壳由 Vaadin
组件与 Iconify 图标搭成，页面契约用 `HTMLElement` 表述），所以演示项目只加这一行就能开始写页面。

## 它给人什么

| 包 | 提供 |
| --- | --- |
| `…ktjs.demo.shell` | [`DemoShell`](./pages.md) 与 `ShellConfig` —— 站点外壳及它唯一的配置对象；以及外壳与其样式表共用的 `SHELL_SHEET`、类名 |
| `…ktjs.demo.nav` | `Catalog`、`Group`、`Entry`、`Page`，以及它们背后的 hash `Router` |
| `…ktjs.demo.i18n` | `Lang`（BCP 47 标签，可注册）、`LanguageState`、`Translations<T>` / `texts(…)` 与 `SiteTexts` |
| `…ktjs.demo.page` | [`DemoPage`](./pages.md) 与区块：`paragraph`、`heading`、`anchor`、`codeText`、`bulletList`、`row`、`col`、`exampleBox`、`sectionBlock`、`Readout` |
| `…ktjs.demo.style` | `Theme` / `ThemeMode`（亮色 / 暗色 / 跟随浏览器）、`Palette`（站点上色用的 Lumo 自定义属性）、`RESET`（共享 reset 表） |

## 接一个站点

演示站的 `main` 永远三步：建状态、定义标签、挂配置。

```kotlin
fun main() {
    val language = LanguageState()          // 内置英语 + 简体中文
    language.init()                         // 把记住的语言写回 <html lang>
    val theme = Theme()                     // 亮 / 暗 / 跟随系统，带持久化
    theme.applyStored()

    DemoShell.define()
    val shell = document.getElementById(ElementId("demo")) as? DemoShell ?: error("no <ktjs-demo-shell>")

    shell.mount(
        ShellConfig(
            catalog = siteCatalog(),
            language = language,
            theme = theme,
            texts = siteTexts,
            siteVersion = KIT_VERSION,
            docsUrl = DOCS_URL,
            apiUrl = API_URL,
            sourceUrl = SOURCE_URL,
            logoSrc = "img/ktjs-dom-kit-logo.png",
        ),
    )
}
```

`ShellConfig` 是**一个**对象，这是有意的。`mount(catalog, language, theme, config, texts)` 这样的签名
每加一个旋钮就多一个参数；一个配置对象意味着外壳以后多一个关注点（`siteVersion`、反馈链接、base
path）时，各演示项目都不用改调用点。`siteVersion` 之后的都是可选项 —— 只有仓库地址的站点会得到只有一条
条目的链接菜单，一个 URL 都没有的站点则完全隐藏该菜单。

元素本身必须由页面提供（`index.html` 里声明 `<ktjs-demo-shell id="demo">`）；在 `main` 里再建一个外壳
只会让声明的那个空着，并给站点整出两个抽屉。

## 语言

kit 自带两种语言，并把「语言」当成**数据**，而不是 enum 分支：

```kotlin
val language = LanguageState(languages = listOf(Langs.EN, Langs.ZH_HANS, Lang("ja")))

private val siteTexts = texts(
    Langs.EN to SiteTexts(siteTitle = "ktjs-dom-kit", …),
    Langs.ZH_HANS to SiteTexts(siteTitle = "ktjs-dom-kit", …),
    Lang("ja") to SiteTexts(siteTitle = "ktjs-dom-kit", …),   // 由应用自己注册
)
```

- `Lang` 是标签之上的 external interface；`Langs` 只放两个内置值，`Lang("ja")` 注册站点有译文的任何
  其他标签。
- `LanguageState` 持有语言列表、当前值、`localStorage` 持久化与 `document.documentElement.lang`；语言菜单
  就是从这份列表建的，所以站点没注册的标签根本不会出现。
- `Translations<T>` 是「一个语言一个文本对象」。缺失的语言回退到**最先声明**的那个 —— 新注册的语言在
  自己的译文到位之前也能显示合理内容。声明顺序就是菜单顺序。
- 切换语言是在已挂载页面上走一遍（`Page.applyTranslation`），绝不重建：滚动位置和用户输入的东西都留着。

## 目录与路由

```kotlin
class Entry(val id: String, val group: Group, val icon: String?, val create: () -> Page)
```

分组由条目推导，所以加一个页面就是加一条 `Entry`，别的都不用动。`id` 就是路由（`#/stylesheets`），也是
`SiteTexts.pageTitles` 的键 —— 表里缺这个 id 时直接显示 id 本身，于是页面能在译文之前先出现在导航里。
页面首次访问时惰性创建，之后缓存，所以页面自己的状态（输入的内容、展开的面板）离开再回来还在。
