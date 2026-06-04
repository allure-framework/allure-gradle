plugins {
    id("allure-gradle.kotlin-dsl-published-plugin")
}

group = "io.qameta.allure.gradle.base"

val generateAllureDefaults = tasks.register("generateAllureDefaults") {
    val outputDirectory = layout.buildDirectory.dir("generated/sources/allureDefaults/kotlin")
    val allure2Version = libs.versions.allure2
    val allure3Version = libs.versions.allure3

    inputs.property("allure2Version", allure2Version)
    inputs.property("allure3Version", allure3Version)
    outputs.dir(outputDirectory)

    doLast {
        val outputFile = outputDirectory.get()
            .file("io/qameta/allure/gradle/base/AllureDefaults.kt")
            .asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(
            """
            package io.qameta.allure.gradle.base

            internal object AllureDefaults {
                const val DEFAULT_ALLURE3 = "${allure3Version.get()}"
                const val DEFAULT_ALLURE2 = "${allure2Version.get()}"
            }
            """.trimIndent() + "\n"
        )
    }
}

tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileKotlin") {
    source(generateAllureDefaults)
}

tasks.named<Jar>("sourcesJar") {
    from(generateAllureDefaults)
}

dependencies {
    testImplementation(project(":testkit-jupiter"))
    testImplementation(libs.assertjCore)
}

tasks.test {
    useJUnitPlatform()
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
