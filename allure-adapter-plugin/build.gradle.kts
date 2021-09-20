plugins {
    id("allure-gradle.kotlin-dsl-published-plugin")
}

group = "io.qameta.allure.gradle.adapter"

dependencies {
    implementation(project(":allure-base-plugin"))
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
        create("allureAdapterBasePlugin") {
            id = "io.qameta.allure-adapter-base"
            displayName = "Plugin for adpater commons between allure plugins"
            description = "Declares common configurations for producing and consuming Allure results and reports"
            implementationClass = "io.qameta.allure.gradle.adapter.AllureAdapterBasePlugin"
        }
        create("allureAdapterPlugin") {
            id = "io.qameta.allure-adapter"
            displayName = "Plugin for allure adapters"
            description = "Implements autoconfiguration for collecting data for Allure"
            implementationClass = "io.qameta.allure.gradle.adapter.AllureAdapterPlugin"
        }
    }
}
