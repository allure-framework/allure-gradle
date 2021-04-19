plugins {
    id("allure-gradle.build-logic.java")
    `java-library`
}

group = "io.qameta.allure.gradle.testkit"

repositories {
    mavenCentral()
}

dependencies {
    api("junit:junit:_")

    implementation(gradleTestKit())
    implementation("commons-io:commons-io:_")
    implementation("org.apache.commons:commons-text:_")
}

