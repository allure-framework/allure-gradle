plugins {
    `kotlin-dsl`
}

group = "allure-gradle"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
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
//    implementation("org.gradle.kotlin:gradle-kotlin-dsl-conventions:0.7.0")
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}
