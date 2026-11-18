---
sidebar_position: 2
---

# 构建元素

kotlin-browser 给每个 HTML 标签都提供了类型化常量（`HtmlTagName.div`、`HtmlTagName.input`、
`HtmlTagName.tableCell`……），dom-kit 把这个常量变成「建一个节点」的写法。三种写法，同一个元素：

```kotlin
// 1. 显式工厂
val card = HtmlTagName.div.createElement { classList.add(CARD_CLASS) }

// 2. 同一个调用，写在标签上
val card = HtmlTagName.div { classList.add(CARD_CLASS) }

// 3. 写在「将要包含它」的那个元素的块里
val card = HtmlTagName.section {
    +HtmlTagName.div { classList.add(CARD_CLASS) }
}
```

三种都走 `Document.createElement(TagName<T>)`，所以块的接收者就是元素的类型：`HtmlTagName.anchor`
给你的 `HTMLAnchorElement` 已经带好 `href`、`target`、`rel`，建完不需要强转，也不需要
`querySelector` 再找一次。

## 用哪一种

| 写法 | 什么时候用 |
| --- | --- |
| `+tag { … }` | 父节点就在外层接收者里，这是大多数情况：它同时完成「建」和「挂」，表达式的值又是子节点，所以 `val x = +tag { … }` 顺手拿到引用。 |
| `tag { … }` | 父节点在别处决定（从函数返回、存进字段、交给 `replaceChildren`）。 |
| `tag.createElement { … }` | 需要第二个参数时：在另一个 document 里建（`document.implementation.createHTMLDocument()`），或者一条链里 `createElement` 比一次调用更好读。 |

## `+` 的父节点从哪来

`+` 是新建元素上的 `unaryPlus`，父节点是一个**上下文参数**：

```kotlin
context(parent: ParentNode)
operator fun <T : HTMLElement> T.unaryPlus(): T
```

父节点取自外层接收者，所以这个运算符只在 `ParentNode` 在作用域内的地方存在——也就是元素的块里，
别处没有。存进字段的节点、已经存在的元素都没有可推断的父节点，那里 `parent.appendChild(node)` 才是
诚实的写法；编译器不会悄悄挑一个容器。

两个值得知道的后果：

- **`+` 只追加，从不清空。** 兄弟节点保持插入顺序，已经有内容的容器仍然保留原有内容，和连续调用
  `appendChild` 一样。
- **表达式的值是子节点，不是父节点。** 两种用法因此都成立：只为副作用就写 `+span { … }`，下次要改
  文案就写 `val note = +span { … }`。

## 它不做什么

- **没有标记。** 没有 `innerHTML`、没有模板字符串、没有重新解析：节点就是节点，建一次。
- **不用再 `querySelector`。** 建出来的节点本来就带类型；构建者把它存进字段，而不是之后再搜。
- **不重建。** 往列表里 `+` 一条只是追加一条，不碰兄弟节点，也不重绘页面。

## 具名 helper

当某个形状反复出现——正文段落、示例里的代码行——具名 helper 才是合适的层次，它内部仍然用同一个
调用建节点。类型化标签是原语，helper 是它上面的词汇，不是第二条建节点的路。
