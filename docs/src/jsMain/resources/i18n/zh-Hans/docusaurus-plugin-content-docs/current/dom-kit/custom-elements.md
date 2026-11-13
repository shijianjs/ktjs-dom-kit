---
sidebar_position: 2
---

# 自定义元素

一个组件就是一个继承 `CustomElementAdapter` 的 Kotlin 类，用 [`attr`](./attributes.md) 声明它的属性，
再用 `defineIfMissing` 注册标签。没有 JS 类字面量，没有组件库，也不需要给生命周期钩子起别名。

```kotlin
class KtjsBadge : CustomElementAdapter() {

    private val anchor = TagName<HTMLAnchorElement>("a").createElement()

    var href: String by attr { change -> anchor.href = change.newValue }

    init {
        // 构造函数里可以挂 shadow root；但不能往元素自己身上加子节点
        attachShadowRoot().append(anchor)
    }

    override fun connectedCallback() {
        applyInitialAttributes()
    }

    companion object : CustomElementReference<KtjsBadge>(KtjsBadge::class.js) {
        val NAME: TagName<KtjsBadge> = TagName("ktjs-badge")
    }
}

defineIfMissing(KtjsBadge.NAME, KtjsBadge.Companion)
```

## 基类做了什么

`CustomElementAdapter : HTMLElement()` 接过了每个组件否则都要重复的三件事：

1. **生命周期钩子**，按原生名字声明成可 override 的方法：`connectedCallback`、
   `connectedMoveCallback`、`disconnectedCallback`、`adoptedCallback`、`attributeChangedCallback`。
   元素类写一句 `override fun connectedCallback()`，浏览器就会调用它。
2. **子类的 `attr` 委托**。每个属性初始化时把自己的委托注册到元素上，元素上的一个 `MutationObserver`
   把之后的每次属性变化喂回它们，所以属性与 attribute 不可能对不上。详见[属性](./attributes.md)。
3. **删桥**，即下面的 `stripBridgesFrom`。

`attributeChangedCallback` 是为完整性声明的：它只为类里静态 `observedAttributes` 列出的名字触发，而那
是元素类自己的事。`attr` 委托不走它——委托不是静态声明，而且对未挂载的元素也要能用。

## 为什么不实现官方的 `CustomElement` 接口

`web.components.CustomElement` 把钩子声明成 `val`。Kotlin 会把 `override val connectedCallback` 编译成
**getter**，而浏览器只认方法，于是元素渲染出来、回调静默不执行——一个没有报错可以搜的故障。把同样的名字
声明成真函数才能被调用；只在基类里做一次，正是不必给每个类起 `onConnected` 之类别名的原因。

## 自递归桥

Kotlin/JS 会为类继承的每一个外部 DOM 成员（`ParentNode`、`ChildNode`、`Animatable`、`HTMLOrSVGElement`、
`EventTargetLike`……）生成一个转发成员，而这个成员调用它自己：

```js
querySelector(selectors) { return this.querySelector(selectors); }
```

`connectedCallback` 里第一句走到这些名字——`querySelector`、`querySelectorAll`、`append`、`animate`、
`focus`、`addEventListener`……——就会递归到栈溢出。`getAttribute` / `setAttribute` / `hasAttribute` /
`textContent` 不需要处理：它们是外部 `Element` 类的抽象成员，根本不会生成桥。

上游对应 [kotlin-wrappers#2932](https://github.com/JetBrains/kotlin-wrappers/issues/2932)，编译器侧的
解释也在那里：桥是为在外部**接口**上声明为 `= definedExternally` 的成员生成的（外部**类**的成员如
`getAttribute` 是抽象的，在 `ABSTRACT` 判断处就返回了），桥的名字与它自己的目标同名，于是里面的调用解析
回它自己。JVM 后端对这个模式有防护（`isErroneousSpecialBridge`，KT-82651），JS 后端没有，所以在它修好
之前这个绕法只能留在本地。

`stripBridgesFrom(klass)` 从类的原型链上一层层删掉这些名字，删到 `HTMLElement.prototype` 之前为止——那里
才是真正的 DOM 方法。删一个本来就没被桥接的名字是空操作，所以名单可以比某个类实际需要的更宽。它刻意走完整
条链：Kotlin 给**每一层**类都生成桥，只清 `CustomElementAdapter` 会留下元素类自己的那一份。

之后原生写法直接可用——`querySelectorAll(...)`、`append(...)`、`animate(...)`——不需要 `…Super` 转发。
（override 成员上也不允许写 `@JsName` 别名。）

## 定义标签

```kotlin
defineIfMissing(KtjsBadge.NAME, KtjsBadge.Companion)
```

`defineIfMissing(tag, reference)` 先跑 `stripBridgesFrom`，再 `customElements.define`，标签已注册时直接
返回。顺序是有原因的，也是它比直接调 `customElements.define` 更该用的理由：定义标签会升级文档里已经存在
的同类元素，它们的 `connectedCallback` 立即执行——如果那时桥还在，就是一次栈溢出。重复调用是安全的，这才
让它可以放在可能被求值多次的模块里。

| 部件 | 来自 |
| --- | --- |
| `NAME: TagName<T>` | 元素自己的 `companion object`；标签名每个元素只写一处 |
| `CustomElementReference<T>(T::class.js)` | `web.components`；companion object **就是**传给 `customElements.define` 的那个引用 |
| `defineIfMissing` | `ktjs-dom-kit` |

## 创建元素

`TagName<T>.createElement { … }` 包装了类型化的 `document.createElement(TagName<T>)`：标签名决定属性类型，
回调不需要 cast 就能拿到：

```kotlin
val action = KtjsAction.NAME.createElement {
    kind = ActionKind.Primary      // MDX 里写 kind="primary"，落到同一个属性
    href = "docs/intro"
    textContent = "Get started"
}
```

## 构造函数的规矩

自定义元素规范禁止构造函数往元素自己身上加 attribute 或子节点。`attachShadow()` 是明确允许的，所以组件把
子树建进 shadow root：

```kotlin
class KtjsBadge : CustomElementAdapter() {
    init {
        attachShadowRoot().append(anchor)      // 可以：shadow root 不是元素本身
        // this.setAttribute("…", "…")         // createElement 时抛 NotSupportedError
        // this.append(anchor)                 // 同样
    }
}
```

两个连带结论值得记住：

- **不要在构造函数里给元素自己设属性。** 构造完就带属性时 `document.createElement("ktjs-badge")` 会抛
  `NotSupportedError`（解析器和升级路径不会）。元素永远需要的样式因此写成自己 sheet 里的 `:host` 规则，
  而不是 `style` 属性。
- **监听器跟着归属走。** 挂在组件自有节点上的监听器写在构造函数里，不需要清理；挂在 `document`、
  `window`、`matchMedia` 上的必须写在 `disconnectedCallback` 里并用标志位保证幂等——元素移动会再触发一次
  `connectedCallback`。
