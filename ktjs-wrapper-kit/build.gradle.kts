import org.jetbrains.kotlin.gradle.ExperimentalJsTestDsl

plugins {
    kotlin("multiplatform")
}

/**
 * The wrapper-facing half of this repository: **only** what works on an element you do not own.
 *
 * A wrapper library (`ktjs-vaadin`, `ktjs-iconify`, …) re-declares components written elsewhere, and
 * all it needs from the kit is the attribute layer — `Attribute`, `TokenListAttribute`,
 * `JsonAttribute`, the codecs and `observe`. Everything else in the kit serves code that *authors*
 * elements or pages, and is either npm-backed or still moving:
 *
 * - `dom.webcomponent` (bridge stripping, shadow roots), `dom.element` (`createElement`,
 *   `inlineStyle`) and the `attr` delegate stay in `ktjs-dom-kit`. They are the parts most likely to
 *   change as the kit's own components evolve, and a wrapper has no use for them.
 * - `dom.css` (emotion + stylis) and `dom.loadjs` are npm-backed, and a wrapper should not inherit
 *   that dependency graph for a `Card.headerAttr`.
 *
 * Package names are unchanged on purpose (`io.github.shijianjs.ktjs.dom.attribute`): this is an
 * artifact split, not an API rename, so existing imports keep working whichever artifact they come
 * from. The leaf of the graph is this module plus `kotlin-browser`; nothing here pulls an npm package.
 */
// Empty in a release build, `-SNAPSHOT` in a working tree: see the note in the root build script.
@Suppress("UNCHECKED_CAST")
val versionSuffix = rootProject.extra["versionSuffix"] as String

version = "0.1.0$versionSuffix"
description =
    "Typed attributes for elements owned elsewhere: attribute objects, token lists, JSON attributes, " +
        "codecs and change subscriptions."

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
            // `api`: the public surface is expressed in kotlin-browser's types (`HTMLElement`).
            // No kotlin-csstype and no npm package: this is the graph a wrapper library inherits.
            api("org.jetbrains.kotlin-wrappers:kotlin-browser:2026.11.2")
        }
        jsTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
