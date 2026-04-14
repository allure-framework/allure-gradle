package io.qameta.allure.gradle.report.tasks

import io.qameta.allure.gradle.base.tasks.AllureExecTask
import io.qameta.allure.gradle.base.tasks.ConditionalArgumentProvider
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.file.ProjectLayout
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.options.Option
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.the
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import report
import java.io.InputStream
import java.io.PrintStream
import java.lang.management.ManagementFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@DisableCachingByDefault(because = "Not worth caching")
abstract class AllureServe : AllureExecTask() {
    @get:Inject
    abstract val layout: ProjectLayout

    @get:Inject
    abstract val execOperations: ExecOperations

    companion object {
        const val NAME = "allureServe"
        const val SERVE_COMMAND = "serve"
        const val OPEN_COMMAND = "open"
    }

    init {
        outputs.upToDateWhen { false }
    }

    @Internal
    @Option(option = "host", description = "This host will be used to start web server for the report")
    val host = objects.property<String>()

    @Internal
    val port = objects.property<Int>()

    @get:Internal
    val allure3ReportDir = objects.directoryProperty().convention(
        project.the<io.qameta.allure.gradle.base.AllureExtension>().report.reportDir.map {
            it.dir(this@AllureServe.name)
        }
    )

    @get:Internal
    val allure3ConfigFile = layout.file(
        providers.provider { temporaryDir.resolve("allurerc.json") }
    )

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
        if (usesAllure3Runtime()) {
            execAllure3()
            return
        }
        if (!Os.isFamily(Os.FAMILY_WINDOWS)) {
            super.exec()
            return
        }
        startWithProcessBuilder(
            allureExecutable = resolveAllureExecutable().absolutePath,
            allureArgs = (args?.toMutableList() ?: mutableListOf<String>()) +
                argumentProviders.flatMap { it.asArguments() },
            environment = resolveEnvironment()
        )
    }

    private fun execAllure3() {
        require(!host.isPresent) {
            "--host is not supported for Allure 3. Use --port only or set allure.version to a 2.x release."
        }

        val allureExecutablePath = resolveAllureExecutable().absolutePath
        val resolvedEnvironment = resolveEnvironment()
        val configFile = allure3ConfigFile.get().asFile
        val reportDir = allure3ReportDir.get().asFile

        writeAllure3Config(
            file = configFile,
            outputDir = reportDir,
            singleFile = false
        )

        val generateArgs = mutableListOf<String>().apply {
            add("generate")
            addAll(rawResults.get().map { it.absolutePath })
            add("--config")
            add(configFile.absolutePath)
        }

        execOperations.exec {
            executable = allureExecutablePath
            args(generateArgs)
            environment(resolvedEnvironment)
        }

        val openArgs = mutableListOf<String>().apply {
            add(OPEN_COMMAND)
            add(reportDir.absolutePath)
            add("--config")
            add(configFile.absolutePath)
            port.orNull?.let {
                add("--port")
                add(it.toString())
            }
        }

        if (!Os.isFamily(Os.FAMILY_WINDOWS)) {
            execOperations.exec {
                executable = allureExecutablePath
                args(openArgs)
                environment(resolvedEnvironment)
            }
            return
        }

        startWithProcessBuilder(
            allureExecutable = allureExecutablePath,
            allureArgs = openArgs,
            environment = resolvedEnvironment
        )
    }

    private fun startWithProcessBuilder(
        allureExecutable: String,
        allureArgs: List<Any>,
        environment: Map<String, String>
    ) {
        val cmd = buildWindowsCommand(allureExecutable, allureArgs)
        logger.info("Starting $cmd")
        ProcessBuilder(cmd)
            .apply {
                for ((key, value) in environment) {
                    environment()[key] = value
                }
            }
            .start().apply {
                if (isAlive) {
                    val allurePid = processOrParentPid
                    project.gradle.buildFinished {
                        logger.info("Terminating process $allurePid to stop allure serve")
                        ProcessBuilder("taskkill", "/PID", allurePid.toString(), "/T", "/F").start().apply {
                            forwardStreams("terminate allure serve")
                            waitFor(15, TimeUnit.SECONDS)
                        }
                    }
                }
                outputStream.close()
                forwardStreams("allure serve")
                waitFor()
            }
    }

    private val Process.processOrParentPid: Long
        get() = try {
            Process::class.java.getMethod("pid").invoke(this) as Long
        } catch (t: Throwable) {
            ManagementFactory.getRuntimeMXBean().name.substringBefore('@').toLong().also {
                logger.info("Will terminate process $it (Gradle Daemon?) when ctrl+c is pressed. Consider upgrading to Java 11+")
            }
        }

    private fun Process.forwardStreams(name: String) = apply {
        forwardStream("$name stdout", inputStream, System.out)
        forwardStream("$name stderr", errorStream, System.err)
    }

    private fun forwardStream(streamName: String, inputStream: InputStream?, out: PrintStream) {
        Thread {
            inputStream?.buffered()?.copyTo(out)
        }.apply {
            isDaemon = true
            name = "Allure serve $streamName forwarder"
            start()
        }
    }
}

internal fun buildWindowsCommand(allureExecutable: String, allureArgs: List<Any>): List<String> =
    // `call` keeps cmd.exe from treating a quoted batch path as the whole command.
    listOf("cmd.exe", "/d", "/c", "call", allureExecutable) + allureArgs.map { it.toString() }
