---
sidebar_position: 5
---

# 类型化样式表

`cssSheet { … }` 从 csstype 的规则对象搭出一份 `CSSStyleSheet`：规则、取值、单位与伪类结构都来自类型，
剩下的字符串只有那些确实没有类型的——自定义属性名、`:host`、`*`。

可运行的对照实验（同一段规则的三种写法 + 一张 adopt 了 sheet 的卡片）在
[ktjs-docusaurus](https://github.com/shijianjs/ktjs-docusaurus) 仓库的 `probes/web-components` 里，入口是
`example.CssSheetExperiment`，在那个 checkout 下跑：

```bash
./gradlew :probes-web-components:jsBrowserDevelopmentRun
```

## 它解决什么

手写 CSS 文本的问题不是长，而是**魔法字符串的范围**：属性名、数值单位、伪类结构都靠人拼，写错只在下一次
肉眼看样式时才发现。

```kotlin
internal val CARD_SHEET: CSSStyleSheet = cssSheet {
    cardClass {                                   // 选择器用 ClassName，见下文
        border = Border(1.px, LineStyle.solid, `var`(cardBorderHook, NamedColor.black))
        transition = many(Transition(PropertyName.borderColor, 0.2.s, TransitionTimingFunction.ease))
        Pseudos.hover { borderColor = `var`(accentHook, `var`(primaryColorHook)) }
    }
}
```

同一段规则手写约 455 个字符的 CSS。上面这种写法里手写的 CSS 文本是 0、cast 是 0，剩下的字符串只有主题的
自定义属性名；伪类变体由 `Pseudos` 给出，一个手写键都没有。

判断值不值得用这套，看的是「还剩多少 CSS 文本要手写」，不是源码行数——一条规则通体都是
`"@media (max-width: 576px)"` 字符串的话，和一份文本文件没有区别。

## 管线与契约

```
PropertiesBuilder (= JSO)
    │  serializeStyles(jsArrayOf(rules))            @emotion/serialize：声明 + 嵌套 key{…}
    ▼
"border:1px solid …;transition:…;:hover{…}"        （还不是扁平规则）
    │  serialize(compile("selector{…}"), middleware(jsArrayOf(stringify)))   stylis
    ▼
".card{…}.card:hover{…}"                            → CSSStyleSheet.replaceSync(…)
```

- **顶层是选择器映射**，不是声明集合。`cardClass { … }`、`Pseudos.hover { … }`、`":host" { … }`、
  `"@media …" { … }` 是 key；写在里面的属性属于最近一层 `{}` 的那条规则。
- `serializeStyles` 的参数是**数组**（emotion 里它是 tagged template 的 `strings`）：
  `serializeStyles([rules])`。传对象本身会去读 `args[0]`，结果是空串。
- 只挂 `stringify`，不挂 `prefixer`：后者会加 `-webkit-` / `-ms-` 前缀，emotion 11 已经不做这件事。
- 版本钉的是 `@emotion/cache` 自己的 pin：`@emotion/serialize` 1.3.3 + `stylis` 4.2.0。
- 序列化在**浏览器侧**发生（`CSSStyleSheet` 在 Node 里不存在），所以带 sheet 的模块只能进浏览器包。

## 写法清单

### 选择器：`ClassName` + `invoke`

`RuleBuilder` 有 `operator fun ClassName.invoke(block)`，点号由它补：

```kotlin
private val cardClass = ClassName("card")   // 也可复用作 element.className

cssSheet { cardClass { … } }
```

坑：`ClassName("card") { … }` **编译不过**——会被解析成「调用工厂 + 尾随 lambda 当第二个实参」。一次性用
可以加括号 `(ClassName("card")) { … }`；要复用就存成 `val` 再 `cardClass { … }`。

伪类、伪元素用本模块自建的 `css/dsl/Pseudos.kt`：它列全 `csstype.SimplePseudosRuleBuilder` 的 64 个选择器，
每个都带 `&`：

```kotlin
Pseudos.hover { … }                                  // 和官方 hover { } 一样的写法
pseudos(Pseudos.hover, Pseudos.focusVisible) { … }   // "&:hover,&:focus-visible" 一个块
Pseudos.before { … }
many(Pseudos.hover, Pseudos.focusVisible)             // 也可直接拼成一个 Pseudo
```

短写法来自 `context(builder: RuleBuilder<T>) operator fun Pseudo.invoke(…)`：context 参数自动取
`cssSheet { … }` 作用域里的 `PropertiesBuilder`（Kotlin 2.2+ context parameters，本仓库 2.4 无需开关），
调用点需要 `import io.github.shijianjs.ktjs.dom.css.dsl.invoke`。和官方糖的差别是两点：我们带 `&`、能组合。
`many(…)` 的结果也是 `Pseudo`，可以直接 `(many(Pseudos.hover, Pseudos.focusVisible)) { … }`——括号不能省
（`many` 是 `vararg`，不加括号尾随 lambda 会被当成 vararg 实参），所以组合态更推荐 `pseudos(…)`。
显式的 `pseudo(value) { … }` 仍在，适合拿到一个 `Pseudo` 变量时用。

**at-rule 有官方入口，不要手写 `"@media …"` / `"@supports …"` 字符串**：

```kotlin
`@media`(MediaType.screen and maxWidth(576.px)) { … }   // csstype.RuleBuilder.@media

// SupportsQuery 仿 web.cssom.MediaQuery：声明两侧都类型化，and/or/not 组合
supports(
    supportsDeclaration(webkitBackgroundClipName, BackgroundClip.text) or
        supportsDeclaration(PropertyName.backgroundClip, BackgroundClip.text),
) { … }
```

官方已经给的（IDE 里查符号时优先找这些，别再自己拼）：

| 入口 | 在哪 | 用途 |
| --- | --- | --- |
| `RuleBuilder.@media / @container / @font-face / @view-transition` | `kotlin-csstype/.../RuleBuilder.kt` | at-rule 块，`@media(query)` 收 `MediaQuery` |
| `AtRules.media / supports / layer / page / scope / …` | `web.cssom.AtRules` | 所有 at-rule 关键字常量（`@JsValue("@media")` 等） |
| `MediaQuery.and / or / not` | `web.cssom.MediaQuery.kt` | 组合条件 |
| `MediaType.screen / print / all` | `web.cssom.atrule.MediaType` | 媒体类型 |
| `maxWidth / minWidth / width / height / orientation / prefersColorScheme / prefersReducedMotion / …` | `web.cssom.atrule/*.fun.kt` | 媒体特性（一整个目录，约 30 个） |
| `var / env / rgb / many / array / important / linearGradient / stop / Content() / NamedColor / PropertyName / translatex` | `web.cssom` 的 `*.functions.kt`、`*.ext.kt`、`*.fun.kt` | 取值与函数 |

**目前确实还是字符串的**：`:host` 与 `*` 这类顶层选择器、应用级自定义属性名。伪类键和 `@supports`
条件都已经不再手写——前者由 `Pseudos` + `pseudo` / `pseudos` 覆盖，后者由 `css/Supports.kt` 照
`MediaQuery` 补的 `SupportsQuery` 覆盖。

### 自定义属性：具名常量 + `var(name, fallback)`

```kotlin
private val accentHook = CustomPropertyName<Color>("--ktjs-accent")
private val shadowHook = CustomPropertyName<BoxShadow>("--ktjs-shadow")

color = var(accentHook, NamedColor.white)                       // var(--ktjs-accent,#FFFFFF)
boxShadow = var(shadowHook, BoxShadow(0.px, 10.px, 24.px, …))
```

类型参数是 hook 承载的值类型，`var` 的兜底值因此也必须同类型——兜底值是**类型化的值**，不是文本。
cssom 没有应用级属性名的常量，字符串在这里出现一次；具名常量避免它散落多处。

### 命名色、多值、过渡

| 需要 | 写法 |
| --- | --- |
| 命名色 | `NamedColor.white`（`@JsValue("#FFFFFF")`，类型就是 `Color`） |
| 任意色 | `rgb(127, 127, 127, 0.24)`、`oklch(…)`、`Color("var(--x)")`（最后手段） |
| 逗号多值 | `many(a, b)`；空格多值 `array(…)`；`!important` 用 `important(x)` |
| 过渡 | `Transition(PropertyName.borderColor, 0.2.s, TransitionTimingFunction.ease)`，多个用 `many(…)` |
| 回退阶梯 | `fallback(Display.grid, Display.flex)`（序列化成两条同名声明） |
| 阴影/边框/内边距 | `BoxShadow(0.px, 10.px, 24.px, rgb(…))`、`Border(1.px, LineStyle.solid, …)`、`Padding(…)` |

`PropertyName.borderColor` 能当过渡属性名，是因为 `PropertyName<T> : Ident`，而 `IdentType` 里包含
`TransitionProperty`——所以 `TransitionProperty` 只暴露 `all` 并不妨碍使用，**不需要 cast**。

### cssom 没覆盖的部分：在 `io.github.shijianjs.ktjs.dom.css` 里补

不要因为「cssom 没有」就把整条规则退回文本，也不要让 `unsafeCast` 散到调用点。缺什么就在本模块的
`css` 包里按官方风格（`unsafeCast` 拼一次、值仍然类型化）补一个，调用点照常写类型：

| 缺的东西 | 补在哪 | 调用点形态 |
| --- | --- | --- |
| `radial-gradient(…)` | `css/Gradients.kt` | `radialGradient(RadialGradientShape.Circle, colorStop(glow), stop(Color.transparent, 70.pct))` |
| 无位置的颜色停靠点 | `css/Gradients.kt` | `colorStop(color)`——cssom 的 `stop` 总要带位置 |
| `-webkit-background-clip`（属性 + 名字） | `css/dsl/VendorProperties.kt` | `webkitBackgroundClip(BackgroundClip.text)`、`webkitBackgroundClipName` 给 supports 条件用 |
| `pointer-events: none` | `css/Values.kt` | `pointerEventsNone`（`PointerEvents` 生成出来只有 SVG 那批值） |
| `@supports` 条件类型 | `css/Supports.kt` | `SupportsQuery` + `supportsDeclaration(PropertyName<T>, T)` + `and/or/not`，照 `web.cssom.MediaQuery` 的形态 |
| `@supports` 块（唯一缺的 at-rule helper） | `css/dsl/AtRules.kt` | `supports(query) { … }`，关键字取 `AtRules.supports` |
| 伪类/伪元素常量 + 组合态 | `css/dsl/Pseudos.kt` | `Pseudos.hover` / `pseudo(…)` / `pseudos(…)`（64 个常量） |

补的规矩：**值仍然类型化**（枚举、工具类型、`Length`），只有「属性名」这种没法类型化的东西才写成字符串；
一个缺口一个函数或常量，不要提供 `putRaw(name, value)` 这类通用逃生口——那等于把字符串又请回来。

## 坑

### 伪类糖缺 `&`，而且表达不了组合态

`SimplePseudosRuleBuilder.hover` / `before` / `after` 写出的 key 是 `":hover"`、`"::before"`，没有 `&`。
stylis 会把它当成**后代**选择器：

```
hover {}      -> .probe-sugar :hover{…}     ← 不是 .probe-sugar:hover
"&:hover" {}  -> .probe-ampersand:hover{…}
```

根因在 kotlin-wrappers 的生成器：`karakum/csstype/Pseudos.kt` 的 `convertSimplePseudo` 把 csstype TS
联合里的字符串原样写进方法体，而同一套 API 里手写的 `RuleBuilder.and` 会加 `&`、`kotlin-css` 的
`CssBuilder.hover` 也一直写 `"&:hover"`。已报
[kotlin-wrappers#2933](https://github.com/JetBrains/kotlin-wrappers/issues/2933)。

**但不必等它修**：这个糖是「一个方法一个伪类」，就算补上 `&` 也写不出
`"&:hover, &:focus-visible"` 这种「多个状态共用一个块」的形式（本站的 `KtjsStep`、`KtjsAction` 都要）。
所以 `css/dsl/Pseudos.kt` 自建一层，形态照 `web.dom.TagName` + `web.html.HtmlTagName`：

- `external interface Pseudo` + `Pseudo(value)` 工厂；
- `object Pseudos` 列全 `SimplePseudosRuleBuilder` 的 64 个字符串，每个带 `&`（`Pseudos.hover` = `"&:hover"`）；
- `Pseudo.asSelector(): Selector` 转成 `Selector`；
- `context(RuleBuilder<T>) operator fun Pseudo.invoke(block)`——于是 `Pseudos.hover { … }` 的写法和官方
  `hover { … }` 一样，但带 `&`；
- `RuleBuilder.pseudo(pseudo, block)`（显式形式）与 `RuleBuilder.pseudos(vararg pseudos, block)`
  （后者用 `web.cssom.many` 把多个 `Selector` 拼成一条）；`many` 也可以直接拼多个 `Pseudo`；
- 列表外的选择器用工厂：`Pseudo("&:not(…)")`、`Pseudo("&::part(…)")`。

### 不要用 CSSOM 逐条 `insertRule` 代替序列化

那条路（`insertRule(".x {}")` + `rule.style` 赋值）也能拿到类型检查，但 CSSOM 对**同一规则里同一属性
只保留最后一个声明**：`fallback(a, b)` 阶梯会只剩最后一条。文本路径能保留阶梯，由浏览器解析时取舍。

### 产物与手写文本不是逐字相同

比对时先归一化，别断言整串：`#fff` → `#FFFFFF`、`0` → `0px`、逗号后可能没有空格、
`-webkit-background-clip` 与 `background-clip` 在 Chrome 里是同一属性的别名，文本里两条都在、从 CSSOM
读回来只剩一条、`transition: border-color 0.2s ease` 里的 `ease` 在 CSSOM 回读时会消失（文本路径不会）。
等价即可；要断言就断语义片段（见本模块 `jsTest` 里的 `CssSheetTest`）。

### 一份 sheet 服务所有实例

`cssSheet` 返回的 `CSSStyleSheet` 放进**组件类的 companion**，所有实例 `attachStyledShadow(HOME_RESET, SHEET)`。
不要在构造函数里现建：规则是静态的，一份就够。

## 本站的落地

首页元素都是这样写的：

| 文件 | 内容 |
| --- | --- |
| `HomeReset.kt` | 共享的那份 sheet：`*` / `*::before` / `*::after` / `:host` |
| `KtjsFeature.kt`、`KtjsStep.kt` | 卡片表面，外加 `&:hover` / `&:focus-visible` |
| `KtjsAction.kt` | `:host > a` 与两个 pill 类及其悬停态 |
| `KtjsHero.kt` | 伪元素光斑、`@supports` 渐变字与一个 `@media` 块（用到 `css` 包的 `radialGradient` / `webkitBackgroundClip`） |

迁移一个组件时的步骤：先把内联表达不了的那几条规则搬进 `cssSheet { }`，跑一次 `jsBrowserTest`，再把 sheet
读出来（`element.shadowRoot.adoptedStyleSheets`）与原来的文本做语义比对（注意上面那条归一化差异），确认
等价后删掉文本。

## 参考

- csstype 规则 DSL：`kotlin-csstype/src/webMain/kotlin/csstype/RuleBuilder.kt`（`Selector`/`ClassName`/`and`）、
  `.../generated/csstype/Simple*Pseudo*RuleBuilder.kt`（伪类糖，注意上面那条坑）
- cssom 取值类型：`kotlin-cssom-core/src/webMain/generated/web/cssom/`（`*Property.kt` 的类型、
  `*.functions.kt` 的工厂：`Border`、`BoxShadow`、`Transition`、`Gradient`、`Color`、`NamedColor`）
- cssom 手写辅助：`Reference.functions.kt`（`var`/`env`）、`many.kt`、`array.kt`、`important.kt`
- 序列化：`@emotion/serialize` 的 `serializeStyles`，`stylis` 的 `compile`/`serialize`/`middleware`/`stringify`
- 对照实验与结论：**ktjs-docusaurus** 仓库的 `probes/web-components`（`CssSheetExperiment`）
