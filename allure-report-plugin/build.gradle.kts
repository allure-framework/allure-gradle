plugins {
    id("allure-gradle.kotlin-dsl-published-plugin")
}

group = "io.qameta.allure.gradle.report"

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
        create("allureDownloadPlugin") {
            id = "io.qameta.allure-download"
            displayName = "Plugin for downloading allure commandline"
            description = "Adds a task to download the required Allure version"
            implementationClass = "io.qameta.allure.gradle.download.AllureDownloadPlugin"
        }
        create("allureReportPlugin") {
            id = "io.qameta.allure-report"
            displayName = "Plugin for building allure report"
            description = "Adds a task to build Allure report for the current project"
            implementationClass = "io.qameta.allure.gradle.report.AllureReportPlugin"
        }
        create("allureAggregateReportPlugin") {
            id = "io.qameta.allure-aggregate-report"
            displayName = "Plugin for building aggregated allure report"
            description = "Adds a task to aggregate the results from multiple projects"
            implementationClass = "io.qameta.allure.gradle.report.AllureAggregateReportPlugin"
        }
    }
}
