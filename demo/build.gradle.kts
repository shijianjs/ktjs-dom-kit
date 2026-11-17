// Referenced with imports rather than fully qualified names: inside a Gradle script `java` is the
// JavaPluginExtension accessor, so `java.awt.…` does not resolve.
import java.awt.RenderingHints
import java.awt.image.BufferedImage

plugins {
    kotlin("multiplatform")
}

// -----------------------------------------------------------------------------------------------
// Lumo styles
//
// The shell is built from the published Vaadin wrapper, which only ships the JavaScript side of
// `@vaadin/vaadin-lumo-styles`; the CSS itself is a page-level `<link>`, taken from the npm
// workspace the Kotlin/JS plugin installs at the root.
// -----------------------------------------------------------------------------------------------
val lumoStyles = rootProject.layout.buildDirectory.dir("js/node_modules/@vaadin/vaadin-lumo-styles/dist")

val copyLumoStyles = tasks.register<Copy>("copyLumoStyles") {
    description = "Copies the pre-built Lumo stylesheet into the demo resources."
    dependsOn(":kotlinNpmInstall")
    from(lumoStyles) {
        include("lumo.css")
        include("utility.css")
    }
    into(layout.buildDirectory.dir("generated/lumo"))
}

// -----------------------------------------------------------------------------------------------
// Site mark
//
// There is exactly one logo file in the repository (the one the README and the documentation site
// show), so the demo does not keep a second copy. It does not ship the 1254px original either: the
// navbar draws it at 28px, so the build writes a 128px rendering into the demo resources.
// -----------------------------------------------------------------------------------------------
val logoPixels = 128

val scaleLogo = tasks.register("scaleLogo") {
    description = "Scales the repository logo down into the demo resources."
    val source = rootProject.file("docs/src/jsMain/resources/static/img/ktjs-dom-kit-logo.png")
    val outputDir = layout.buildDirectory.dir("generated/logo")
    inputs.file(source)
    outputs.dir(outputDir)
    doLast {
        val target = outputDir.get().asFile.resolve("img/ktjs-dom-kit-logo.png")
        target.parentFile.mkdirs()
        val original = javax.imageio.ImageIO.read(source)
        val scaled = BufferedImage(logoPixels, logoPixels, BufferedImage.TYPE_INT_ARGB)
        val graphics = scaled.createGraphics()
        graphics.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BILINEAR,
        )
        graphics.drawImage(original, 0, 0, logoPixels, logoPixels, null)
        graphics.dispose()
        javax.imageio.ImageIO.write(scaled, "png", target)
    }
}

kotlin {
    js {
        useEsModules()

        // Custom elements need a class. At the default ES5 target Kotlin emits
        // `function X() { HTMLElement.call(this) }`, and a DOM constructor may only be invoked with
        // `new` — the tag never upgrades. ES2015 is the first target that can express a class.
        compilerOptions {
            target = "es2015"
        }

        binaries.executable()
        browser()
    }

    // JVM carries only the Playwright end-to-end suite in `src/jvmTest`.
    jvm()

    sourceSets {
        jsMain {
            resources.srcDir(copyLumoStyles)
            resources.srcDir(scaleLogo)
        }
        jsMain.dependencies {
            // The library this site documents, as a project dependency: a demo always runs against
            // the working tree, never against a released version.
            implementation(project(":ktjs-dom-kit"))
            implementation(project(":ktjs-demo-kit"))
            implementation("org.jetbrains.kotlin-wrappers:kotlin-browser:2026.11.1")
        }

        jvmTest.dependencies {
            implementation("com.microsoft.playwright:playwright:1.61.0")
            implementation("cn.hutool:hutool-all:5.8.48")
            implementation("org.junit.jupiter:junit-jupiter:6.1.3")
            implementation(kotlin("test-junit5"))
            runtimeOnly("org.junit.platform:junit-platform-launcher:6.1.3")
        }
    }
}

// -----------------------------------------------------------------------------------------------
// End-to-end suite
//
// The suite lives in `src/jvmTest`, so the demo stays one KMP module. It uses Playwright's official
// JUnit 5 integration plus Hutool's `SimpleServer` for the static bundle, and reuses the Chromium
// that `kotlinInstallPlaywrightBrowsers` installs for the library's own browser tests.
// `jvmTest` is disabled so `build` and `check` stay browser-free; `e2eTest` runs the same test
// compilation on demand.
// -----------------------------------------------------------------------------------------------
val jvmE2eCompilation = kotlin.targets.getByName("jvm").compilations.getByName("test")

tasks.register<Test>("e2eTest") {
    group = "verification"
    description = "Runs the Kotlin/JVM Playwright suite against the bundled demo page."
    useJUnitPlatform()
    dependsOn("jsBrowserDistribution")
    dependsOn(":ktjs-dom-kit:kotlinInstallPlaywrightBrowsers")

    testClassesDirs = jvmE2eCompilation.output.classesDirs
    classpath = files(jvmE2eCompilation.output.allOutputs, jvmE2eCompilation.runtimeDependencyFiles)
    // Points at the production bundle by default; `-Ddemo.dist=…` lets a run target the development
    // bundle instead (`build/dist/js/developmentExecutable`), which is the closest thing to
    // `jsBrowserDevelopmentRun` a headless suite can check.
    systemProperty(
        "demo.dist",
        providers.systemProperty("demo.dist").getOrElse(
            layout.buildDirectory.dir("dist/js/productionExecutable").get().asFile.absolutePath,
        ),
    )
    systemProperty("e2e.output", layout.buildDirectory.dir("e2e-results").get().asFile.absolutePath)
    systemProperty("junit.jupiter.execution.timeout.default", "30 s")

    outputs.upToDateWhen { false }
}

tasks.named<Test>("jvmTest") { enabled = false }
