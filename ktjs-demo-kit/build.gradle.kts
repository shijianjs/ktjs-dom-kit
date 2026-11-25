plugins {
    kotlin("multiplatform")
    id("com.google.devtools.ksp")
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
            api(libs.kotlin.browser)
            // The unreleased wrapper revision that adds `lumoutility.LumoUtility` and the typed
            // `theme` variants. Versions here stay SNAPSHOT until the release batch: every module is
            // published to mavenLocal and consumed from there, so nothing reaches Central before the
            // user signs off on the whole migration.
            api(libs.ktjs.vaadin.web.components)
            // Icons in the sidebar are `<iconify-icon>` elements. The type never leaves this module
            // (the catalog only carries icon-name strings), so a plain implementation is enough.
            implementation(libs.ktjs.iconify.icon)
            // Menu items are plain JS objects (`js.objects.unsafeJso`), which live in kotlin-wrappers'
            // `kotlin-js` module; the old standalone `plain-objects` artifact stops at Kotlin 2.2.
            implementation(libs.kotlin.js)
        }
        jsTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

// Every element in this module names its tag by hand in its reference — `ktjs-demo-layout`, `ktjs-demo-page`,
// … With the processor, `ktjs-dom-kit-ksp` writes the `…DefaultReference` the companion extends, so the tag
// is the class name in kebab case behind the default `ktjs-` prefix and a companion is one line. The one
// class whose tag is not its own name (`SectionBlock` → `ktjs-demo-section`) passes `name =` instead.
dependencies {
    add("kspJs", project(":ktjs-dom-kit-ksp"))
}

ksp {
    // The default name is derived from the tag prefix (`ktjs-` → `KtjsCustomElements`), which reads as the
    // library family rather than this kit; this module is the demo kit, so it says so.
    arg("ktjs.registryName", "DemoCustomElements")
}
