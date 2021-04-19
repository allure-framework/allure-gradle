plugins {
    id("allure-gradle.build-logic.kotlin-dsl-gradle-plugin")
}

group = "io.qameta.allure.buildlogic"

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("net.researchgate.release:net.researchgate.release.gradle.plugin:2.8.1")
    implementation("io.github.gradle-nexus.publish-plugin:io.github.gradle-nexus.publish-plugin.gradle.plugin:1.1.0")
}
