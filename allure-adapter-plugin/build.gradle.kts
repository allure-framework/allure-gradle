plugins {
    id("allure-gradle.kotlin-dsl-published-plugin")
}

group = "io.qameta.allure.gradle.adapter"

dependencies {
    api(project(":allure-base-plugin"))
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
    val pluginTags = listOf("allure", "reporting", "testing")
    plugins {
        register("allureAdapterBase") {
            id = "io.qameta.allure-adapter-base"
            displayName = "Plugin for adpater commons between allure plugins"
            description = "Declares common configurations for producing and consuming Allure results and reports"
            implementationClass = "io.qameta.allure.gradle.adapter.AllureAdapterBasePlugin"
            tags = pluginTags
        }
        register("allureAdapter") {
            id = "io.qameta.allure-adapter"
            displayName = "Plugin for allure adapters"
            description = "Implements autoconfiguration for collecting data for Allure"
            implementationClass = "io.qameta.allure.gradle.adapter.AllureAdapterPlugin"
            tags = pluginTags
        }
    }
}
