import org.jetbrains.kotlin.gradle.ExperimentalJsTestDsl

plugins {
    kotlin("multiplatform")
    id("com.google.devtools.ksp")
}

/**
 * Generic DOM helpers for Kotlin/JS.
 *
 * `CustomElementAdapter` is the base for custom elements written in Kotlin; `attachShadowRoot` /
 * `attachStyledShadow` attach a shadow root (the latter adopting constructed `CSSStyleSheet`s), and
 * `HTMLElement.inlineStyle` is the typed view used to style a node in place.
 *
 * It wraps no npm package, so the version is its own semantic one instead of the
 * `{npm version}-kt.{revision}` scheme the npm-backed libraries in this repository follow.
 *
 * The module shares the root build's KMP settings (`-Xir-dce=false`, ES2015 target, detekt) through
 * the `subprojects` blocks, so it needs no local copy of those workarounds.
 */
// Empty in a release build, `-SNAPSHOT` in a working tree: see the note in the root build script.
@Suppress("UNCHECKED_CAST")
val versionSuffix = rootProject.extra["versionSuffix"] as String
@Suppress("UNCHECKED_CAST")
val npmVersions = rootProject.extra["npmVersions"] as Map<String, String>

version = "0.2.0$versionSuffix"
description =
    "Generic Kotlin/JS DOM helpers: custom-element base class, shadow-root styles, typed inline styles."

kotlin {
    js {
        useEsModules()
        browser {
            // The attribute delegates drive real attributes on a real custom element, and the change
            // notifications come from the browser's own `attributeChangedCallback`, so the tests need
            // a tag that is really defined and really upgraded; neither can be faked convincingly in a
            // Node test. Playwright's Chromium runs these browser tests (Karma is deprecated).
            @OptIn(ExperimentalJsTestDsl::class)
            test {
                chromium()
            }
        }
        binaries.library()
    }

    compilerOptions {
        optIn.add("kotlin.js.ExperimentalWasmJsInterop")
    }

    sourceSets {
        jsMain.dependencies {
            // The attribute layer, element helpers and the custom-element base live in
            // `ktjs-wrapper-kit` so wrapper libraries can use them without pulling emotion's
            // serializer. `api`: this module is meant to hand out the whole DOM API to page code.
            api(project(":ktjs-wrapper-kit"))
            // `api`: the public surface exposes kotlin-browser types (`HTMLElement`, `Element`, …)
            // and kotlin-csstype's `Properties`, the typed view of an element's inline style.
            api(libs.kotlin.browser)
            api(libs.kotlin.csstype)
            // `cssSheet` serializes csstype rules with emotion's own text pipeline: serializeStyles
            // writes declarations, stylis resolves the nested selectors. `implementation` since
            // neither type is in the public API.
            implementation(npm("@emotion/serialize", npmVersions.getValue("@emotion/serialize")))
            implementation(npm("stylis", npmVersions.getValue("stylis")))
            // `dom.loadjs` is written in Kotlin — no npm package, nothing to bind: the upstream
            // source is ~320 lines of stable ES5, so the loader is owned here instead of resolved.
            //
            // `dom.loadjs` is `suspend`, and a page awaits it from `launch { Loadjs(…) }`; the module
            // itself uses `coroutineScope`/`async` for the list form. `api` so the coroutines library
            // comes with the module instead of being a line every consumer has to add. kotlin-browser
            // only declares it `implementation`, hence the explicit coordinate.
            api(libs.kotlinx.coroutines.core)
        }
        jsTest.dependencies {
            implementation(kotlin("test"))
            // The loader suspends, so its cases are written as `runTest { … }`.
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

// The test probe is the repository's only element that declares `by attr` properties, so the
// processor runs over the test source set: `AttributeProbeAttributes` is generated next to it and
// the probe registers that array. `kspJs` is wired as well — this module has no element of its own,
// so it generates nothing, but it is the line an application module copies for its own classes.
dependencies {
    add("kspJs", project(":ktjs-dom-kit-ksp"))
    add("kspJsTest", project(":ktjs-dom-kit-ksp"))
}
