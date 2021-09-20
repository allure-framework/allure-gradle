package io.qameta.allure.gradle.adapter

import adapter
import io.qameta.allure.gradle.base.AllureBasePlugin
import io.qameta.allure.gradle.base.AllureExtension
import io.qameta.allure.gradle.adapter.autoconfigure.BaseTrimMetaInfServices
import io.qameta.allure.gradle.adapter.autoconfigure.TrimMetaInfServices53
import io.qameta.allure.gradle.adapter.autoconfigure.TrimMetaInfServices54
import io.qameta.allure.gradle.adapter.config.AdapterHandler
import io.qameta.allure.gradle.adapter.config.AllureJavaAdapter
import io.qameta.allure.gradle.util.categoryLibrary
import io.qameta.allure.gradle.util.libraryElementsJar
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Usage
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.*
import org.gradle.util.GradleVersion

/**
 * The plugin instruments [Test] and [JavaExec] tasks so they collect data for Allure.
 * The data is collected into [AllureAdapterBasePlugin.ALLURE_RAW_RESULT_ELEMENTS_CONFIGURATION_NAME] configuration.
 */
open class AllureAdapterPlugin : Plugin<Project> {
    companion object {
        const val PLUGIN_NAME = "io.qameta.allure-adapter"
        const val ASPECTJ_WEAVER_CONFIGURATION = "allureAspectjWeaverAgent"
        const val ALLURE_DIR_PROPERTY = "allure.results.directory"
    }

    override fun apply(target: Project): Unit = target.run {
        apply<AllureBasePlugin>()
        apply<AllureAdapterBasePlugin>()

        val allureExtension = the<AllureExtension>()
        val adapterExtension = allureExtension.adapter

        // Configuration for AspectJ agent
        configurations.create(ASPECTJ_WEAVER_CONFIGURATION) {
            description = "Classpath for AspectJ to be used for Allure waver"
            isCanBeResolved = true
            isCanBeConsumed = false
            defaultDependencies {
                add(project.dependencies.create("org.aspectj:aspectjweaver:${adapterExtension.aspectjVersion.get()}"))
            }
            attributes {
                categoryLibrary(objects)
                libraryElementsJar(objects)
                attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
            }
        }

        autoconfigureDependencyRules(adapterExtension)
        configureTestTasks(adapterExtension)
        val artifactType = Attribute.of("artifactType", String::class.java)

        afterEvaluate {
            // We don't know if the user updates autoconfigure value, so we delay the decision till afterEvaluate
            adapterExtension.takeIf { it.adapters.configuredAdapters.isEmpty() }?.run {
                for (adapter in AllureJavaAdapter.values()) {
                    adapters.create(adapter.name)
                }
            }

            if (GradleVersion.current() >= GradleVersion.version("6.6")) {
                configureSpiOffSubstitution(adapterExtension.adapters)
            } else if (GradleVersion.current() >= GradleVersion.version("5.3")) {
                // Older Gradle do not have "substitute with classifier" feature
                // so we use ArtifactTransformation to trim META-INF/services from the jar
                dependencies {
                    val transformClass = if (GradleVersion.current() >= GradleVersion.version("5.4")) {
                        TrimMetaInfServices54::class
                    } else {
                        TrimMetaInfServices53::class
                    }
                    registerTransform(transformClass) {
                        from.attribute(artifactType, "jar")
                        to.attribute(artifactType, BaseTrimMetaInfServices.NO_SPI_JAR)
                    }
                }
            }
        }
    }

    private fun Project.configureSpiOffSubstitution(adapters: AdapterHandler) {
        // Substitute adapter with spi-off classifier when trimServicesFromJar == true
        adapters.matching { it.trimServicesFromJar.get() }
            .all {
                val adapterConfig = this
                configurations.all {
                    resolutionStrategy {
                        dependencySubstitution {
                            eachDependency {
                                if (requested.group == "io.qameta.allure"
                                    && requested.name == adapterConfig.module
                                ) {
                                    artifactSelection {
                                        selectArtifact("jar", null, "spi-off")
                                    }
                                }
                            }
                        }
                    }
                }
            }
    }

    private fun Project.autoconfigureDependencyRules(extension: AllureAdapterExtension) {
        // We need to initialize all the adapters, so we don't need lazy evaluation configureEach
        extension.adapters.all {
            activateOn.all {
                val rule = this
                dependencies {
                    components {
                        rule.configure(this)
                    }
                }
            }
        }
    }

    private fun Project.configureTestTasks(extension: AllureAdapterExtension) {
        extension.run {
            tasks.withType<Test>().let {
                gatherResultsFrom(it)
                addAspectjTo(it)
            }
            tasks.withType<JavaExec>()
                .matching { task -> task.name == "junitPlatformTest" }
                .let {
                    gatherResultsFrom(it)
                    addAspectjTo(it)
                }
        }
    }
}
