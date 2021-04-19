plugins {
    id("allure-gradle.build-logic.kotlin-dsl-published-plugin")
}

group = "io.qameta.allure.gradle.allure"

dependencies {
    implementation(project(":allure-gather-plugin"))
    implementation(project(":allure-report-plugin"))

    testImplementation(project(":testkit-junit4"))
    testImplementation("org.assertj:assertj-core:_")
}

tasks.test {
    // Treat test task out-of-date if src/it changes
    inputs.dir(layout.projectDirectory.dir("src/it")).optional()
}

gradlePlugin {
    plugins {
        create("allurePlugin") {
            id = "io.qameta.allure"
            implementationClass = "io.qameta.allure.gradle.allure.AllurePlugin"
        }
    }
}
