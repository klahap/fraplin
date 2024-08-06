import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    id("com.gradle.plugin-publish") version "1.2.1"
    kotlin("jvm") version "2.0.10"
    kotlin("plugin.serialization") version "2.0.10"
}

val groupStr = "io.github.klahap.fraplin"
val gitRepo = "https://github.com/klahap/fraplin"

version = System.getenv("GIT_TAG_VERSION") ?: "1.0.0-SNAPSHOT"
group = groupStr

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.squareup:kotlinpoet:1.16.0")
    implementation("com.squareup.okhttp3:okhttp-coroutines:5.0.0-alpha.14")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.0.10")
    implementation("org.jsoup:jsoup:1.15.3")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

gradlePlugin {
    website = gitRepo
    vcsUrl = "$gitRepo.git"

    val generateFrappeDsl by plugins.creating {
        id = groupStr
        implementationClass = "$groupStr.Plugin"
        displayName = "Generate Kotlin Client for a Frappe Site"
        description = "A plugin that generates a Frappe REST client in Kotlin"
        tags = listOf("Frappe", "Client", "Kotlin", "Kotlin DSL", "generate")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        freeCompilerArgs.add("-Xcontext-receivers")
        jvmTarget.set(JvmTarget.JVM_21)
        languageVersion = KotlinVersion.KOTLIN_2_0
    }
}