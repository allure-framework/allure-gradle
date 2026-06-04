plugins {
    id("allure-gradle.kotlin-dsl-published-plugin")
}

group = "io.qameta.allure.gradle.adapter"

val generateAllureAdapterDefaults = tasks.register("generateAllureAdapterDefaults") {
    val outputDirectory = layout.buildDirectory.dir("generated/sources/allureAdapterDefaults/kotlin")
    val allureJavaVersion = libs.versions.allureJava
    val aspectjVersion = libs.versions.aspectj

    inputs.property("allureJavaVersion", allureJavaVersion)
    inputs.property("aspectjVersion", aspectjVersion)
    outputs.dir(outputDirectory)

    doLast {
        val outputFile = outputDirectory.get()
            .file("io/qameta/allure/gradle/adapter/AllureAdapterDefaults.kt")
            .asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(
            """
            package io.qameta.allure.gradle.adapter

            internal object AllureAdapterDefaults {
                const val DEFAULT_ALLURE_JAVA_VERSION = "${allureJavaVersion.get()}"
                const val DEFAULT_ASPECTJ_VERSION = "${aspectjVersion.get()}"
            }
            """.trimIndent() + "\n"
        )
    }
}

tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileKotlin") {
    source(generateAllureAdapterDefaults)
}

tasks.named<Jar>("sourcesJar") {
    from(generateAllureAdapterDefaults)
}

dependencies {
    api(project(":allure-base-plugin"))
    testImplementation(project(":testkit-jupiter"))
    testImplementation(libs.allureJunit5)
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
