import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("allure-gradle.kotlin-dsl-gradle-plugin")
}

group = "io.qameta.allure.buildlogic"

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(project(":basics"))
    implementation(project(":jvm"))
    implementation(libs.nexusPublishPlugin)
    implementation(libs.versionCatalogUpdatePlugin)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}
