package io.qameta.allure.gradle.adapter.tasks

import adapter
import io.qameta.allure.gradle.base.AllureExtension
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.setProperty
import org.gradle.kotlin.dsl.the
import java.io.File
import javax.inject.Inject

@CacheableTask
abstract class CopyCategories : DefaultTask() {
    @get:Inject
    abstract val objects: ObjectFactory

    @get:Inject
    abstract val layout: ProjectLayout

    @get:Inject
    abstract val fs: FileSystemOperations

    @Optional
    @InputFile
    @SkipWhenEmpty
    @PathSensitive(PathSensitivity.NONE)
    val categoriesFile = objects.fileProperty()
        .convention(project.the<AllureExtension>().adapter.categoriesFile)

    @Internal
    val destinationDirs = objects.setProperty<File>()

    @OutputFiles
    protected val outputFiles = destinationDirs.map { set ->
        set.map { it.resolve("categories.json") }
    }

    @OutputFile
    val markerFile = objects.directoryProperty()
        .convention(layout.buildDirectory.dir("copy-categories/$name"))

    @TaskAction
    fun run() {
        val categories = categoriesFile.get().asFile
        var didWork = false
        for (dir in destinationDirs.get()) {
            logger.warn("Copying $categories to $dir")
            didWork = didWork or fs.copy {
                into(dir)
                from(categories) {
                    rename {
                        "categories.json"
                    }
                }
            }.didWork
        }
        didWork = didWork or markerFile.get().asFile.mkdirs()
        this.didWork = didWork
    }
}
