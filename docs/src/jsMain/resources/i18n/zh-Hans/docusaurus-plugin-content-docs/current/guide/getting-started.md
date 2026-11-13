---
sidebar_position: 1
---

# 快速开始

## 加依赖

```kotlin
kotlin {
    js {
        browser()
    }
    sourceSets {
        jsMain.dependencies {
            implementation("io.github.shijianjs:ktjs-dom-kit:0.1.0")
        }
    }
}
```

`kotlin-browser` 与 `kotlin-csstype` 作为 `api` 依赖一起带来：库的公开接口就是用它们的类型写的
（`web.html.HTMLElement`、`csstype.Properties`），所以已经在用 `kotlin-wrappers` 的项目不会多出一套 DOM
类型。

## 第一个元素

一个组件 = 一个继承 `CustomElementAdapter` 的类 + 一个标签名 + 一次 `defineIfMissing`：

```kotlin
class KtjsChip : CustomElementAdapter() {
    private val label = HtmlTagName.span.createElement {
        inlineStyle.display = Display.block
    }

    /** `text`：胶囊上的文字。 */
    @JsName("text")
    var text: String by attr { change -> label.textContent = change.newValue }

    init {
        // 构造函数不能碰 `this`，但 shadow root 是它自己的一棵树。
        val shadow = attachStyledShadow(SHEET)
        shadow.appendChild(label)
    }

    override fun connectedCallback() {
        // 已经解析出来的标记从未经过上面的属性 setter。
        applyInitialAttributes()
    }

    companion object : CustomElementReference<KtjsChip>(KtjsChip::class.js) {
        val NAME: TagName<KtjsChip> = TagName("ktjs-chip")

        private val SHEET: CSSStyleSheet = cssSheet {
            ":host" {
                display = Display.block
                padding = Padding(0.5.rem)
            }
        }
    }
}

fun registerKtjsChip() = defineIfMissing(KtjsChip.NAME, KtjsChip.Companion)
```

`defineIfMissing` 在注册标签之前先删掉编译器为外部 DOM 成员生成的自递归转发方法，元素体里这才可以用
`appendChild` / `querySelectorAll` 的原生拼写。漏掉这一步，元素里第一次调 DOM 方法就会一路递归到爆栈——
细节见[自定义元素](../dom-kit/custom-elements.md)。

之后这个元素就和其他自定义元素一样接受标记与属性：

```html
<ktjs-chip text="0.1.0"></ktjs-chip>
<script type="module" src="./ktjs-chip.mjs"></script>
```

## 在本仓库里开发

构建是 Gradle 多项目：库模块是 `ktjs-dom-kit`，站点是 `docs`。

```bash
./gradlew build                       # 编译、Detekt、浏览器测试
./gradlew :ktjs-dom-kit:jsBrowserTest
./gradlew :docs:docusaurusBuild       # 文档站
./gradlew pagesSite                   # 文档在 /，Dokka 在 /api/
./gradlew :docs:docusaurusStart       # 开发服务器
```

`build` 不组装 Pages 站点，那是 Pages 工作流干的事。

| 路径 | 内容 |
| --- | --- |
| `package.json` | npm 版本的唯一声明处（由 Dependabot 维护） |
| `ktjs-dom-kit/src/jsMain/kotlin` | 库的声明 |
| `ktjs-dom-kit/src/jsTest/kotlin` | 对应的浏览器测试 |
| `docs/src/jsMain/kotlin` | 站点配置与侧边栏 |
| `docs/src/jsMain/resources` | Markdown 页面、MDX 首页、CSS、翻译 |

Chromium 在第一次跑浏览器测试时由 Playwright 下载；测试本身只需要 JDK 21，npm 由 Kotlin Gradle 插件装进
workspace。

## 锁文件

npm 依赖对整个 workspace 只解析一次，钉在 `kotlin-js-store/package-lock.json`。新增或升级 npm 依赖、或者改了
模块版本之后要重新生成：

```bash
./gradlew kotlinUpgradePackageLock
```

CI 会校验提交进去的这份文件，漏了就构建失败，不会悄悄漂移。
