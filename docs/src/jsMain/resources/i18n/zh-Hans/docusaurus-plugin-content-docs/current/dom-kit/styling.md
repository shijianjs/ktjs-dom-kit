---
sidebar_position: 4
---

# 样式

组件的样式写在组件自己身上，只有三个去处——而三者之间怎么选，不是口味问题：

| 位置 | 放什么 | 怎么写 |
| --- | --- | --- |
| 节点自己 | 一条普通声明能表达的全部 | `node.inlineStyle.apply { display = Display.flex; padding = Padding(1.5.rem) }` |
| 组件类自己的一份 sheet | 内联表达不了的：`:hover`、`:focus-visible`、`::before`、`@media`、`@supports`、`@keyframes` | 类的 `companion object` 里一份 `CSSStyleSheet`，由 shadow root adopt |
| 多个组件共享的一份 sheet | 重置、主题 token、父容器统一施加给子组件的布局 | 同样的机制，一份 sheet 被多个元素类 adopt |

## 类型化的内联样式

`HTMLElement.style` 来自 lib.dom，每个属性都是普通 `String`。`inlineStyle` 是同一个
`CSSStyleDeclaration` 的另一种视图——kotlin-csstype 的 `Properties`——于是元素自己的盒子、配色与排印
都能用真实类型写，不必手拼字符串：

```kotlin
card.inlineStyle.apply {
    display = Display.flex
    padding = Padding(1.5.rem)
    borderRadius = 1.rem
    color = Color("var(--ktjs-accent, #1f6feb)")   // 最后手段：类型表达不了的字符串
}
```

取值来自 `web.cssom` / csstype：`Display.flex`、`1.5.rem`、`Padding(top, horizontal, bottom)`、
`BoxShadow(…)`、`rgb(…)`、`NamedColor.white`（命名色**就是** `Color`，不必写 `"#fff"`）、
`var(hook, fallback)`、`clamp(…)`。这只是同一个对象上的更丰富接口，样式仍然是元素上的普通内联样式。

## Shadow root

```kotlin
fun HTMLElement.attachShadowRoot(mode: ShadowRootMode = ShadowRootMode.open): ShadowRoot

fun HTMLElement.attachStyledShadow(
    vararg styles: CSSStyleSheet,
    mode: ShadowRootMode = ShadowRootMode.open,
): ShadowRoot
```

`attachShadowRoot` 挂上 root；`attachStyledShadow` 做同一件事，并顺手 adopt 给定的 sheet——只要有 CSS 规则
的组件，要的都是后者：

```kotlin
class Card : CustomElementAdapter() {
    private val shadow = attachStyledShadow(HOME_RESET, SHEET)

    companion object {
        private val cardClass = ClassName("card")

        private val SHEET: CSSStyleSheet = cssSheet {
            cardClass { Pseudos.hover { borderColor = `var`(accentHook, `var`(primaryColorHook)) } }
        }
    }
}
```

**每个类一份 sheet，放在该类的 `companion object` 里。** 规则是静态的，所以解析一次、一份 `CSSStyleSheet`
对象服务所有实例——就是 Lit 的 `static styles`，只是放在 Kotlin 存类静态成员的地方。绝不要每个实例建一个
`<style>` 节点：一个四百个单元格的页面会背上四百份同样的文本，并付四百次解析成本。adopt 是一种**共享**
机制，上表那两个层级都用它。

## 为什么默认 shadow

shadow root 让页面样式表进不来、组件的规则出不去。主题变化从另一侧穿过来，靠的是可继承的自定义属性
——那是唯一会继承的一类值：

```css
:root {
  --ktjs-accent: #1f6feb;
  --ktjs-card-bg: var(--ifm-background-surface-color);
}
```

边界带来两条规矩：

- **属性声明在外面**，即 `:root` / `[data-theme]` 上。组件内部不要写自己的 `[data-theme]` 选择器，也不要
  依赖宿主 class：这些东西都穿不过边界。
- **只有可继承属性穿得过去。** `box-sizing` 不是继承属性，所以 shadow 树里必须重新声明自己的重置；
  `:host { display: block }` 要写在 sheet 里，理由和「`display` 不能写在构造函数里」一样（在那里写属性会抛
  异常）。

只有在组件确实必须复用宿主 CSS 规则时才退回 light DOM，并且要在评审里说明——前缀与自定义属性才是让
light DOM 样式不碍事的凭据。

## 怎么搭这份 sheet

规则本身写成类型化对象，而不是 CSS 文本——这是[类型化样式表](./css-sheet.md)那一页的内容。
