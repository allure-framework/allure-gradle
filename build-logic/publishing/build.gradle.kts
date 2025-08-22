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
    implementation(libs.gradlePublishPlugin)
    implementation(libs.nexusPublishPlugin)
    implementation(libs.researchgateReleasePlugin)
}
