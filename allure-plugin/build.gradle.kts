plugins {
    id("allure-gradle.kotlin-dsl-published-plugin")
}

group = "io.qameta.allure.gradle.allure"

dependencies {
    implementation(project(":allure-adapter-plugin"))
    implementation(project(":allure-report-plugin"))

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
        create("allurePlugin") {
            id = "io.qameta.allure"
            implementationClass = "io.qameta.allure.gradle.allure.AllurePlugin"
        }
    }
}
