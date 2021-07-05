import buildlogic.embeddedKotlinDsl
import buildlogic.plugin

plugins {
    id("allure-gradle.kotlin-dsl-gradle-plugin")
}

group = "io.qameta.allure.buildlogic"

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(embeddedKotlinDsl())
    implementation(plugin("com.github.vlsi.gradle-extensions", "1.74"))
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin")
    implementation(plugin("org.jetbrains.dokka", "1.4.32"))
}
