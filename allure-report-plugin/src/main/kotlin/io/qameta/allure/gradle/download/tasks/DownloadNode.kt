package io.qameta.allure.gradle.download.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RelativePath
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class DownloadNode : DefaultTask() {
    @get:Inject
    abstract val objects: ObjectFactory

    @get:Inject
    abstract val layout: ProjectLayout

    @get:Inject
    abstract val fs: FileSystemOperations

    @get:Inject
    abstract val archiveOps: ArchiveOperations

    @Input
    val nodeVersion = objects.property<String>()

    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    val nodeDistribution: ConfigurableFileCollection = objects.fileCollection()

    @OutputDirectory
    val destinationDir = layout.buildDirectory.dir("allure/node")

    @TaskAction
    fun downloadNode() {
        val archive = nodeDistribution.singleFile
        logger.info("Unpacking Node.js {} to {}", nodeVersion.get(), destinationDir.get().asFile)
        val result = fs.sync {
            into(destinationDir)
            from(archiveTree(archive)) {
                eachFile {
                    relativePath = RelativePath(true, *relativePath.segments.drop(1).toTypedArray())
                }
                includeEmptyDirs = false
            }
        }
        didWork = result.didWork
    }

    private fun archiveTree(archive: File) = when {
        archive.name.endsWith(".zip") -> archiveOps.zipTree(archive)
        archive.name.endsWith(".tar.gz") || archive.name.endsWith(".tgz") ->
            project.tarTree(project.resources.gzip(archive))
        else -> throw IllegalArgumentException(
            "Unsupported Node.js archive format: ${archive.name}. Supported formats: .zip, .tar.gz, .tgz"
        )
    }
}

