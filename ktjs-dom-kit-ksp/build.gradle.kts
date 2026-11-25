import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
}

@Suppress("UNCHECKED_CAST")
val versionSuffix = rootProject.extra["versionSuffix"] as String

version = "0.1.0$versionSuffix"
description =
    "KSP processor that derives the observed attribute names of a custom element from its `attr` declarations."

kotlin {
    compilerOptions {
        // KSP loads the processor inside the Kotlin compiler's JVM; 11 is the lowest target that
        // both the CI JDK and the local one load without preview features.
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

java {
    // Gradle validates that `compileJava` and `compileKotlin` agree; this module has no Java sources.
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation(libs.ksp.symbol.processing.api)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
