package io.qameta.allure.gradle.download

import commandline
import io.qameta.allure.gradle.base.AllureBasePlugin
import io.qameta.allure.gradle.base.AllureExtension
import io.qameta.allure.gradle.download.tasks.DownloadAllure
import io.qameta.allure.gradle.report.AllureReportBasePlugin
import io.qameta.allure.gradle.util.gradleGe51
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.plugins.ExtensionAware
import org.gradle.internal.Factory
import org.gradle.kotlin.dsl.*
import org.gradle.util.GradleVersion

/**
 * The plugin Adds [DownloadAllure] task.
 * Note: the end-user needs to specify repositories manually (e.g. `repositories { mavenCentral() }`)
 */
open class AllureDownloadPlugin : Plugin<Project> {
    companion object {
        const val PLUGIN_NAME = "io.qameta.allure-download"
        const val ALLURE_DOWNLOAD_TASK_NAME = "downloadAllure"
        const val ALLURE_COMMANDLINE_CONFIGURATION = "allureCommandline"
    }

    override fun apply(target: Project): Unit = target.run {
        apply<AllureBasePlugin>()

        // allure.report extension
        apply<AllureReportBasePlugin>()

        val allureExtension = the<AllureExtension>()

        val reportExtension = (allureExtension as ExtensionAware).extensions.create<AllureCommandlineExtension>(
            AllureCommandlineExtension.NAME,
            allureExtension,
            project,
            objects
        )

        val allureCommandLine = configurations.create(ALLURE_COMMANDLINE_CONFIGURATION) {
            isCanBeResolved = true
            isCanBeConsumed = false
            defaultDependencies {
                val fileExtension = reportExtension.extension.map { "@$it" }.orNull ?: ""
                val group = reportExtension.group.get()
                val module = reportExtension.module.get()
                val version = allureExtension.version.get()
                add(project.dependencies.create("$group:$module:$version$fileExtension"))

                allureExtension.commandline.downloadUrlPattern.orNull?.let { link ->
                    val formattedLink = formatLink(link, group, module, version)
                    declareCustomAllureCommandlineRepository(group, module, formattedLink)
                }
            }
        }

        tasks.register<DownloadAllure>(ALLURE_DOWNLOAD_TASK_NAME) {
            this.allureCommandLine.set(allureCommandLine)
        }
    }

    /**
     * `IvyArtifactRepository` supports patterns, however, we do not use it since
     * artifact declaration requries separate `repository url` + `layoutPattern`.
     * Parsing URLs is non-trivial, so we replace patterns and build a single URL.
     * That works as long as we need a single binary from the said repository
     * which is good enough.
     */
    private fun formatLink(link: String, group: String, module: String, version: String) =
        link.replace(Regex("\\[([^]]+)]")) {
            when (it.groups[1]!!.value) {
                "organization", "group" -> group
                "module" -> module
                "version" -> version
                else -> throw IllegalArgumentException(
                    "Unexpected pattern ${it.value} detected in allure.commandline.downloadUrlPattern $link." +
                            " The following patterns are supported: [organization] = $group, [group] = $group, [module] = $module, [version] = $version"
                )
            }
        }

    /**
     * Adds a repository so Gradle can retrieve `group:module` artifact with its regular dependency resolution.
     */
    private fun Project.declareCustomAllureCommandlineRepository(group: String, module: String, link: String) {
        repositories {
            exclusiveRepo(group, module) {
                ivy {
                    url = uri(link)
                    patternLayout {
                        // Link should already be formatted, so no pattern needed here
                        artifact("")
                    }
                    metadataSources { // skip downloading ivy.xml
                        artifact()
                    }
                }
            }
        }
    }

    fun RepositoryHandler.exclusiveRepo(group: String, module: String, repository: Factory<out ArtifactRepository>) {
        val gradleGe62 = GradleVersion.current() >= GradleVersion.version("6.2")
        if (gradleGe62) {
            // exclusiveContent is Gradle 6.2+ feature
            exclusiveContent {
                filter {
                    includeModule(group, module)
                }
                forRepository(repository)
            }
            return
        }
        repository.create()!!.apply {
            if (gradleGe51) {
                // content filtering is Gradle 6.2+ feature
                content {
                    includeModule(group, module)
                }
            }
        }
    }
}
