package io.qameta.allure.gradle.download
import io.qameta.allure.gradle.base.AllureBasePlugin
import io.qameta.allure.gradle.base.AllureExtension
import io.qameta.allure.gradle.base.AllureRuntimeFamily
import io.qameta.allure.gradle.download.tasks.DownloadAllure
import io.qameta.allure.gradle.download.tasks.DownloadNode
import io.qameta.allure.gradle.download.tasks.InstallAllure3
import io.qameta.allure.gradle.report.AllureReportBasePlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.IvyArtifactRepository
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.*

/**
 * The plugin Adds [DownloadAllure] task.
 * Note: the end-user needs to specify repositories manually (e.g. `repositories { mavenCentral() }`)
 */
open class AllureDownloadPlugin : Plugin<Project> {
    companion object {
        const val PLUGIN_NAME = "io.qameta.allure-download"
        const val ALLURE_DOWNLOAD_TASK_NAME = "downloadAllure"
        const val ALLURE_COMMANDLINE_CONFIGURATION = "allureCommandline"
        const val ALLURE_NODE_CONFIGURATION = "allureNodeDistribution"
        const val ALLURE_3_PACKAGE_CONFIGURATION = "allure3Package"
        const val DOWNLOAD_NODE_TASK_NAME = "downloadNode"
        const val INSTALL_ALLURE_3_TASK_NAME = "installAllure3"
    }

    override fun apply(target: Project): Unit = target.run {
        apply<AllureBasePlugin>()

        // allure.report extension
        apply<AllureReportBasePlugin>()

        val allureExtension = the<AllureExtension>()

        val reportExtension = (allureExtension as ExtensionAware).extensions.create<AllureCommandlineExtension>(
            AllureCommandlineExtension.NAME,
            allureExtension,
            project
        )

        val allureCommandLine = configurations.register(ALLURE_COMMANDLINE_CONFIGURATION) {
            isCanBeResolved = true
            isCanBeConsumed = false
            defaultDependencies {
                if (allureRuntimeFamily(allureExtension.version.get()) != AllureRuntimeFamily.ALLURE_2) {
                    return@defaultDependencies
                }
                val fileExtension = reportExtension.extension.map { "@$it" }.orNull ?: ""
                val group = reportExtension.group.get()
                val module = reportExtension.module.get()
                val version = allureExtension.version.get()
                add(project.dependencies.create("$group:$module:$version$fileExtension"))

                reportExtension.downloadUrlPattern.orNull?.let { link ->
                    val formattedLink = formatLink(link, group, module, version)
                    declareCustomAllureCommandlineRepository(group, module, formattedLink)
                }
            }
        }

        val nodeDistribution = configurations.register(ALLURE_NODE_CONFIGURATION) {
            isCanBeResolved = true
            isCanBeConsumed = false
            defaultDependencies {
                if (allureRuntimeFamily(allureExtension.version.get()) != AllureRuntimeFamily.ALLURE_3) {
                    return@defaultDependencies
                }
                val nodeDistribution = detectNodeDistribution()
                val module = "node-${nodeDistribution.classifier}"
                val nodeVersion = DEFAULT_NODE_VERSION
                add(
                    project.dependencies.create(
                        mapOf(
                            "group" to "org.nodejs",
                            "name" to module,
                            "version" to nodeVersion,
                            "ext" to nodeDistribution.extension,
                        )
                    )
                )
                declareCustomAllureCommandlineRepository(
                    group = "org.nodejs",
                    module = module,
                    link = nodeDistributionLink(nodeVersion, nodeDistribution)
                )
            }
        }

        val allure3Package = configurations.register(ALLURE_3_PACKAGE_CONFIGURATION) {
            isCanBeResolved = true
            isCanBeConsumed = false
        }

        val downloadNode = tasks.register<DownloadNode>(DOWNLOAD_NODE_TASK_NAME) {
            nodeVersion.set(DEFAULT_NODE_VERSION)
            this.nodeDistribution.from(nodeDistribution)
        }

        val installAllure3 = tasks.register<InstallAllure3>(INSTALL_ALLURE_3_TASK_NAME) {
            dependsOn(downloadNode)
            allureVersion.set(allureExtension.version)
            nodeHome.set(downloadNode.flatMap { it.destinationDir })
            allurePackageOverride.from(allure3Package)
            installEnvironment.putAll(allureExtension.environment)
        }

        tasks.register<DownloadAllure>(ALLURE_DOWNLOAD_TASK_NAME) {
            allureVersion.set(allureExtension.version)
            this.allureCommandLine.from(allureCommandLine)
            nodeHome.set(downloadNode.flatMap { it.destinationDir })
            allure3Home.set(installAllure3.flatMap { it.destinationDir })
            dependsOn(
                allureExtension.version.map {
                    if (allureRuntimeFamily(it) == AllureRuntimeFamily.ALLURE_3) {
                        listOf(installAllure3)
                    } else {
                        emptyList<Any>()
                    }
                }
            )
        }
    }

    /**
     * `IvyArtifactRepository` supports patterns, however, we do not use it since
     * artifact declaration requires separate `repository url` + `layoutPattern`.
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

    private fun nodeDistributionLink(nodeVersion: String, distribution: NodeDistribution): String =
        "https://nodejs.org/dist/v$nodeVersion/node-v$nodeVersion-${distribution.classifier}.${distribution.extension}"

    fun RepositoryHandler.exclusiveRepo(
        group: String,
        module: String,
        configure: IvyArtifactRepository.() -> Unit
    ) {
        // Create an Ivy repository using public API and restrict its content to a single module
        ivy {
            configure()
            content { includeModule(group, module) }
        }
    }
}
