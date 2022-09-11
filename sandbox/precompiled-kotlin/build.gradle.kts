plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    // io.qameta.allure:io.qameta.allure.gradle.plugin is preferable,
    // however Gradle does not recognize .gradle.plugin within included build,
    // so we use io.qameta.allure.gradle.allure:allure-plugin fallback
    implementation("io.qameta.allure.gradle.allure:allure-plugin")
}
