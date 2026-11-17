---
sidebar_label: 演示
---

# 演示

可交互的演示发布在 [`/demo/`](pathname:///demo/)。它是一个完整的单页应用：一个外壳，左侧是按组
划分的目录，顶栏有面包屑、API 链接、中英切换和亮暗切换，内容区一次只显示一个页面。路由放在
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
  只 adopt 它需要的 sheet，页面里唯一的 `style` 属性在外壳宿主元素上；
- **换语言不重建页面** —— 逐个节点改写文案，所以已经点过的计数器在切换语言后仍是原值。

### 外壳

外壳就是一个 `vaadin-app-layout`：

- 站点标题（`<h1>`）、版本号、抽屉开关和面包屑都在导航栏里，写法照官方 `app-layout` 的 drawer
  示例；左侧抽屉里是分组的 `vaadin-side-nav`，一个组一个无 `path` 的父项、一个页面一个子项；
- 每个导航项都带 `iconify-icon`（`lucide:*`），所以演示本身也在验证 `ktjs-iconify` 与外壳的配合；
- 中英、亮暗四个按钮固定等宽，切换时只改哪个按钮是 `theme="primary"`，导航栏不会整体抖动；
- Lumo 的 `utility.css` 是**文档级**样式表，规则穿不进 shadow 树。外壳仍然用包装里的类名，但把用到的
  两条声明（`items-center`、`justify-between`）写进自己的 sheet —— 见 `Sheets.kt` 里的注释。

`./gradlew pagesSite` 会生成整个站点：文档在 `/`，演示在 `/demo/`，API 参考在 `/api/`。
