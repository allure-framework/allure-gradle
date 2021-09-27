package buildlogic

import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.`kotlin-dsl`
import org.gradle.plugin.use.PluginDependenciesSpec
import org.gradle.plugin.use.PluginDependency
import org.gradle.plugin.use.PluginDependencySpec

val DependencyHandlerScope.kotlinDslVersion: String
    get() = object : PluginDependenciesSpec {
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

fun DependencyHandlerScope.plugin(id: String, version: String) =
    "$id:$id.gradle.plugin:$version"

fun DependencyHandlerScope.embeddedKotlinDsl() =
    plugin("org.gradle.kotlin.kotlin-dsl", kotlinDslVersion)
