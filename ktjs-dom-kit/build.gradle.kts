import org.jetbrains.kotlin.gradle.ExperimentalJsTestDsl

plugins {
    kotlin("multiplatform")
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

version = "0.1.0$versionSuffix"
description =
    "Generic Kotlin/JS DOM helpers: custom-element base class, shadow-root styles, typed inline styles."

kotlin {
    js {
        useEsModules()
        browser {
            // The attribute delegates drive real attributes on a real custom element, and the change
            // notifications come from a `MutationObserver`; neither can be faked convincingly in a
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
            // `api`: the public surface exposes kotlin-browser types (`HTMLElement`, `Element`, …)
            // and kotlin-csstype's `Properties`, the typed view of an element's inline style.
            api("org.jetbrains.kotlin-wrappers:kotlin-browser:2026.11.1")
            api("org.jetbrains.kotlin-wrappers:kotlin-csstype:2026.11.1-3.2.3")
            // `cssSheet` serializes csstype rules with emotion's own text pipeline: serializeStyles
            // writes declarations, stylis resolves the nested selectors. `implementation` since
            // neither type is in the public API.
            implementation(npm("@emotion/serialize", npmVersions.getValue("@emotion/serialize")))
            implementation(npm("stylis", npmVersions.getValue("stylis")))
            // The `dom.loadjs` package wraps this loader; it ships UMD only (`dist/loadjs.umd.js`),
            // which is why the declaration binds to the module's default export.
            implementation(npm("loadjs", npmVersions.getValue("loadjs")))
        }
        jsTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
