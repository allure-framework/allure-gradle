plugins {
    id("allure-gradle.build-logic.kotlin-dsl-published-plugin")
}

group = "io.qameta.allure.gradle.gather"

dependencies {
    implementation(project(":allure-base-plugin"))
    testImplementation(project(":testkit-junit4"))
    testImplementation("org.assertj:assertj-core:_")
}

tasks.test {
    // Treat test task out-of-date if src/it changes
    inputs.dir(layout.projectDirectory.dir("src/it")).optional()
}

gradlePlugin {
    plugins {
        create("allureCollectBasePlugin") {
            id = "io.qameta.allure-gather-base"
            description = "Declares common configurations for producing and consuming Allure results and reports"
            implementationClass = "io.qameta.allure.gradle.gather.AllureGatherBasePlugin"
        }
        create("allureCollectPlugin") {
            id = "io.qameta.allure-gather"
            description = "Implements autoconfiguration for gathering data for Allure"
            implementationClass = "io.qameta.allure.gradle.gather.AllureGatherPlugin"
        }
    }
}
