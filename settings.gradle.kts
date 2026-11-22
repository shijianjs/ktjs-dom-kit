pluginManagement {
    repositories {
        // The docs site is built with the ktjs-docusaurus Gradle plugin. Released versions come from
        // Central; `mavenLocal` is what serves a SNAPSHOT one, which is how the site is developed
        // against an unreleased wrapper. An empty local repository falls through to the next
        // repository, so the order costs nothing in CI.
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
    }

    // The plugin's version lives in gradle.properties next to the wrapper's: both come from
    // ktjs-docusaurus, but each of its modules carries its own `-kt.N` revision.
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "io.github.shijianjs.ktjs.docusaurus") {
                useVersion(providers.gradleProperty("ktjs-docusaurus-gradle-plugin.version").get())
            }
        }
    }
}

dependencyResolutionManagement {
    repositories {
        // `mavenLocal` is only here for this account's own SNAPSHOT wrappers, and it is limited to
        // them on purpose: `~/.m2` also collects POM-only copies from non-Gradle tools, and a copy
        // without the `.module` file has no variant metadata. `kotlin-stdlib` resolved from such a
        // copy loses its JS variant, so the JS compile ends with "Missing stdlib class" / hundreds of
        // unresolved `kotlin.*` references instead of an honest "artifact not found".
        mavenLocal {
            content { includeGroupByRegex("io\\.github\\.shijianjs(\\..*)?") }
        }
        // Maven Central is the source of truth for kotlin-wrappers: the Aliyun mirror lags behind
        // and sometimes serves metadata without the corresponding klibs. Gradle does not fall back
        // to the next repository once metadata is found, so pin these modules to Central even when
        // a local mirror is active.
        exclusiveContent {
            forRepository { mavenCentral() }
            filter {
                includeGroup("org.jetbrains.kotlin-wrappers")
                includeModule("org.jetbrains.kotlin", "kotlin-js-plain-objects")
                // the stdlib half of the same trap: its JS klib only exists on Central
                includeGroup("org.jetbrains.kotlin")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "ktjs-dom-kit"

// One Gradle project per published library. `ktjs-dom-kit` is the first one: generic DOM helpers
// with no npm package behind them. Libraries added later follow the same rule — a directory named
// after the Maven artifact, listed here, and nothing else to change: publishing, Dokka and the
// version report pick up every `ktjs-*` project automatically.
include(":ktjs-dom-kit")

// The KSP processor that turns a class's `by attr` declarations into the constants and the
// `observedAttributes` array it registers with. It has to be a JVM module: KSP loads the processor
// inside the compiler, which cannot consume a Kotlin/JS library.
include(":ktjs-dom-kit-ksp")

// The wrapper-facing half of the DOM layer: the attribute objects, element helpers and the
// custom-element base, with no npm dependency. Wrapper libraries (`ktjs-vaadin`, `ktjs-iconify`, …)
// depend on this one and stay free of emotion's serializer and stylis; `ktjs-dom-kit` depends on it
// with `api` and adds the typed-stylesheet half and the script loader.
include(":ktjs-wrapper-kit")

// The demo layer shared by every demo site in this account: shell, catalog, router, pages.
include(":ktjs-demo-kit")

// The documentation site: Kotlin configuration plus Markdown, assembled by the ktjs-docusaurus
// Gradle plugin into a static site that `pagesSite` publishes at the Pages root.
// The interactive demo: the same shell every demo site uses, over this library.'s own APIs.
include(":demo")

include(":docs")
