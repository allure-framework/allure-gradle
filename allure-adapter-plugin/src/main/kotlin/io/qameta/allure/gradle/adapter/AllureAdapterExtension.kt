package io.qameta.allure.gradle.adapter

import groovy.json.JsonOutput
import io.qameta.allure.gradle.adapter.config.*
import io.qameta.allure.gradle.base.AllureExtension
import io.qameta.allure.gradle.base.tasks.ConditionalArgumentProvider
import io.qameta.allure.gradle.base.tasks.JavaAgentArgumentProvider
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.RegularFile
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskCollection
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*
import org.gradle.process.JavaForkOptions
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import javax.inject.Inject

/**
 * Configures Allure raw result adapters (e.g. [junit5], [testng], ...) as `allure { adapter {...` extension.
 */
open class AllureAdapterExtension @Inject constructor(
    private val project: Project,
    private val objects: ObjectFactory
) {
    companion object {
        const val NAME = "adapter"
        const val EXECUTOR_FILE_NAME = "executor.json"
    }

    /**
     * `allure-java` version (adapters for test engines)
     */
    val allureJavaVersion: Property<String> =
        objects.property<String>().convention("2.28.0")
    val aspectjVersion: Property<String> = objects.property<String>().convention("1.9.22.1")


    /**
     * Automatically add the relevant test engine adapters
     */
    val autoconfigure: Property<Boolean> = objects.property<Boolean>().convention(true)

    /**
     * Configure default listeners by default (e.g. JUnit5, TestNG).
     * This should be disabled if the project uses custom listeners
     */
    val autoconfigureListeners: Property<Boolean> = objects.property<Boolean>().convention(autoconfigure)

    /**
     * Automatically add AspectJ waver
     */
    val aspectjWeaver = objects.property<Boolean>().convention(autoconfigure)

    /**
     * Path to `categories.json` file for Allure.
     * The default path is `test/resources/**/categories.json`.
     */
    val categoriesFile: Property<RegularFile> = objects.fileProperty().convention(defaultCategoriesFile(project))

    private val allureResultsDir = project.layout.buildDirectory.dir("allure-results")

    val frameworks = AdapterHandler(project.container {
        objects.newInstance<AdapterConfig>(it, objects, this).also { adapter ->
            AllureJavaAdapter.find(it)?.apply {
                config.execute(adapter)
            }
        }
    })

    fun frameworks(action: Action<in AdapterHandler>) {
        // Custom type is for better Kotlin DSL
        action.execute(frameworks)
    }

    fun addAspectjTo(tasks: TaskCollection<out Task>) = tasks.configureEach {
        addAspectjTo(this)
    }

    fun addAspectjTo(task: TaskProvider<out Task>) = task.configure {
        addAspectjTo(this)
    }

    fun addAspectjTo(task: Task): Unit = task.run {
        if (aspectjWeaver.get() && this is JavaForkOptions) {
            val aspectJAgent = project.configurations[AllureAdapterPlugin.ASPECTJ_WEAVER_CONFIGURATION]
            jvmArgumentProviders.add(JavaAgentArgumentProvider(aspectJAgent))
        }
    }

    fun gatherResultsFrom(tasks: TaskCollection<out Task>) {
        project.apply<AllureAdapterBasePlugin>()
        project.afterEvaluate {
            tasks.names.forEach {
                exposeArtifact(tasks.named(it))
            }
        }
        tasks.configureEach {
            internalGatherResultsFrom(this)
        }
    }

    fun gatherResultsFrom(task: TaskProvider<out Task>) {
        project.apply<AllureAdapterBasePlugin>()
        exposeArtifact(task)
        task.configure {
            internalGatherResultsFrom(this)
        }
    }

    fun gatherResultsFrom(task: Task) {
        project.apply<AllureAdapterBasePlugin>()
        exposeArtifact(project.tasks.named(task.name))
        internalGatherResultsFrom(task)
    }

    // TODO: move to [AllureAdapterBasePlugin] like `allure { gatherResults { fromTask(..) } }
    private fun internalGatherResultsFrom(task: Task) {
        task.run {
            // Each task should store results in its own folder
            // End user should not depend on the folder name, so we do not expose it
            val rawResults = allureResultsDir.get().asFile
            // See https://github.com/allure-framework/allure2/issues/1236
            // We exclude categories.json since report task would copy categories right to the folder
            // of the current task
            outputs.files(project.fileTree(rawResults).matching { exclude("categories.json") })

            // Pass the path to the task
            if (this is JavaForkOptions) {
                systemProperty(AllureAdapterPlugin.ALLURE_DIR_PROPERTY, rawResults.absolutePath)
                // We don't know if the task will execute JUnit5 engine or not,
                // so we add extensions.autodetection.enabled to all the tasks if
                // junit5.autoconfigureListeners is enabled
                jvmArgumentProviders += ConditionalArgumentProvider(
                    project.provider {
                        frameworks.configuredAdapters[AllureJavaAdapter.junit5]?.let {
                            listOf(
                                if (it.autoconfigureListeners.get()) {
                                    "-Djunit.jupiter.extensions.autodetection.enabled=true"
                                } else {
                                    "-Dskipped.junit.jupiter.extensions.autodetection.enabled=true"
                                }
                            )
                        } ?: emptyList()
                    }
                )
            }

            doFirst {
                rawResults.mkdirs()
                // TODO: remove dependence on project at the execution time for compatibility with configuration cache
                generateExecutorInfo(rawResults, project, task.name)
            }
        }
    }

    private fun exposeArtifact(task: TaskProvider<*>) {
        // Expose the gathered raw results
        val allureResults =
            project.configurations[AllureAdapterBasePlugin.ALLURE_RAW_RESULT_ELEMENTS_CONFIGURATION_NAME]
        allureResults.outgoing.artifact(allureResultsDir) {
            builtBy(task)
        }
    }

    private fun generateExecutorInfo(resultsDir: File, project: Project, taskName: String) {
        val executorInfo = mapOf(
            "name" to "Gradle",
            "type" to "gradle",
            "taskName" to taskName,
            "buildName" to project.name,
            "projectPath" to project.path,
            // toString is to avoid unexpected interaction of custom objects and Json serializer
            "projectVersion" to project.version.toString()
        )
        val resultsPath = Paths.get(resultsDir.absoluteFile.path)
        Files.createDirectories(resultsPath)
        val executorPath = resultsPath.resolve(EXECUTOR_FILE_NAME)
        Files.write(executorPath, JsonOutput.toJson(executorInfo).toByteArray(StandardCharsets.UTF_8))
    }

    private fun defaultCategoriesFile(project: Project): Provider<RegularFile> {
        val categoriesInResources = categoriesInResources(project)

        val sourceSets = project.findProperty("sourceSets") as? SourceSetContainer

        val categoriesProvider = if (sourceSets == null) {
            // SourceSets are missing, so will try regular test/resources path
            categoriesInResources
        } else {
            // SourceSets detected, will try test sourceset first
            project.provider<File> {
                val test = sourceSets.findByName(SourceSet.TEST_SOURCE_SET_NAME) ?: return@provider null
                val file = test.resources.matching { include("**/categories.json") }.firstOrNull()
                file ?: categoriesInResources.orNull
            }
        }
        return project.layout.file(categoriesProvider)
    }

    private fun categoriesInResources(project: Project): Provider<File> {
        val tree = project.fileTree(project.layout.projectDirectory.dir("test/resources")) {
            matching { include("**/categories.json") }
        }
        return project.provider {
            tree.firstOrNull()
        }
    }
}
