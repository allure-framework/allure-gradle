plugins {
    `kotlin-dsl`
}

group = "allure-gradle"

tasks.withType<JavaCompile>().configureEach {
    options.release = 8
}

val kotlinDslVersion = PluginDependenciesSpec { id ->
    object : PluginDependencySpec {
        var version: String? = null
        override fun version(version: String?) = apply { this.version = version }
        override fun apply(apply: Boolean) = this
        override fun toString() = version ?: ""
    }
}.`kotlin-dsl`.toString()

dependencies {
    implementation("org.gradle.kotlin.kotlin-dsl:org.gradle.kotlin.kotlin-dsl.gradle.plugin:$kotlinDslVersion")
}
