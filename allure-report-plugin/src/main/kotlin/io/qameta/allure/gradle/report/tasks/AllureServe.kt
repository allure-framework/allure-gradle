package io.qameta.allure.gradle.report.tasks

import io.qameta.allure.gradle.base.tasks.AllureExecTask
import io.qameta.allure.gradle.base.tasks.ConditionalArgumentProvider
import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.DefaultExecutor
import org.apache.commons.exec.PumpStreamHandler
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.options.Option
import org.gradle.kotlin.dsl.property
import org.gradle.work.DisableCachingByDefault
import java.io.File

@DisableCachingByDefault(because = "Not worth caching")
abstract class AllureServe : AllureExecTask() {
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

    init {
        executable(allureExecutable.map { it.absolutePath }.lazyToString())
        argumentProviders += ConditionalArgumentProvider(
            providers.provider {
                val args = mutableListOf<String>()
                if (verbose.get()) {
                    args += "--verbose"
                }
                args += SERVE_COMMAND
                host.orNull?.let {
                    args.add("--host")
                    args.add(it)
                }
                port.orNull?.let {
                    args.add("--port")
                    args.add(it.toString())
                }
                val rawResults = rawResults.get().map { it.absolutePath }
                logger.info("Input directories for $name: $rawResults")
                args.addAll(rawResults)
                args
            }
        )
    }

    override fun exec() {
        val resolvedExecutable = resolveAllureExecutable()
        val resolvedArgs = (args?.toMutableList() ?: mutableListOf<String>()) +
            argumentProviders.flatMap { it.asArguments() }
        val commandLine = buildAllureCommandLine(
            allureExecutable = resolvedExecutable.absolutePath,
            allureArgs = resolvedArgs,
            handleQuoting = Os.isFamily(Os.FAMILY_WINDOWS)
        )

        val executor = DefaultExecutor.builder().get().apply {
            setExitValue(0)
            workingDir?.let { setWorkingDirectory(it) }
            streamHandler = PumpStreamHandler(System.out, System.err)
        }

        logger.info("Starting {}", commandLine)
        executor.execute(commandLine, resolveEnvironment())
    }
}

internal fun buildAllureCommandLine(
    allureExecutable: String,
    allureArgs: List<Any>,
    handleQuoting: Boolean
): CommandLine = CommandLine(File(allureExecutable)).apply {
    allureArgs.forEach { addArgument(it.toString(), handleQuoting) }
}
