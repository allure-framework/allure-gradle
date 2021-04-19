package io.qameta.allure.gradle.gather.config

import io.qameta.allure.gradle.gather.autoconfigure.AutoconfigureRuleBuilder
import io.qameta.allure.gradle.gather.autoconfigure.BaseTrimMetaInfServices
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
    testng("testng", {
        supportsAutoconfigureListeners.set(true)
        activateOn("org.testng:testng") {
            compileAndRuntimeWithServices(adapterDependency, trimServicesFromJar)
        }
    }),
    spock("spock", {
        activateOn("org.spockframework:spock-core")
    }),
    cucumberJvm("cucumber-jvm", cucumberJvm(1)),
    cucumber2Jvm("cucumber2-jvm", cucumberJvm(2)),
    cucumber3Jvm("cucumber3-jvm", cucumberJvm(3)),
    cucumber4Jvm("cucumber4-jvm", cucumberJvm(4)),
    cucumber5Jvm("cucumber5-jvm", cucumberJvm(5)),
    cucumber6Jvm("cucumber6-jvm", cucumberJvm(6)),
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
                    if (gradleVersion < GradleVersion.version("5.3")) {
                        throw IllegalStateException(
                            "Autoconfiguration for $name with autoconfigureListeners=false" +
                                    " requires Gradle 5.3+. Please upgrade Gradle to 5.3+ or add ${dep.get()}:spi-off " +
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
