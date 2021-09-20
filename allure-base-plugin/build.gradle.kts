plugins {
    id("allure-gradle.kotlin-dsl-published-plugin")
}

group = "io.qameta.allure.gradle.base"

dependencies {
    testImplementation(project(":testkit-junit4"))
    testImplementation("org.assertj:assertj-core:_")
}

tasks.test {
    // Treat test task out-of-date if src/it changes
    inputs.dir(layout.projectDirectory.dir("src/it")).optional()
}

pluginBundle {
    website = "https://github.com/allure-framework/allure-gradle"
    vcsUrl = "https://github.com/allure-framework/allure-gradle.git"
    tags = listOf("allure", "reporting", "testing")
}

gradlePlugin {
    plugins {
        create("allureBasePlugin") {
            id = "io.qameta.allure-base"
            description = "Adds a common allure extension to the project"
            implementationClass = "io.qameta.allure.gradle.base.AllureBasePlugin"
        }
    }
}
