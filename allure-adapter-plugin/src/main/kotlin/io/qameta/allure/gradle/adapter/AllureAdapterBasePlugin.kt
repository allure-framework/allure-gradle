package io.qameta.allure.gradle.adapter

import io.qameta.allure.gradle.base.AllureExtension
import io.qameta.allure.gradle.base.dsl.extensions
import io.qameta.allure.gradle.base.metadata.AllureResultType
import io.qameta.allure.gradle.adapter.AllureAdapterBasePlugin.Companion.ALLURE_RAW_RESULT_ELEMENTS_CONFIGURATION_NAME
import io.qameta.allure.gradle.adapter.tasks.CopyCategories
import io.qameta.allure.gradle.util.categoryDocumentation
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Usage
import org.gradle.kotlin.dsl.*

/**
 * The plugin adds [ALLURE_RAW_RESULT_ELEMENTS_CONFIGURATION_NAME] configuration so the project
 * can share raw Allure results for aggregation.
 */
open class AllureAdapterBasePlugin : Plugin<Project> {
    companion object {
        const val PLUGIN_NAME = "io.qameta.allure-adapter-base"
        const val ALLURE_RAW_RESULT_ELEMENTS_CONFIGURATION_NAME = "allureRawResultElements"
        const val ALLURE_COPY_CATEGORIES_ELEMENTS_CONFIGURATION_NAME = "allureCopyCategoriesElements"
        const val ALLURE_USAGE = "Allure"
    }

    override fun apply(target: Project): Unit = target.run {
        val allureExtension = the<AllureExtension>()
        allureExtension.extensions.create<AllureAdapterExtension>(
            AllureAdapterExtension.NAME,
            project
        )

        val rawResultElements = configurations.create(ALLURE_RAW_RESULT_ELEMENTS_CONFIGURATION_NAME) {
            description =
                "The configuration exposes Allure raw results (simple-result.json, executor.json) for reporting"
            isCanBeConsumed = true
            isCanBeResolved = false
            attributes {
                categoryDocumentation(objects)
                attribute(Usage.USAGE_ATTRIBUTE, objects.named(ALLURE_USAGE))
                attribute(AllureResultType.ATTRIBUTE, AllureResultType.RAW)
            }
        }

        val copyCategoriesElements = configurations.create(ALLURE_COPY_CATEGORIES_ELEMENTS_CONFIGURATION_NAME) {
            description =
                "The configuration Allows registering tasks that would copy updated categories.json files without re-running tests"
            isVisible = false
            isCanBeConsumed = true
            isCanBeResolved = false
            attributes {
                categoryDocumentation(objects)
                attribute(Usage.USAGE_ATTRIBUTE, objects.named(ALLURE_USAGE))
                attribute(AllureResultType.ATTRIBUTE, @Suppress("deprecation") AllureResultType.COPY_CATEGORIES)
            }
        }

        val allureRawResultDirs by configurations.creating {
            description = "gather all allure-results folders in the current project"
            isVisible = false
            isCanBeConsumed = false
            isCanBeResolved = true
            extendsFrom(rawResultElements)
        }

        val copyCategories by tasks.registering(CopyCategories::class) {
            description = "Copies categories.json to allure-results folders"
            destinationDirs.set(allureRawResultDirs)
        }

        copyCategoriesElements.outgoing.artifact(copyCategories.flatMap { it.markerFile }) {
            builtBy(copyCategories)
        }

        // Workaround for https://github.com/gradle/gradle/issues/6875
        target.afterEvaluate {
            configurations.findByName("archives")?.let { archives ->
                removeArtifactsFromArchives(archives, rawResultElements)
                removeArtifactsFromArchives(archives, copyCategoriesElements)
            }
        }
    }

    private fun Project.removeArtifactsFromArchives(archives: Configuration, elements: Configuration) {
        val allureResultNames = elements.outgoing.artifacts.mapTo(mutableSetOf()) { it.name }
        if (allureResultNames.isEmpty()) {
            return
        }
        logger.debug("Will remove artifacts $allureResultNames (outgoing artifacts of $elements) from $archives configuration to workaround https://github.com/gradle/gradle/issues/6875")
        archives.outgoing.artifacts.removeIf { it.name in allureResultNames }
    }
}
