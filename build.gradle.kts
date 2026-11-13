import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import dev.detekt.gradle.extensions.DetektExtension
import groovy.json.JsonSlurper
import org.gradle.api.publish.PublishingExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmExtension

plugins {
    kotlin("multiplatform") version "2.4.21" apply false
    id("com.vanniktech.maven.publish") version "0.37.0" apply false
    id("dev.detekt") version "2.0.0-alpha.6" apply false
    id("org.jetbrains.dokka") version "2.3.0"
}

// A working tree is never a release. Every module derives the version of its *next* release, which
// Maven Central does not have, so the suffix marks it as unreleased: consumers can tell an artifact
// built here from one that was published, and a local publish can never shadow a real version.
// The release workflow passes `-Pktjs.release=true`, which drops the suffix for both the build and
// `printPublishedVersions`. Nothing else reads the flag.
val versionSuffix =
    if (providers.gradleProperty("ktjs.release").map(String::toBoolean).getOrElse(false)) "" else "-SNAPSHOT"
extra["versionSuffix"] = versionSuffix

// Every published library carries this prefix, which is also how publishing, Dokka and the version
// report find them. Keep it in sync with the module directories and settings.gradle.kts. The prefix
// deliberately has no trailing dash: it is what future libraries in this repository are named after,
// whatever they wrap.
val publishedPrefix = "ktjs-"

// Libraries that wrap no npm package have no upstream version to follow, so they carry a
// hand-written semantic version in their own build script instead of `{npm version}-kt.{revision}`.
// Everything else derives its version from package.json and wrapper-revisions.properties.
val semanticModules = setOf("ktjs-dom-kit")

fun isPublished(module: String): Boolean = module.startsWith(publishedPrefix)

// Dokka aggregates every published module into one API reference. `docs` is not part of the
// published API surface, so it is excluded by the prefix filter.
dependencies {
    subprojects
        .filter { isPublished(it.name) }
        .forEach { dokka(project(it.path)) }
}

dokka {
    dokkaPublications.html {
        moduleName.set("ktjs-dom-kit")
        outputDirectory.set(layout.buildDirectory.dir("dokka/html"))
    }
}

// GitHub-backed namespace; verify it once in the Central Portal. All modules share it.
// Setting the root `group` alone does not propagate to subprojects.
val publishedGroup = "io.github.shijianjs"

// npm package versions live in package.json so that Dependabot can update them.
// Subprojects read this map to declare npm(...) dependencies and derive their version.
@Suppress("UNCHECKED_CAST")
val npmVersions: Map<String, String> = run {
    val manifest = JsonSlurper().parse(file("package.json")) as Map<*, *>
    @Suppress("UNCHECKED_CAST")
    val dependencies = manifest["dependencies"] as Map<String, Any?>
    dependencies.mapValues { it.value.toString() }
}
extra["npmVersions"] = npmVersions

// npm-backed modules follow the upstream release: the base version comes from package.json and the
// per-module revision from wrapper-revisions.properties. A release bumps and publishes only the
// modules that changed, so unchanged ones keep a version that already exists on Central.
@Suppress("UNCHECKED_CAST")
val wrapperRevisions: Map<String, Int> = run {
    val properties = java.util.Properties()
    file("wrapper-revisions.properties").inputStream().use { properties.load(it) }
    properties.stringPropertyNames().associate { name ->
        name to properties.getProperty(name).trim().toInt()
    }
}
extra["wrapperRevisions"] = wrapperRevisions

// Keep a single webpack instance across the npm workspace: npm otherwise installs a second copy
// for source-map-loader's peer dependency, and the test bundle fails with
// "The 'compilation' argument must be an instance of Compilation".
plugins.withType<NodeJsRootPlugin> {
    extensions.getByType(NpmExtension::class.java).override("webpack", "5.108.1")
}

// The public API must use kotlin-browser's web.* types. Kotlin/JS's own org.w3c.dom.* and
// kotlinx.browser.* compile and run against the same objects, but the compiler treats them as
// unrelated types, which breaks callers that use kotlin-browser. detekt.yml forbids the imports.
// Kotlin/JS links every library module with IR dead-code elimination on by default, keeping only
// what is reachable from that module's own `@JsExport` roots. A library module has no such roots,
// so its `.mjs` output would be empty and any cross-module reference would resolve to `undefined`.
// Turning it off keeps the public API of each module in its output. It is applied to every KMP
// module — the docs site included — because each module is linked separately.
subprojects {
    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension> {
            compilerOptions {
                freeCompilerArgs.add("-Xir-dce=false")
            }

            // Custom elements need a real `class X extends HTMLElement`. At the default ES5 target
            // Kotlin emits `function X() { HTMLElement.call(this) }`, and a DOM constructor may only
            // be invoked with `new` — the browser throws "Please use the 'new' operator". ES2015 is
            // the first target that can express a class.
            //
            // It has to be set on the js target's compiler options; `-target es2015` through
            // `freeCompilerArgs` on the extension has no effect.
            targets
                .withType(org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl::class.java)
                .configureEach {
                    compilerOptions {
                        target = "es2015"
                    }
                }
        }
    }
}

subprojects {
    apply(plugin = "dev.detekt")

    extensions.configure<DetektExtension> {
        config.setFrom(rootProject.files("detekt.yml"))
        // The plain detekt task is wired into check; point it at the multiplatform source tree.
        source.setFrom(files("src"))
    }

    // Only published modules are documented; `docs` is the GitHub Pages site.
    if (isPublished(name)) {
        apply(plugin = "org.jetbrains.dokka")
    }
}

// Publishing is configured once for every module so that coordinates, POM metadata, signing and
// the Maven Central target cannot drift apart.
//
// klibs.io discovers libraries automatically: it reads the POM (url / scm.url must point at the
// GitHub repository) and requires the kotlin-tooling-metadata.json produced by the Kotlin
// Multiplatform plugin. Keep both intact when changing this block.
subprojects {
    if (!isPublished(name)) return@subprojects

    if (name !in semanticModules) {
        check(wrapperRevisions.containsKey(name)) {
            "Missing '$name' in wrapper-revisions.properties"
        }
    }

    val module = this

    group = publishedGroup

    apply(plugin = "com.vanniktech.maven.publish")

    // The subproject's Kotlin Multiplatform plugin is applied after this root script runs.
    // Choosing the javadoc jar earlier would hit the "no compatible plugin" branch and the
    // plugin's afterEvaluate default (Dokka HTML) would win.
    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        extensions.configure<MavenPublishBaseExtension> {
            // Maven Central requires a javadoc jar, but Dokka's HTML output duplicates its static
            // assets into every module's jar (~1.1 MB each). The API reference is deployed to the
            // Pages site, so publish the required empty jar instead of the HTML copy.
            configureBasedOnAppliedPlugins(javadocJar = JavadocJar.Empty())
        }
    }

    extensions.configure<MavenPublishBaseExtension> {
        publishToMavenCentral(automaticRelease = true)
        signAllPublications()

        // Gradle writes .md5/.sha1 next to every .asc signature. Maven Central does not need
        // them and they count toward the monthly file quota (KTC-5453, gradle/gradle#20232).
        excludeSignatureChecksums()

        pom {
            name.set(module.name)
            description.set(
                module.provider {
                    module.description ?: "Kotlin/JS library published from ktjs-dom-kit."
                }
            )
            url.set("https://github.com/shijianjs/ktjs-dom-kit")
            inceptionYear.set("2026")

            licenses {
                license {
                    name.set("MIT License")
                    url.set("https://github.com/shijianjs/ktjs-dom-kit/blob/main/LICENSE")
                }
            }

            developers {
                developer {
                    id.set("shijianjs")
                    name.set("shijianjs")
                    url.set("https://github.com/shijianjs")
                }
            }

            scm {
                url.set("https://github.com/shijianjs/ktjs-dom-kit")
                connection.set("scm:git:git://github.com/shijianjs/ktjs-dom-kit.git")
                developerConnection.set("scm:git:ssh://git@github.com/shijianjs/ktjs-dom-kit.git")
            }
        }
    }

    // A plain file repository produces the Maven repository layout — POMs, artifacts, sources,
    // javadoc and the `.asc` signatures — without contacting Central. The release workflow packages
    // one archive per coordinate from it and registers them with the hub.
    // Task name: publishAllPublicationsToHubStagingRepository
    extensions.configure<PublishingExtension> {
        repositories {
            maven {
                name = "hubStaging"
                url = module.layout.buildDirectory.dir("hub-staging").get().asFile.toURI()
            }
        }
    }
}

// Prints "<module>=<version>" for every publishable module. Use it to see which versions a release
// would upload before running publishToMavenCentral, so version derivation stays in the module
// scripts rather than being duplicated in shell.
tasks.register("printPublishedVersions") {
    val publishedModules = subprojects.filter { isPublished(it.name) }
    doLast {
        publishedModules
            .associate { it.name to it.version.toString() }
            .toSortedMap()
            .forEach { (name, version) -> println("$name=$version") }
    }
}

// Assembles the GitHub Pages site: the documentation at the root and the API reference under /api/.
// The docs site's `baseUrl` in Config.kt has to mirror this layout.
//
// `kotlinNpmInstall` is explicit: the docs module only links a library and unpacks the kit's bundle
// from a klib, so nothing else in this graph reaches the npm workspace, and `docusaurusBuild` needs
// `node_modules/@docusaurus/core` to exist. (A site that also builds a browser bundle gets it for
// free, because the webpack step installs npm; this one does not build one.)
tasks.register<Sync>("pagesSite") {
    dependsOn("kotlinNpmInstall", ":docs:docusaurusBuild", "dokkaGeneratePublicationHtml")
    into(layout.buildDirectory.dir("pages"))
    from(layout.buildDirectory.dir("js/docusaurus/docs/build"))
    from(layout.buildDirectory.dir("dokka/html")) {
        into("api")
    }
}
