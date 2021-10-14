plugins {
    `kotlin-dsl`
}

group = "allure-gradle"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

val kotlinDslVersion = object : PluginDependenciesSpec {
    override fun id(id: String): PluginDependencySpec {
        return object : PluginDependencySpec {
            var version: String? = null
            override fun version(version: String?) = apply { this.version = version }
            override fun apply(apply: Boolean) = this
            override fun toString() = version ?: ""
        }
    }

    override fun alias(notation: Provider<PluginDependency>): PluginDependencySpec {
        return object : PluginDependencySpec {
            var version: String? = null
            override fun version(version: String?) = apply { this.version = version }
            override fun apply(apply: Boolean) = this
            override fun toString() = version ?: ""
        }
    }
}.`kotlin-dsl`.toString()

dependencies {
    implementation("org.gradle.kotlin.kotlin-dsl:org.gradle.kotlin.kotlin-dsl.gradle.plugin:$kotlinDslVersion")
//    implementation("org.gradle.kotlin:gradle-kotlin-dsl-conventions:0.7.0")
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}
