plugins {
    id("allure-gradle.kotlin-dsl-published-plugin")
}

group = "io.qameta.allure.gradle.report"

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
        register("allureDownloadPlugin") {
            id = "io.qameta.allure-download"
            displayName = "Plugin for downloading allure commandline"
            description = "Adds a task to download the required Allure version"
            implementationClass = "io.qameta.allure.gradle.download.AllureDownloadPlugin"
            tags = pluginTags
        }
        register("allureReportPlugin") {
            id = "io.qameta.allure-report"
            displayName = "Plugin for building allure report"
            description = "Adds a task to build Allure report for the current project"
            implementationClass = "io.qameta.allure.gradle.report.AllureReportPlugin"
            tags = pluginTags
        }
        register("allureAggregateReportPlugin") {
            id = "io.qameta.allure-aggregate-report"
            displayName = "Plugin for building aggregated allure report"
            description = "Adds a task to aggregate the results from multiple projects"
            implementationClass = "io.qameta.allure.gradle.report.AllureAggregateReportPlugin"
            tags = pluginTags
        }
    }
}
