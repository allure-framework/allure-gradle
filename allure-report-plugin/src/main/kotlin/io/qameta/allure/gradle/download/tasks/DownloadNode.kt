package io.qameta.allure.gradle.download.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
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
        if (!archive.isSupportedArchive()) {
            throw IllegalArgumentException(
                "Unsupported Node.js archive format: ${archive.name}. Supported formats: .zip, .tar.gz, .tgz"
            )
        }
        val destination = destinationDir.get().asFile
        fs.delete {
            delete(destination)
        }
        when {
            archive.name.endsWith(".zip") -> ArchiveFileOperations.extractZip(archive, destination)
            archive.isTarGzip() -> ArchiveFileOperations.extractTarGzip(archive, destination)
        }
        didWork = true
    }

    private fun File.isSupportedArchive() = name.endsWith(".zip") || isTarGzip()

    private fun File.isTarGzip() = name.endsWith(".tar.gz") || name.endsWith(".tgz")
}
