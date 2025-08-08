package io.qameta.allure.gradle.download.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RelativePath
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
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

    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    val allureCommandLine = objects.property<Configuration>()

    @OutputDirectory
    val destinationDir = layout.buildDirectory.dir("allure/commandline")

    @TaskAction
    fun downloadAllure() {
        logger.info("Unpacking Allure Commandline to ${destinationDir.get()}")
        val result = fs.sync {
            into(destinationDir)
            from(archiveOps.zipTree(allureCommandLine.get().singleFile)) {
                eachFile {
                    // Replace "allure-.../abc/..." with "abc/..." for predictable folder locations
                    relativePath = RelativePath(true, *relativePath.segments.drop(1).toTypedArray())
                }
                includeEmptyDirs = false
            }
        }
        didWork = result.didWork
    }
}
