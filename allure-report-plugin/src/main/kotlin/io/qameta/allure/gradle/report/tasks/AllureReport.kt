package io.qameta.allure.gradle.report.tasks

import io.qameta.allure.gradle.base.AllureExtension
import io.qameta.allure.gradle.base.tasks.AllureExecTask
import io.qameta.allure.gradle.util.conv
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.the
import report
import java.io.File
import javax.inject.Inject

open class AllureReport @Inject constructor(objects: ObjectFactory) : AllureExecTask(objects) {
    @OutputDirectory
    val reportDir = objects.directoryProperty().conv(
        project.the<AllureExtension>().report.reportDir.map { it.dir(this@AllureReport.name) }
    )

    @Option(option = "report-dir", description = "The directory to generate Allure report into")
    fun setReportDir(directory: String) {
        reportDir.set(project.layout.dir(project.provider { File(directory) }))
    }

    @Input
    @Option(option = "clean", description = "Clean Allure report directory before generating a new one")
    val clean = objects.property<Boolean>().conv(false)

    companion object {
        const val NAME = "allureReport"
        const val GENERATE_COMMAND = "generate"
    }

    @TaskAction
    fun generateAllureReport() {
        val rawResults = rawResults.map { it.absolutePath }
        logger.info("Input directories for $name: $rawResults")
        project.exec {
            executable(allureExecutable)
            if (verbose.get()) {
                args("--verbose")
            }
            args(GENERATE_COMMAND)
            args(rawResults)
            args("-o", reportDir.get().asFile.absolutePath)
            // TODO: replace with Gradle's delete?
            if (clean.get()) {
                args("--clean")
            }
        }
    }
}
