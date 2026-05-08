plugins {
    id("allure-gradle.java")
    id("allure-gradle.repositories")
    `java-library`
}

group = "io.qameta.allure.gradle.testkit"

dependencies {
    api(libs.junitJupiter)

    implementation(libs.allureJavaCommons)
    implementation(gradleTestKit())
    implementation(libs.commons.io)
    implementation(libs.commons.text)
    testImplementation(libs.allureJunit5)
    runtimeOnly(libs.junitPlatformLauncher)
}

tasks.test {
    useJUnitPlatform()
    systemProperty("junit.jupiter.extensions.autodetection.enabled", "true")
}
