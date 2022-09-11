package io.qameta.allure.gradle.base.tasks

import io.qameta.allure.gradle.base.AllureExtension
import io.qameta.allure.gradle.util.conv
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.the
import org.gradle.util.GradleVersion
import java.io.File

abstract class AllureExecTask constructor(objects: ObjectFactory) : Exec() {
    @InputDirectory
    @PathSensitive(PathSensitivity.NONE)
    val allureHome = objects.directoryProperty()

    @Internal
    @Option(option = "verbose", description = "Switch on the verbose mode")
    val verbose = objects.property<Boolean>().conv(false)

    /**
     * Gradle's [Exec.executable] does not support [Provider<String>], and it uses [Object.toString],
     * so we create an object that calls [Provider.get] in its [Object.toString].
     */
    protected fun<T> Provider<T>.lazyToString() = object {
        override fun toString(): String = this@lazyToString.get().toString()
    }

    /**
     * We might need to recover values from [Provider] values stored in [Exec.getEnvironment]
     * when launching process via [ProcessBuilder].
     */
    private fun unwrapProvider(value: Any): String = when (value) {
        is Provider<*> -> unwrapProvider(value.get())
        else -> value.toString()
    }

    @get:Internal
    protected val allureExecutable = objects.property<File>().conv(
        project.provider {
            val homeDir = allureHome.get().asFile
            val binDir = homeDir.resolve("bin")

            val allureExecutable = if (!Os.isFamily(Os.FAMILY_WINDOWS)) {
                binDir.resolve("allure")
            } else {
                binDir.resolve("allure.cmd").takeIf { it.exists() } ?: binDir.resolve("allure.bat")
            }
            if (!allureExecutable.exists()) {
                throw IllegalArgumentException("Cannot find allure commandline in $homeDir")
            }

            allureExecutable.setExecutable(true)
            allureExecutable
        }
    )

    // InputDirectories does not exist yet: https://github.com/gradle/gradle/issues/7485#issuecomment-585289792
    @Internal
    val resultsDirs = objects.property<Configuration>()

    @Option(option = "depends-on-tests", description = "Execute the relevant test tasks before launching Allure")
    fun dependsOnTests() {
        dependsOnTests.set(true)
    }

    /**
     * Typically, [allureReport] would execute all the tests it depends upon.
     * However, in certain cases only report re-execution is needed, then `skipDependsOn` would be useful.
     */
    @Input
    val dependsOnTests = objects.property<Boolean>().conv(false)

    @get:Internal
    protected val rawResults: Provider<FileCollection> =
        resultsDirs.map { it.filter { it.exists() && it.isDirectory } }

    @InputFiles
    @SkipWhenEmpty
    @IgnoreEmptyDirectories
    @PathSensitive(PathSensitivity.RELATIVE)
    protected val inputFiles = project.files(resultsDirs.map { dirs -> dirs.map { project.fileTree(it) } })

    @get:Internal
    protected abstract val defaultEnvironment: MapProperty<String, Any>

    init {
        defaultEnvironment.convention(project.the<AllureExtension>().environment)
        dependsOn(dependsOnTests.map { if (it) resultsDirs else emptyList<Any>() })
        // In any case, if user launches "./gradlew test allureReport" the report generation
        // should wait for test execution
        if (GradleVersion.current() < GradleVersion.version("7.5")) {
            mustRunAfter(resultsDirs)
        } else {
            // See https://github.com/allure-framework/allure-gradle/issues/90
            // See https://github.com/gradle/gradle/issues/21962
            mustRunAfter(resultsDirs.map { it.elements })
        }
    }

    override fun exec() {
        val environment = environment
        for ((key, value) in defaultEnvironment.get()) {
            if (key !in environment) {
                logger.info("Adding $key to environment properties (value omitted for security reasons)")
                environment[key] = unwrapProvider(value)
            }
        }
        super.exec()
    }
}
