package io.qameta.allure.gradle.adapter.config

import io.qameta.allure.gradle.adapter.autoconfigure.AutoconfigureRuleBuilder
import io.qameta.allure.gradle.adapter.autoconfigure.BaseTrimMetaInfServices
import org.gradle.api.Action
import org.gradle.api.artifacts.DependencyMetadata
import org.gradle.api.provider.Provider
import org.gradle.util.GradleVersion

internal enum class AllureJavaAdapter(
    val adapterName: String,
    val config: Action<in AdapterConfig>
) {
    junit4("junit4", {
        activateOn("junit:junit") {
            compileAndRuntime(adapterDependency)
            runtimeOnly(adapterVersion.map { "io.qameta.allure:allure-junit4-aspect:$it" })
        }
    }),
    junit5("junit5", {
        supportsAutoconfigureListeners.set(true)
        activateOn("org.junit.jupiter:junit-jupiter-api") {
            compileAndRuntimeWithServices(adapterDependency, trimServicesFromJar)
        }
    }),
    junitPlatform("junit-platform", {
        supportsAutoconfigureListeners.set(true)
        activateOn("org.junit.platform:junit-platform-launcher") {
            compileAndRuntimeWithServices(adapterDependency, trimServicesFromJar)
        }
    }),
    testng("testng", {
        supportsAutoconfigureListeners.set(true)
        activateOn("org.testng:testng") {
            matching {
                versionAtLeast(it.version, 6, 14, 3)
            }
            compileAndRuntimeWithServices(adapterDependency, trimServicesFromJar)
        }
    }),
    jbehave("jbehave", {
        activateOn("org.jbehave:jbehave-core") {
            matching {
                it.version.startsWith("4.")
            }
            compileAndRuntime(adapterDependency)
        }
    }),
    jbehave5("jbehave5", {
        activateOn("org.jbehave:jbehave-core") {
            matching {
                it.version.startsWith("5.")
            }
            compileAndRuntime(adapterDependency)
        }
    }),
    karate("karate", {
        supportsAutoconfigureListeners.set(true)
        activateOn("com.intuit.karate:karate-core") {
            compileAndRuntimeWithServices(adapterDependency, trimServicesFromJar)
        }
    }),
    scalatest("scalatest", {
        activateOn("org.scalatest:scalatest_2.12") {
            compileAndRuntime(adapterVersion.map { "io.qameta.allure:allure-scalatest_2.12:$it" })
        }
        activateOn("org.scalatest:scalatest_2.13") {
            compileAndRuntime(adapterVersion.map { "io.qameta.allure:allure-scalatest_2.13:$it" })
        }
    }),
    // Spock 2 runs on JUnit Platform, Allure provides allure-spock2 for it
    spock("spock2", {
        activateOn("org.spockframework:spock-core") {
            compileAndRuntime(adapterDependency)
        }
    }),
    cucumber4Jvm("cucumber4-jvm", cucumberJvm(4)),
    cucumber5Jvm("cucumber5-jvm", cucumberJvm(5)),
    cucumber6Jvm("cucumber6-jvm", cucumberJvm(6)),
    cucumber7Jvm("cucumber7-jvm", cucumberJvm(7)),
    ;

    companion object {
        private val adapters = values().associateBy { it.adapterName }
        private val values = values().associateBy { it.name }

        fun find(name: String) = values[name] ?: adapters[name]?.let {
            // We don't want to have both cucumberJvm and cucumber-jvm used at the same time
            // so we allow only cucumberJvm
            throw IllegalStateException("Please use ${it.name} name for adapter instead of $name")
        }

        private fun AutoconfigureRuleBuilder.compileAndRuntimeWithServices(
            dep: Provider<String>,
            trimServices: Provider<Boolean>
        ) {
            // Gradle does not provide a way to add a dependency with capability via metadata rule
            // So we use "substitute with classifier" (6.6+) or artifact transformation (5.3+) workarounds
            // https://github.com/gradle/gradle/issues/17035
            compileAndRuntime(dep) {
                val gradleVersion = GradleVersion.current()
                if (gradleVersion < GradleVersion.version("6.6") && trimServices.get()) {
                    if (gradleVersion < GradleVersion.version("6.0")) {
                        throw IllegalStateException(
                            "Autoconfiguration for $name with autoconfigureListeners=false" +
                                    " requires Gradle 6.0+. Please upgrade Gradle to 6.0+ or add ${dep.get()}:spi-off " +
                                    "to the relevant configurations (e.g. testImplementation) manually and " +
                                    "turn off autoconfiguration with $name { enabled.set(false) }"
                        )
                    }
                    jarWithoutMetainfServices()
                }
            }
        }

        private fun DependencyMetadata<*>.jarWithoutMetainfServices() {
            attributes {
                attribute(
                    BaseTrimMetaInfServices.ARTIFACT_TYPE_ATTRIBUTE,
                    BaseTrimMetaInfServices.NO_SPI_JAR
                )
            }
        }

        private fun versionAtLeast(version: String, vararg minimum: Int): Boolean {
            val actual = version.substringBefore('-')
                .split('.')
                .map { it.toIntOrNull() ?: 0 }

            for (index in 0 until maxOf(actual.size, minimum.size)) {
                val actualPart = actual.getOrElse(index) { 0 }
                val minimumPart = minimum.getOrElse(index) { 0 }
                if (actualPart != minimumPart) {
                    return actualPart > minimumPart
                }
            }
            return true
        }
    }
}


private fun cucumberJvm(majorVersion: Int): Action<in AdapterConfig> =
    Action {
        if (majorVersion == 1) {
            activateOn("info.cukes:cucumber-junit")
        } else {
            activateOn("io.cucumber:cucumber-core") {
                matching {
                    it.version.startsWith("$majorVersion.")
                }
                compileAndRuntime(adapterDependency)
            }
        }
    }
