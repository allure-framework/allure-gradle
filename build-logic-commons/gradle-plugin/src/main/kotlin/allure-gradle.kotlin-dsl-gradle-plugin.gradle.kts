plugins {
    id("java-library")
    id("org.gradle.kotlin.kotlin-dsl") // this is 'kotlin-dsl' without version
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 17
}

tasks.validatePlugins {
    failOnWarning.set(true)
    enableStricterValidation.set(true)
}
