# ktjs-dom-kit

<p align="center"><img src="docs/src/jsMain/resources/static/img/ktjs-dom-kit-logo.png" width="120" alt="ktjs-dom-kit"></p>

DOM helpers for Kotlin/JS: a custom-element base class, an attribute delegate that keeps the DOM as the
single source of truth, shadow-root style helpers, typed stylesheets, and a wrapper for the `loadjs`
loader. No UI framework, no template language, no state-driven re-render.

The repository is also the home of the small libraries that do not need a repository of their own.

[English](README.md) | [简体中文](README.zh-CN.md)

**[Documentation](https://shijianjs.github.io/ktjs-dom-kit/)** ·
**[API reference](https://shijianjs.github.io/ktjs-dom-kit/api/)**

## Modules

| Module | Coordinate | Version |
| --- | --- | --- |
| [ktjs-dom-kit](ktjs-dom-kit) | `io.github.shijianjs:ktjs-dom-kit` | `0.1.0` |

```kotlin
implementation("io.github.shijianjs:ktjs-dom-kit:0.1.0")
```

## Versioning

A library that wraps no npm package carries a hand-written semantic version in its own build script
(`ktjs-dom-kit` is one). Everything else follows the wrapper rule, `{upstream npm version}-kt.{revision}`:
the upstream version is declared once in the root [`package.json`](package.json) and the revision is
read per module from [`wrapper-revisions.properties`](wrapper-revisions.properties).

- Only `-kt.N` wrapper versions are published; a version without the suffix never exists.
- A working tree always adds `-SNAPSHOT`: the derived number is the *next* release, so development
  builds never impersonate it. Publishing passes `-Pktjs.release=true`, which drops the suffix.
- Revisions are tracked per module and only ever increase. A release bumps and publishes only the
  modules that changed.

Libraries added later are Gradle modules named after their Maven artifact; the root build collects
every `ktjs-` project for publishing, Dokka and the version report, so the only edits are the module
directory, one `include` in [`settings.gradle.kts`](settings.gradle.kts) and its documentation.

## Requirements

- JDK 21 or newer (the build uses Gradle 9.4.1).
- Node and npm are downloaded automatically by the Kotlin Gradle plugin; `kotlin.js.yarn=false` in
  `gradle.properties` keeps npm as the package manager.
- Chromium is downloaded by Playwright the first time browser tests run.

## Build and test

```bash
./gradlew build                       # compile, Detekt, browser tests
./gradlew :ktjs-dom-kit:jsBrowserTest

# Documentation site: Kotlin configuration, Markdown pages, built by ktjs-docusaurus
./gradlew :docs:docusaurusBuild

# GitHub Pages site (documentation at the root, Dokka output under /api/)
./gradlew pagesSite
```

After adding or upgrading an npm dependency, refresh the shared lock file:

```bash
./gradlew kotlinUpgradePackageLock
```

The `docs` module is a Kotlin/JS project too. All three ktjs-docusaurus versions live in
`gradle.properties` — the wrapper klib, the Gradle plugin and the kit whose home sections the landing
page uses — because each of those modules carries its own revision. A SNAPSHOT there is resolved from
`mavenLocal`, so building the site against an unreleased wrapper means publishing that wrapper locally
first. The kit's browser bundle is not a Maven artifact: it ships as a resource of the kit's klib, and
`extractKitBundle` copies it to the site's `static/ktjs-client/`.

## Publishing

Publishing is configured once in the root [`build.gradle.kts`](build.gradle.kts) for all modules.
Releases run through `.github/workflows/release.yml`: it signs the artifacts, packages one archive per
Maven coordinate and registers them with the shared Maven Central hub, which merges the pending
registrations into a single deployment.

## License

Released under the [MIT License](LICENSE).
