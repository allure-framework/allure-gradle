plugins {
    id("allure-gradle.kotlin-dsl-published-plugin")
}

group = "io.qameta.allure.gradle.allure"

dependencies {
    api(project(":allure-adapter-plugin"))
    api(project(":allure-report-plugin"))

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
        register("allurePlugin") {
            id = "io.qameta.allure"
            displayName = "Allure Framework integration plugin"
            description = "Adds a tasks to aggregate the results from multiple projects"
            implementationClass = "io.qameta.allure.gradle.allure.AllurePlugin"
            tags = listOf("allure", "reporting", "testing")
        }
    }
}
