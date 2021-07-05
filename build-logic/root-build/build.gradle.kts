import buildlogic.plugin

plugins {
    id("allure-gradle.kotlin-dsl-gradle-plugin")
}

group = "io.qameta.allure.buildlogic"

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(plugin("io.github.gradle-nexus.publish-plugin", "1.1.0"))
}
