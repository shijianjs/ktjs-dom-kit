---
sidebar_position: 1
---

# DOM 工具箱

`ktjs-dom-kit` 是一个发布出去的工具库：用 Kotlin/JS 对着原生 DOM 写组件时用的辅助代码，不认识任何文档
框架，背后也没有任何组件运行时。

它是「不用 UI 框架写 UI」时踩的那层地皮——自定义元素可以继承的基类、把 DOM 当作唯一状态来源的属性委托、
元素内联样式的类型化视图、用 Kotlin 值（而不是 CSS 文本）搭出 `CSSStyleSheet` 的方式，以及 `loadjs`
加载器的类型化封装。

## 谁在用它

[ktjs-docusaurus](https://github.com/shijianjs/ktjs-docusaurus) 的站点就建在它上面：那个首页上的每个
`<ktjs-…>` 元素都继承 `CustomElementAdapter`、用 `cssSheet { … }` 写自己的样式。那是消费方，不是上级——
构件由这个仓库发布，站点像任何普通项目一样依赖它。

## 为什么单独一个仓库

这里没有任何 npm 包要跟随，所以版本是手写的语义版本（`0.1.0`），不走本仓库里 npm 封装模块那套
`{npm 版本}-kt.{修订号}`。

这个仓库刻意不做「一库一项目」：一个 API 只有十几个声明的封装，不值得为它单独开仓库、CI、发布流程和
Pages 站点，所以小库作为额外的 Gradle 模块落在这里，并在本站拿到自己的一节文档。构建会自动收集每个
`ktjs-*` 模块：发布、Dokka 与版本报表都不用改。

## 坐标

```kotlin
implementation("io.github.shijianjs:ktjs-dom-kit:0.1.0")
```

构件发布到 Maven Central，API 参考在本站 `/api/` 下。

## 里面有什么

模块按关注点分包：一件事一个包；只要一个包里存在明确的父子级或一对多关系，就再下沉一层子包（一个类型与
它的各种实现、一张样式表与写它时用的语法糖）；一个声明一个文件，没有哪个文件是杂物间。

| 包 | 提供 |
| --- | --- |
| `element/` | 通用元素辅助：`TagName<T>.createElement { … }`，以及 `HTMLElement.inlineStyle`（元素自己的样式，`csstype.Properties` 视图） |
| `webcomponent/` | 自定义元素的构成：`CustomElementAdapter`（按原生名字声明生命周期钩子、承载 `attr` 委托）、`defineIfMissing`、[`stripBridgesFrom`](./custom-elements.md)，以及 `attachShadowRoot` / `attachStyledShadow` |
| `attribute/` | `attr` 属性委托：`attr`、`AttributeChange`、它背后的 provider 与 delegate，以及属性名规则；`attribute/types/` 放编解码器——委托认识的每种类型一个，外加 `enumAttributeCodec` |
| `css/` | `cssSheet { … }` / `cssText { … }`，以及 cssom 没建模的类型化值（`RadialGradientShape`、`radialGradient`、`pointerEventsNone`、`SupportsQuery`……）；`css/dsl/` 放写样式表时用的语法糖：`Pseudos` / `pseudo` / `pseudos`、at-rule `supports { … }` 与 `webkitBackgroundClip` |
| `loadjs/` | [`loadjs`](https://github.com/kubetail-org/loadjs) 加载器的封装：四种调用形态各按分支写成一个重载、路径前缀（`css!`、`img!`、`module!`、`nomodule!`）与 bundle 注册表（`ready` / `done` / `reset` / `isDefined`） |

## 它不做什么

- **没有组件模型。** 没有状态、没有 `render()`、没有 diff，也没有 UI 运行时。组件构造时建一次子树，
  之后只改自己持有的节点——实际形态见 ktjs-docusaurus 站点的
  [首页分区](https://shijianjs.github.io/ktjs-docusaurus/docs/kit/home-sections/)。
- **不拼 HTML 字符串。** 节点就是节点（`document.createElement` 配类型化的 `TagName<T>`），不从标记
  文本拼出来再解析。
- **没有 `<style>` 节点。** 组件自己的样式要么是节点上的内联声明（`inlineStyle`），要么是 shadow root
  adopt 的一份 `CSSStyleSheet`；sheet 属于类，不属于实例。

## 各页面

- [自定义元素](./custom-elements.md)——基类、生命周期钩子与标签定义。
- [属性](./attributes.md)——`attr` 委托与它的编解码器。
- [样式](./styling.md)——内联样式、shadow root 与 adopt 的 sheet。
- [类型化样式表](./css-sheet.md)——`cssSheet { … }`：序列化管线、类型化的值、坑，以及 cssom 没有的
  类型怎么办。
