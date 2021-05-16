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
    implementation("com.gradle.plugin-publish:com.gradle.plugin-publish.gradle.plugin:0.14.0")
    implementation("io.github.gradle-nexus.publish-plugin:io.github.gradle-nexus.publish-plugin.gradle.plugin:1.0.0")
    implementation("net.researchgate.release:net.researchgate.release.gradle.plugin:2.8.1")
}
