package io.qameta.allure.gradle.download.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.RelativePath
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

/**
 * The task downloads Allure distribution.
 * It would probably be better to use Gradle 6.1 [org.gradle.api.services.BuildService] to keep
 * a single Allure binary across all subprojects.
 */
open class DownloadAllure @Inject constructor(objects: ObjectFactory) : DefaultTask() {
    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    val allureCommandLine = objects.property<Configuration>()

    @OutputDirectory
    val destinationDir = project.layout.buildDirectory.dir("allure/commandline")

    @TaskAction
    fun downloadAllure() {
        logger.info("Unpacking Allure Commandline to ${destinationDir.get()}")
        val result = project.sync {
            into(destinationDir)
            from(project.zipTree(allureCommandLine.get().singleFile)) {
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
