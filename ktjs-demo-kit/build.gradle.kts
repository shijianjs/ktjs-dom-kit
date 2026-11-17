plugins {
    kotlin("multiplatform")
}

/**
 * Reusable building blocks for a demo site: an application shell, a page catalog, a hash router, the
 * language state, the page contract and the blocks a page is built from.
 *
 * It is not a wrapper — it has no upstream npm package to follow — so it keeps its own semantic
 * version. Publishing, signing and the POM metadata are configured once for every published module
 * in the root build.
 *
 * The module shares the root build's KMP settings (ES2015 target, `-Xir-dce=false`, detekt) through
 * the `subprojects` blocks, so it needs no local copy of those workarounds.
 */
@Suppress("UNCHECKED_CAST")
val versionSuffix = rootProject.extra["versionSuffix"] as String

version = "0.1.0$versionSuffix"
description = "Reusable Kotlin/JS building blocks for demo sites: shell, catalog, router, pages."

kotlin {
    js {
        useEsModules()
        browser()
        binaries.library()
    }

    compilerOptions {
        optIn.add("kotlin.js.ExperimentalWasmJsInterop")
    }

    sourceSets {
        jsMain.dependencies {
            // `api`: the public surface exposes kotlin-browser types and the vaadin wrappers' element
            // classes, so consumers write against them without an extra dependency line.
            api(project(":ktjs-dom-kit"))
            api("org.jetbrains.kotlin-wrappers:kotlin-browser:2026.11.1")
            api("io.github.shijianjs:ktjs-vaadin-web-components:25.3.5-kt.4")
        }
        jsTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
