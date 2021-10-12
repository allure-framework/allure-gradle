package io.qameta.allure.gradle.report.tasks

import io.qameta.allure.gradle.base.tasks.AllureExecTask
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.kotlin.dsl.property
import java.io.File
import java.io.InputStream
import java.io.PrintStream
import java.lang.management.ManagementFactory
import java.util.concurrent.TimeUnit
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
        val allureArgs = mutableListOf<Any>().apply {
            if (verbose.get()) {
                add("--verbose")
            }
            add(SERVE_COMMAND)
            host.orNull?.let {
                add("--host")
                add(it)
            }
            port.orNull?.let {
                add("--port")
                add(it)
            }
            addAll(rawResults)
        }

        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            // Workaround https://github.com/gradle/gradle/issues/7603
            // The issues is that "terminate process" in Windows does not terminate its children
            startWithProcessBuilder(allureExecutable, allureArgs)
            return
        }

        project.exec {
            executable(allureExecutable)
            args(allureArgs)
        }
    }

    private fun startWithProcessBuilder(allureExecutable: File, allureArgs: List<Any>) {
        val cmd = listOf("cmd", "/c", allureExecutable.toString()) + allureArgs.map { it.toString() }
        logger.info("Starting $cmd")
        ProcessBuilder(cmd).start().apply {
            if (isAlive) {
                val allurePid = processOrParentPid
                project.gradle.buildFinished {
                    logger.info("Terminating process $allurePid to stop allure serve")
                    // /T kills all the children, so it does terminate 'allure serve' command
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
            // Java 9+
            Process::class.java.getMethod("pid").invoke(this) as Long
        } catch (t: Throwable) {
            // Almost all the implementations return name as pid@...
            // https://stackoverflow.com/a/35885/1261287
            ManagementFactory.getRuntimeMXBean().name.substringBefore('@').toLong().also {
                logger.info("Will terminate process $it (Gradle Daemon?) when ctrl+c is pressed. Consider upgrading to Java 11+")
            }
        }

    private fun Process.forwardStreams(name: String) = apply {
        // ProcessBuilder.inheritIO does not work, see https://github.com/gradle/gradle/issues/16719
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
