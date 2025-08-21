plugins {
    id("java-library")
    id("org.gradle.kotlin.kotlin-dsl") // this is 'kotlin-dsl' without version
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 8
}

tasks.validatePlugins {
    failOnWarning.set(true)
    enableStricterValidation.set(true)
}
