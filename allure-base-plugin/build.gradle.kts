plugins {
    id("allure-gradle.kotlin-dsl-published-plugin")
}

group = "io.qameta.allure.gradle.base"

dependencies {
    testImplementation(project(":testkit-junit4"))
    testImplementation(libs.assertjCore)
}

tasks.test {
    // Treat test task out-of-date if src/it changes
    inputs.dir(layout.projectDirectory.dir("src/it")).optional()
}

gradlePlugin {
    website = "https://github.com/allure-framework/allure-gradle"
    vcsUrl = "https://github.com/allure-framework/allure-gradle.git"
    plugins {
        register("allureBasePlugin") {
            id = "io.qameta.allure-base"
            displayName = "Plugin for commons between allure plugins"
            description = "Adds a common allure extension to the project"
            implementationClass = "io.qameta.allure.gradle.base.AllureBasePlugin"
            tags = listOf("allure", "reporting", "testing")
        }
    }
}
