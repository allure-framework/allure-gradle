package io.qameta.allure.gradle.download.tasks

import io.qameta.allure.gradle.base.AllureExtension
import io.qameta.allure.gradle.download.AllureDownloadPlugin
import io.qameta.allure.gradle.download.commandlineExtension
import io.qameta.allure.gradle.download.hasAllure2CommandlineCustomization
import io.qameta.allure.gradle.download.allureRuntimeFamily
import io.qameta.allure.gradle.base.AllureRuntimeFamily
import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RelativePath
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.the
import java.io.File
import javax.inject.Inject

/**
 * The task downloads Allure distribution. It would probably be better to
 * use Gradle 6.1 [org.gradle.api.services.BuildService] to keep a single
 * Allure binary across all subprojects.
 */
@CacheableTask
abstract class DownloadAllure : DefaultTask() {
    @get:Inject
    abstract val objects: ObjectFactory

    @get:Inject
    abstract val layout: ProjectLayout

    @get:Inject
    abstract val fs: FileSystemOperations

    @get:Inject
    abstract val archiveOps: ArchiveOperations

    @Input
    val allureVersion = objects.property<String>()

    @InputFiles
    @Optional
    @PathSensitive(PathSensitivity.NONE)
    val allureCommandLine: ConfigurableFileCollection = objects.fileCollection()

    @get:org.gradle.api.tasks.Internal
    abstract val nodeHome: DirectoryProperty

    @get:org.gradle.api.tasks.Internal
    abstract val allure3Home: DirectoryProperty

    @OutputDirectory
    val destinationDir = layout.buildDirectory.dir("allure/commandline")

    @TaskAction
    fun downloadAllure() {
        when (allureRuntimeFamily(allureVersion.get())) {
            AllureRuntimeFamily.ALLURE_2 -> unpackAllure2()
            AllureRuntimeFamily.ALLURE_3 -> assembleAllure3()
        }
    }

    private fun unpackAllure2() {
        logger.info("Unpacking Allure 2 runtime to {}", destinationDir.get().asFile)
        val result = fs.sync {
            into(destinationDir)
            from(archiveOps.zipTree(allureCommandLine.singleFile)) {
                eachFile {
                    // Replace "allure-.../abc/..." with "abc/..." for predictable folder locations
                    relativePath = RelativePath(true, *relativePath.segments.drop(1).toTypedArray())
                }
                includeEmptyDirs = false
            }
        }
        didWork = result.didWork
    }

    private fun assembleAllure3() {
        validateAllure3Configuration()

        val destination = destinationDir.get().asFile
        logger.info("Assembling Allure 3 runtime to {}", destination)
        fs.delete {
            delete(destination)
        }
        ArchiveFileOperations.copyDirectory(nodeHome.get().asFile, destination.resolve("node"))
        ArchiveFileOperations.copyDirectory(allure3Home.get().asFile, destination.resolve("package"))
        writeLaunchers(destination)
        didWork = true
    }

    private fun validateAllure3Configuration() {
        val allureExtension = project.the<AllureExtension>()
        val commandline = allureExtension.commandlineExtension()
        val allureCommandlineConfig = project.configurations.getByName(AllureDownloadPlugin.ALLURE_COMMANDLINE_CONFIGURATION)

        val hasManualAllure2Dependency = allureCommandlineConfig.dependencies.isNotEmpty()
        if (allureExtension.hasAllure2CommandlineCustomization() || hasManualAllure2Dependency) {
            val details = buildString {
                append("Allure 3 does not support Allure 2 allure.commandline or allureCommandline customization. ")
                append("Set allure.version to a 2.x release to use Allure 2 commandline configuration.")
                if (commandline.downloadUrlPattern.orNull != null) {
                    append(" Detected allure.commandline.downloadUrlPattern.")
                }
                if (hasManualAllure2Dependency) {
                    append(" Detected manual allureCommandline dependencies.")
                }
            }
            throw IllegalArgumentException(details)
        }
    }

    private fun writeLaunchers(homeDir: File) {
        val binDir = homeDir.resolve("bin").apply { mkdirs() }
        val unixLauncher = binDir.resolve("allure")
        unixLauncher.writeText(
            """
            #!/bin/sh
            SCRIPT_DIR=${'$'}(CDPATH= cd -- "${'$'}(dirname "${'$'}0")" && pwd)
            exec "${'$'}SCRIPT_DIR/../node/bin/node" "${'$'}SCRIPT_DIR/../package/node_modules/allure/cli.js" "${'$'}@"
            """.trimIndent() + "\n"
        )
        unixLauncher.setExecutable(true)

        val windowsLauncher = binDir.resolve("allure.bat")
        windowsLauncher.writeText(
            (
                """
                @echo off
                "%~dp0..\node\node" "%~dp0..\package\node_modules\allure\cli.js" %*
                """.trimIndent() + "\r\n"
                )
        )
    }
}
