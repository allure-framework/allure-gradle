import org.gradle.api.tasks.testing.Test

plugins {
    id("allure-gradle.root-build")
}

group = "io.qameta.allure"

val testGradleVersionProperty = "testGradleVersion"
val defaultTestGradleVersion = "9.4.1"

allprojects {
    tasks.withType<Test>().configureEach {
        val testGradleVersion = providers.gradleProperty(testGradleVersionProperty)
            .orElse(defaultTestGradleVersion)
        inputs.property(testGradleVersionProperty, testGradleVersion)
        systemProperty(testGradleVersionProperty, testGradleVersion.get())
    }
}
