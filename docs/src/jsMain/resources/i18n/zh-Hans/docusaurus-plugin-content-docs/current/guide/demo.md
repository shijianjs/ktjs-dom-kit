---
sidebar_label: 演示
---

# 演示

可交互的演示发布在 [`/demo/`](pathname:///demo/)。它是一个完整的单页应用：一个布局，左侧是按组
划分的目录，顶栏有站点标记、标题、面包屑和三个下拉菜单，内容区一次只显示一个页面。路由放在
fragment（`#/elements`）里，所以静态托管上的深链可用，刷新也不会丢。

| 路由 | 展示内容 |
| --- | --- |
| `#/overview` | 这个库是什么、坐标、API 参考在哪 |
| `#/elements` | `TagName<T>.createElement { }` —— 标签决定元素类型 |
| `#/attributes` | `attr { }` —— 属性即属性，Kotlin 侧与 `setAttribute` 侧写同一个值 |
| `#/stylesheets` | `cssSheet { }` 由组件的 shadow root adopt，含内联样式表达不了的 `:hover` |
| `#/custom-elements` | `CustomElementAdapter` + `defineIfMissing`，同一标签的两个独立实例 |

页面守住两条不变量，端到端测试逐条断言：

- **自有样式表绝不挂 `document`** —— `document.adoptedStyleSheets.length` 恒为 `0`，每个 shadow root
  只 adopt 它需要的 sheet，页面正文里没有内联 `style`（唯一的内联声明是菜单项上的 Iconify 尺寸，
  它们跟着元素进了菜单的 overlay —— 那是我们的 sheet 进不去的 shadow 树）；
- **换语言不重建页面** —— 逐个节点改写文案，所以已经点过的计数器在切换语言后仍是原值。

### 布局

布局就是一个 `vaadin-app-layout`：

- 站点标记、标题（`<h1>`）、版本号、抽屉开关和面包屑都在导航栏里，写法照官方 `app-layout` 的
  drawer 示例；左侧抽屉里是分组的 `vaadin-side-nav`，一个组一个无 `path` 的父项、一个页面一个子项；
- 标记和标题一起是**回首页的链接**：外面套一个 `<a href="#/">`，走丢了的读者点一下字标就回到首页，
  同时保留链接的行为（中键新开、复制地址、键盘激活）。`#/` 会被路由解析成默认路由，所以这个链接
  不需要布局知道首页的 slug；
- 每个导航项都带 `iconify-icon`（`lucide:*`），所以演示本身也在验证 `ktjs-iconify` 与布局的配合；
- 抽屉开关是图标按钮：Lumo 给它的是 44px 的盒子加 `margin: 0 var(--lumo-space-s)`，等于在汉堡和字标
  之间留了一个空槽。布局保留组件本身的尺寸（真实可点的尺寸），但去掉它的外边距，改由导航栏自己给左
  内缩，于是图标、开关和字标读起来是一组；
- 操作区是三个 `vaadin-menu-bar` 下拉菜单 —— **文档**（使用文档、API 参考、仓库）、**语言**、
  **外观** —— 而不是一排按钮：这几样语义上就是菜单，当前值放在菜单里，按钮用固定名词，
  所以打开或选择都不会让导航栏改宽度；
- 外观菜单三项：**亮色**、**暗色**、**跟随浏览器**。最后一项给文档元素写 `theme="light-dark"`，
  由 Lumo 自己解析 `prefers-color-scheme`，系统偏好一变整站跟着重绘 —— 布局里没有 `matchMedia`
  监听；
- 菜单子项落在 `menu-bar` 自己的 shadow 树里，布局的 sheet 够不到，所以那部分内容的尺寸与选中强调
  只能内联；
- Lumo 的 `utility.css` 是**文档级**样式表，规则穿不进 shadow 树。布局仍然用包装里的类名，但把用到的
  两条声明（`items-center`、`justify-between`）写进自己的 sheet（`layout/LayoutSheet.kt`）。

### 语言与站点标记

- 语言是 BCP 47 标签本身，不是 `enum`：`Lang("ja")` 就注册一门语言，`LanguageState(languages = …)`
  决定站点提供哪几门，每个文本类用 `texts(Langs.EN to …, Langs.ZH_HANS to …)` 声明自己的译文。
  没有条目的语言回落到第一个声明的语言，所以新注册的语言在译文补齐前就能出现在菜单里；
  声明顺序就是菜单顺序。
- 站点标记是 `<img>`：`LayoutConfig.logoSrc` 指向 README 与文档站用的同一份 logo。演示的构建
  （`scaleLogo`）把它缩到 128px —— 仓库里只有一份 logo，产物也不会为 28px 的位置塞一张 1254px 的原图。

### 本地运行

`./gradlew :demo:jsBrowserDevelopmentRun` 起开发服务器；`:demo:jsBrowserProductionRun` 跑生产包。
`js { }` 里必须设 `target = "es2015"`：否则 Kotlin 发射 ES5 风格的类，`HTMLElement.call(this)` 不能
用 `new` 调用，自定义元素永远不会升级。如果运行只报 `Error occurred. See log for details`、没有别的
信息，先关掉 Gradle daemon（`./gradlew --stop`）再试 —— 这个故障出现过一次，是 daemon 的问题，
不是构建的问题。

`./gradlew pagesSite` 会生成整个站点：文档在 `/`，演示在 `/demo/`，API 参考在 `/api/`。
