package io.qameta.allure.gradle.report.tasks

import io.qameta.allure.gradle.base.tasks.AllureExecTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

open class AllureServe @Inject constructor(objects: ObjectFactory) : AllureExecTask(objects) {
    companion object {
        const val NAME = "allureServe"
        const val SERVE_COMMAND = "serve"
    }

    init {
        outputs.upToDateWhen { false }
    }

    @Internal
    @Option(option = "host", description = "This host will be used to start web server for the report")
    val host = objects.property<String>()

    @Internal
    val port = objects.property<Int>()

    @Option(option = "port", description = "This port will be used to start web server for the report")
    fun setPort(port: String) {
        this.port.set(port.toInt())
    }

    @TaskAction
    fun serveAllureReport() {
        val rawResults = rawResults.map { it.absolutePath }
        logger.info("Input directories for $name: $rawResults")
        project.exec {
            executable(allureExecutable)
            if (verbose.get()) {
                args("--verbose")
            }
            args(SERVE_COMMAND)
            host.orNull?.let {
                args("--host", it)
            }
            port.orNull?.let {
                args("--port", it)
            }
            args(rawResults)
        }
    }
}
