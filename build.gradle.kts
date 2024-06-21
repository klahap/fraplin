import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.gradle.plugin-publish") version "1.2.1"
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.serialization") version "1.9.20"
    `java-gradle-plugin`
    `kotlin-dsl`
}

group = "com.fraplin"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.squareup:kotlinpoet:1.16.0")
    implementation("com.squareup.okhttp3:okhttp-coroutines:5.0.0-alpha.14")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.20")
    implementation("org.jsoup:jsoup:1.15.3")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        freeCompilerArgs += "-Xcontext-receivers"
        // freeCompilerArgs += "-Xdebug"
        jvmTarget = "21"
    }
}

gradlePlugin {
    website = "https://github.com/KlausH09/fraplin"
    vcsUrl = "https://github.com/KlausH09/fraplin.git"

    val generateFrappeDsl by plugins.creating {
        id = "com.fraplin"
        implementationClass = "com.fraplin.Plugin"
        displayName = "Generate Kotlin Client for a Frappe Site"
        description = "A plugin that generates a Frappe REST client in Kotlin"
        tags = listOf("Frappe", "Client", "Kotlin", "Kotlin DSL", "generate")
    }
}


val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    languageVersion = "1.9"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    languageVersion = "1.9"
}