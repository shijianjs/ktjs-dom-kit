# ktjs-dom-kit

<p align="center"><img src="docs/src/jsMain/resources/static/img/ktjs-dom-kit-logo.png" width="120" alt="ktjs-dom-kit"></p>

用 Kotlin/JS 手写 DOM 组件时用的辅助库：自定义元素基类、把 DOM 当作唯一状态来源的属性委托、shadow root
样式辅助、类型化样式表，以及 `loadjs` 加载器的封装。没有 UI 框架、没有模板语言、没有「状态驱动重渲染」。

这个仓库也是那些不值得单独开仓库的小库的落脚点。

[English](README.md) | [简体中文](README.zh-CN.md)

**[文档](https://shijianjs.github.io/ktjs-dom-kit/)** ·
**[API 文档](https://shijianjs.github.io/ktjs-dom-kit/api/)**

## 模块

| 模块 | 坐标 | 版本 |
| --- | --- | --- |
| [ktjs-dom-kit](ktjs-dom-kit) | `io.github.shijianjs:ktjs-dom-kit` | `0.1.0` |

```kotlin
implementation("io.github.shijianjs:ktjs-dom-kit:0.1.0")
```

## 版本约定

不包装 npm 包的库用手写的语义版本，版本号写在它自己的构建脚本里（`ktjs-dom-kit` 就是这种）。其余模块沿用
wrapper 的规则 `{上游 npm 版本}-kt.{修订号}`：上游版本只在根 [`package.json`](package.json) 声明，每个模块
的修订号在 [`wrapper-revisions.properties`](wrapper-revisions.properties) 维护。

- 只发布带 `-kt.N` 后缀的版本，不存在不带后缀的版本。
- 工作树里的版本一律再带 `-SNAPSHOT`：推导出的是「下一个要发的版本」，Central 上还没有，开发构建不冒用它。
  发布时才用 `-Pktjs.release=true` 去掉后缀。
- 修订号按模块维护，只增不减。发布时只发修订号变化的模块。

之后新增的库都是以 Maven 构件命名的 Gradle 模块；根构建按 `ktjs-` 前缀收集发布、Dokka 与版本报表，所以只需
加模块目录、在 [`settings.gradle.kts`](settings.gradle.kts) 加一行 `include`，再写它的文档。

## 环境要求

- JDK 21 或更高（构建使用 Gradle 9.4.1）。
- Node 与 npm 由 Kotlin Gradle 插件自动下载；`gradle.properties` 里的 `kotlin.js.yarn=false` 保证包管理器
  是 npm。
- 第一次跑浏览器测试时由 Playwright 下载 Chromium。

## 构建与测试

```bash
./gradlew build                       # 编译、Detekt、浏览器测试
./gradlew :ktjs-dom-kit:jsBrowserTest

# 文档站：Kotlin 配置 + Markdown 页面，由 ktjs-docusaurus 构建
./gradlew :docs:docusaurusBuild

# GitHub Pages 站点（根目录是文档，API 文档在 /api/）
./gradlew pagesSite
```

新增或升级 npm 依赖后，刷新共享锁文件：

```bash
./gradlew kotlinUpgradePackageLock
```

`docs` 模块同样是 Kotlin/JS 项目。三个 ktjs-docusaurus 版本都写在 `gradle.properties` 里：wrapper klib、
Gradle 插件、以及首页分区用到的工具箱——那个项目的每个模块各自带 `-kt.N` 修订号。写 SNAPSHOT 时从
`mavenLocal` 解析。工具箱的浏览器那一半不是独立的 Maven 产物，而是 kit klib 里的资源，由
`extractKitBundle` 解到站点的 `static/ktjs-client/`。

## 发布

发布配置统一在根 [`build.gradle.kts`](build.gradle.kts) 里，对所有模块生效。`.github/workflows/release.yml`
签名、按 Maven 坐标打包并注册到共享的 Maven Central hub，由 hub 合并成一次 deployment。

## 许可证

[MIT](LICENSE)
