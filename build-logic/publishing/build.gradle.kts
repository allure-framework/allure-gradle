import buildlogic.plugin

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
    implementation("allure-gradle:gradle-plugin")
    implementation(plugin("com.gradle.plugin-publish", "1.0.0"))
    implementation(plugin("io.github.gradle-nexus.publish-plugin", "1.1.0"))
    implementation("net.researchgate.release:net.researchgate.release.gradle.plugin:2.8.1")
}
