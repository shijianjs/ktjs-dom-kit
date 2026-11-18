import org.jetbrains.kotlin.gradle.ExperimentalJsTestDsl

plugins {
    kotlin("multiplatform")
}

/**
 * The wrapper-facing half of this repository: everything that works on an element you do not own.
 *
 * `ktjs-dom-kit` grew out of two different needs, and they pull different dependencies apart:
 *
 * - **Wrapper libraries** (`ktjs-vaadin`, `ktjs-iconify`, …) re-declare components written elsewhere.
 *   What they need is the attribute layer (`Attribute`, `TokenListAttribute`, the codecs, `attr`
 *   delegation), element helpers and the custom-element base — all of it pure Kotlin/JS on top of
 *   `kotlin-browser`, with no npm package behind it.
 * - **Page code** adds the parts that do need npm: typed stylesheets through `@emotion/serialize` +
 *   `stylis`, and `loadjs` for loading third-party bundles.
 *
 * Shipping both in one artifact made every wrapper consumer pull emotion and loadjs for a
 * `Card.headerAttr`. Keeping the npm-backed half in `ktjs-dom-kit` and the pure half here reverses
 * that: a wrapper depends on this module and stays npm-free, and `ktjs-dom-kit` still exposes the
 * whole API to pages by depending on it with `api`.
 *
 * Package names are unchanged on purpose (`io.github.shijianjs.ktjs.dom.attribute`,
 * `…dom.element`, `…dom.webcomponent`): this is an artifact split, not an API rename, so existing
 * imports keep working whichever artifact they come from.
 */
// Empty in a release build, `-SNAPSHOT` in a working tree: see the note in the root build script.
@Suppress("UNCHECKED_CAST")
val versionSuffix = rootProject.extra["versionSuffix"] as String

version = "0.1.0$versionSuffix"
description =
    "Attribute and element wrappers for elements owned elsewhere: typed attribute objects, " +
        "token lists, the attr delegate and the custom-element base."

kotlin {
    js {
        useEsModules()
        browser {
            // The attribute objects read and write real attributes of a real element, and the change
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
        }
        jsTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
