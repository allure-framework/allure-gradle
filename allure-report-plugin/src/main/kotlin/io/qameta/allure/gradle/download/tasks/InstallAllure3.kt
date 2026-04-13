package io.qameta.allure.gradle.download.tasks

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.Property
import org.gradle.process.ExecOperations
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.mapProperty
import org.gradle.kotlin.dsl.property
import java.io.File
import javax.inject.Inject

abstract class InstallAllure3 : DefaultTask() {
    @get:Inject
    abstract val objects: ObjectFactory

    @get:Inject
    abstract val layout: ProjectLayout

    @get:Inject
    abstract val fs: FileSystemOperations

    @get:Inject
    abstract val execOperations: ExecOperations

    @get:Input
    abstract val allureVersion: Property<String>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val nodeHome: DirectoryProperty

    @get:Optional
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    val allurePackageOverride: ConfigurableFileCollection = objects.fileCollection()

    @get:Input
    val installEnvironment: MapProperty<String, Any> = objects.mapProperty<String, Any>()

    @get:OutputDirectory
    val destinationDir = layout.buildDirectory.dir("allure/allure3")

    @TaskAction
    fun installAllure3() {
        val installDir = destinationDir.get().asFile
        fs.delete {
            delete(installDir)
        }
        installDir.mkdirs()

        installDir.resolve("package.json").writeText(
            """
            {
              "name": "allure-gradle-runtime",
              "private": true
            }
            """.trimIndent()
        )

        val installTarget = resolveInstallTarget()
        val npmExecutable = npmExecutable(nodeHome.get().asFile)

        logger.info("Installing Allure {} into {}", allureVersion.get(), installDir)
        execOperations.exec {
            executable = npmExecutable.absolutePath
            args(
                "--prefix", installDir.absolutePath,
                "install",
                "--no-package-lock",
                "--no-save",
                "--ignore-scripts",
                installTarget
            )
            environment(resolveEnvironment())
        }
    }

    private fun resolveInstallTarget(): String {
        if (allurePackageOverride.isEmpty) {
            return "allure@${allureVersion.get()}"
        }
        require(allurePackageOverride.files.size == 1) {
            "allure3Package must resolve to a single archive"
        }
        return allurePackageOverride.singleFile.absolutePath
    }

    private fun npmExecutable(homeDir: File): File = if (Os.isFamily(Os.FAMILY_WINDOWS)) {
        homeDir.resolve("npm.cmd")
    } else {
        homeDir.resolve("bin").resolve("npm")
    }

    private fun resolveEnvironment(): Map<String, String> = installEnvironment.get().mapValues { unwrapProvider(it.value) }

    private fun unwrapProvider(value: Any): String = when (value) {
        is Provider<*> -> unwrapProvider(value.get())
        else -> value.toString()
    }
}
