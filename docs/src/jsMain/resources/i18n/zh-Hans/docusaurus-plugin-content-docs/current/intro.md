---
sidebar_position: 1
title: 简介
---

# 简介

`ktjs-dom-kit` 是一个手写 DOM 组件用的 Kotlin/JS 库：没有 UI 框架、没有模板语言、没有「状态驱动重渲染」。
一个组件就是一个持有自己节点的类，属性就是状态，样式要么是类型化的内联声明，要么是 shadow root adopt 的
一份 `CSSStyleSheet`。

它是发布出去的：

```kotlin
implementation("io.github.shijianjs:ktjs-dom-kit:0.1.0")
```

## 里面有什么

| 部分 | 作用 |
| --- | --- |
| `ktjs-dom-kit` | 库本体：`CustomElementAdapter`、`attr` 委托、shadow root 辅助、`cssSheet { … }`、`loadjs` 封装 |
| `docs` | 本站：Kotlin 配置 + Markdown 页面，由 [ktjs-docusaurus](https://github.com/shijianjs/ktjs-docusaurus) 的 Gradle 插件构建 |
| 其余 | Detekt、Playwright 浏览器测试、经 hub 发布到 Maven Central、Pages 工作流 |

`/api/` 下是发布模块的 Dokka 产物——和 `pagesSite` 组装本站时用的是同一次 Dokka。

## 一个仓库，几个小库

这个仓库里每个库都是一个以 Maven 构件命名的 Gradle 模块，构建会自动收集全部：发布、Dokka 与版本报表都按
`ktjs-` 前缀找模块，所以加一个库就是加一个目录加一行 `include`。

理由就是成本：一个 API 只有十几个声明的封装，不值得为它单独开仓库、CI、发布流程和 Pages 站点。它落在这里，
在本站拿到自己的一节文档，消费方照常从 Maven Central 解析。Docusaurus 那种量级的封装才留在各自的仓库里。

## 接下来

- [快速开始](guide/getting-started.md)：加依赖、写第一个元素。
- [DOM 工具箱](dom-kit/overview.md)：库里有什么，按包讲。
