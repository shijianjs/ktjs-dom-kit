---
sidebar_position: 3
---

# 属性

`attr` 把一个 Kotlin 属性搭在 HTML attribute 上：

```kotlin
class KtjsBadge : CustomElementAdapter() {
    var href: String by attr { change -> anchor.href = change.newValue }
    var height: Int by attr("height", default = 20) { change -> image.height = change.newValue }
}
```

attribute 就是这个属性。getter 读它并解码，setter 编码并写它，Kotlin 侧除「上次上报的值」外不存任何东西
——而那份只为区分真变化与重复写入。标记、`setAttribute`、devtools 与 Kotlin 最终落到同一处，没有第二份
状态要同步。

`attr` 是 inline 函数，`name`、`default`、`codec` 与变化回调都可省：

```kotlin
var logoSrc: String by attr                                              // logo-src
var ratio: Double by attr("data-ratio", default = 1.0) { … }             // 显式给名
var kind: ActionKind by attr(codec = enumAttributeCodec(ActionKind.entries), default = ActionKind.Primary)
```

## 命名

属性名默认是属性名的中划线形式：`maxCount` → `max-count`、`XMLHttp` → `xml-http`。规则是在「一个词的
开头的大写字母」前加连字符；数字与已有的连字符保持原样。

原生拼写不做特殊照顾。`tabIndex` 要写 `attr("tabindex")`；属性名本来属于 `HTMLElement` 的成员
（`title`、`id`、`hidden`）时必须换个名字并显式映射：

```kotlin
@JsName("titleText")
var titleText: String by attr("title") { change -> heading.textContent = change.newValue }
```

**要让 JavaScript 看到属性就加 `@JsName`。** Kotlin/JS 会把委托属性编译成模块内混淆名的访问器方法，而
**不**生成 JS 属性，于是页面上写 `element.titleText = "x"` 只会静默写一个 expando 字段。`@JsName("titleText")`
才会生成真正的 `get`/`set` 访问器；Kotlin 调用方不需要它。这也是这套 API 里「属性名」与「attribute 名」
是两件事的原因——`titleText` 是 TypeScript 看到的，`title` 是浏览器存的，`@JsName` + `attr("title")` 是
两者的桥。

## 取值与编解码器

取值由 `AttributeCodec<T>` 转换。内置的几种：

| 编解码器 | 属性类型 | 行为 |
| --- | --- | --- |
| `StringAttributeCodec` | `String` | 原样读写；属性缺失时读回 `""`，所以 `String` 属性永远不为 null |
| `BooleanAttributeCodec` | `Boolean` | 存在性语义，和 `hidden` 一样——`<x flagged>` 与 `<x flagged="false">` 都是 `true`；`true` 写成裸属性，`false` 删掉它 |
| `IntAttributeCodec` | `Int` | `toIntOrNull`；否则回退到 `default`（或 `0`） |
| `DoubleAttributeCodec` | `Double` | `toDoubleOrNull`；回退到 `default`（或 `0.0`） |

其它类型就是一个 `AttributeCodec<T>` 实现——三个函数，不引序列化库：

```kotlin
object InstantCodec : AttributeCodec<Instant> {
    override fun encode(value: Instant): String = value.toString()
    override fun decode(value: String): Instant? = runCatching { Instant.parse(value) }.getOrNull()
    override val absent: Instant = Instant.EPOCH
}
```

`decode` 返回 `null` 与「属性缺失」同等对待：委托回退到 `default`，没有 `default` 就用 `absent`。解码
永远不抛异常，标记里写错也弄不坏元素。

枚举必须给编解码器，因为 Kotlin/JS 无法反射枚举常量。`enumAttributeCodec` 显式收下取值，按标签
（默认是小写常量名）忽略大小写匹配，解码时也接受常量名本身：

```kotlin
enum class ActionKind { Primary, Secondary }

var kind: ActionKind by attr(codec = enumAttributeCodec(ActionKind.entries), default = ActionKind.Primary)
```

## `AttributeChange`

回调收到的是「一次变化」而不是只有新值，所以名字、删除、原始字符串都在手边，不必等以后改签名：

| 成员 | 含义 |
| --- | --- |
| `name` | attribute 名，如 `max-count` |
| `oldValue` / `newValue` | 解码后的值；首次上报时两者相等 |
| `oldRaw` / `newRaw` | 原始字符串；属性缺失时为 `null` |
| `isRemoval` | 属性是被删掉（而不是写入）时为 `true` |

`newRaw == null` 才是「删除」与「解码出来恰好等于默认值」的区别所在。

## `default` 不会写进标记

`default` 是属性缺失时 getter 报告的值——一个 Kotlin 侧的值，不是声明。删除 attribute 就回到它。要写进
标记得显式要求，而且只有一处：

```kotlin
override fun connectedCallback() {
    applyInitialAttributes()                          // 默认值留在 Kotlin 侧
    applyInitialAttributes(writeDefaults = true)      // ……先把它拷进标记
}
```

## 回调什么时候跑

- **来自 setter：** 同步。
- **来自外部**（`setAttribute`、devtools、别的元素）：微任务里。元素用 `MutationObserver` 观察自己，而不是
  声明 `observedAttributes`——那份名单由 `customElements.define` 从静态 getter 读取，那时任何委托都还没
  创建；而且 `MutationObserver` 还能看到没人声明过的属性，未挂载的元素也一样。
- **连接时：** 来自 `applyInitialAttributes()`。标签上原本就有的标记不会经过 setter，这是回调第一次得知
  它们的途径。它是幂等的——已经上报过值的委托会被跳过——所以重新连接时再调一次不会重绘，而且它会顺手丢掉
  观察器排队的记录，避免上报两遍。

只有真实变化才上报。写回同样的解码值不算变化，删除 attribute 算。回调里写自己的属性不会自我重入，因此
元素可以规范化它拿到的值。

## JavaScript 侧赋进来的字符串是什么意思

属性同时也是元素的 JS 接口，而 JS 调用方手里只有它拿到的那个值。React 19 会把每个同名 JSX prop 直接赋给
property，所以 `<ktjs-action kind="secondary">` 到这里是**字符串** `"secondary"`，尽管属性类型是
`ActionKind`。

因此委托把字符串当成 attribute 的原始形态——正是 `setAttribute` 会存下的那段文本——只有属性自身类型的值
才过编解码器。对 `String` 来说两者是一回事，因为那个编解码器就是恒等函数。漏掉这条的症状很隐蔽：整页加载
正常（React 在 hydration 时只对账、不重设 prop），而客户端导航到一个由 React 新建该元素的页面时，会在
编解码器里抛 `ClassCastException`，错误边界把整页换掉。

## 只有「反射型」属性该放这里

`attr` 会写 attribute，所以它适合「值就是那个 attribute」的属性——也就是标记能声明、CSS 能选中的那些。
`input.value` 不属于这类：它是比 attribute 活得更久的内部状态，照旧写成普通属性。
