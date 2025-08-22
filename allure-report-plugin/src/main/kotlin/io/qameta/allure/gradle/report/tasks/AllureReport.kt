package io.qameta.allure.gradle.report.tasks

import io.qameta.allure.gradle.base.AllureExtension
import io.qameta.allure.gradle.base.tasks.AllureExecTask
import io.qameta.allure.gradle.base.tasks.ConditionalArgumentProvider
import org.gradle.api.file.ProjectLayout
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.options.Option
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.the
import org.gradle.work.DisableCachingByDefault
import report
import java.io.File
import javax.inject.Inject

@DisableCachingByDefault(because = "Not worth caching")
abstract class AllureReport : AllureExecTask() {
    @get:Inject
    abstract val layout: ProjectLayout

    @OutputDirectory
    val reportDir = objects.directoryProperty().convention(
        project.the<AllureExtension>().report.reportDir.map { it.dir(this@AllureReport.name) }
    )

    @Option(option = "report-dir", description = "The directory to generate Allure report into")
    fun setReportDir(directory: String) {
        reportDir.set(layout.dir(providers.provider { File(directory) }))
    }

    @Input
    @Option(option = "clean", description = "Clean Allure report directory before generating a new one")
    val clean = objects.property<Boolean>().convention(false)

    @Input
    val singleFile = objects.property<Boolean>().convention(
        project.the<AllureExtension>().report.singleFile
    )

    companion object {
        const val NAME = "allureReport"
        const val GENERATE_COMMAND = "generate"
    }

    init {
        executable(allureExecutable.map { it.absolutePath }.lazyToString())
        argumentProviders += ConditionalArgumentProvider(
            providers.provider {
                val args = mutableListOf<String>()
                if (verbose.get()) {
                    args += "--verbose"
                }
                args += GENERATE_COMMAND
                val rawResults = rawResults.get().map { it.absolutePath }
                logger.info("Input directories for $name: $rawResults")
                args.addAll(rawResults)
                args += "-o"
                args += reportDir.get().asFile.absolutePath
                if (clean.get()) {
                    args += "--clean"
                }
                if (singleFile.get()) {
                    args += "--single-file"
                }
                args
            }
        )
    }
}
