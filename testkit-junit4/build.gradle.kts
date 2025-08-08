plugins {
    id("allure-gradle.java")
    id("allure-gradle.repositories")
    `java-library`
}

group = "io.qameta.allure.gradle.testkit"

dependencies {
    api(libs.junit4)

    implementation(gradleTestKit())
    implementation(libs.commons.io)
    implementation(libs.commons.text)
}

