package io.qameta.allure.gradle.base.tasks

import io.qameta.allure.gradle.base.AllureExtension
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.the
import org.gradle.work.DisableCachingByDefault
import java.io.File
import javax.inject.Inject

@DisableCachingByDefault(because = "Not worth caching")
abstract class AllureExecTask() : Exec() {
    @get:Inject
    abstract val objects: ObjectFactory

    @get:Inject
    abstract val providers: ProviderFactory

    @InputDirectory
    @PathSensitive(PathSensitivity.NONE)
    val allureHome = objects.directoryProperty()

    @Internal
    @Option(option = "verbose", description = "Switch on the verbose mode")
    val verbose = objects.property<Boolean>().convention(false)

    /**
     * Gradle's [Exec.executable] does not support [Provider<String>], and it uses [Object.toString],
     * so we create an object that calls [Provider.get] in its [Object.toString].
     */
    protected fun <T: Any> Provider<T>.lazyToString() = object {
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
    protected val allureExecutable: Provider<File> = allureHome.map {
        defaultAllureExecutable(it.asFile)
    }

    // InputDirectories does not exist yet: https://github.com/gradle/gradle/issues/7485#issuecomment-585289792
    @Internal
    val resultsDirs: ConfigurableFileCollection = objects.fileCollection()

    @Option(option = "depends-on-tests", description = "Execute the relevant test tasks before launching Allure")
    fun dependsOnTests() {
        dependsOnTests.set(true)
    }

    /**
     * Typically, [allureReport] would execute all the tests it depends upon.
     * However, in certain cases only report re-execution is needed, then `skipDependsOn` would be useful.
     */
    @Input
    val dependsOnTests = objects.property<Boolean>().convention(false)

    @get:Internal
    protected val rawResults: Provider<FileCollection> = providers.provider {
        project.files(resultsDirs.filter { it.exists() && it.isDirectory })
    }

    @InputFiles
    @SkipWhenEmpty
    @IgnoreEmptyDirectories
    @PathSensitive(PathSensitivity.RELATIVE)
    protected val inputFiles = project.files(
        providers.provider { resultsDirs.files.map { project.fileTree(it) } }
    )

    @get:Internal
    protected abstract val defaultEnvironment: MapProperty<String, Any>

    init {
        defaultEnvironment.convention(project.the<AllureExtension>().environment)
        dependsOn(dependsOnTests.map { if (it) resultsDirs else emptyList<Any>() })
        // In any case, if user launches "./gradlew test allureReport" the report generation
        // should wait for test execution
        // See https://github.com/allure-framework/allure-gradle/issues/90
        // See https://github.com/gradle/gradle/issues/21962
        mustRunAfter(resultsDirs)
    }

    override fun exec() {
        val allureExecutable = resolveAllureExecutable()
        executable = allureExecutable.absolutePath

        val resolvedEnvironment = resolveEnvironment()
        environment.clear()
        environment.putAll(resolvedEnvironment)
        super.exec()
    }

    protected fun resolveAllureExecutable(): File = validateAllureExecutable()

    protected fun resolveEnvironment(): Map<String, String> {
        val resolvedEnvironment = linkedMapOf<String, String>()
        for ((key, value) in environment) {
            resolvedEnvironment[key] = unwrapProvider(value)
        }
        for ((key, value) in defaultEnvironment.get()) {
            if (key !in resolvedEnvironment) {
                logger.info("Adding $key to environment properties (value omitted for security reasons)")
                resolvedEnvironment[key] = unwrapProvider(value)
            }
        }
        return resolvedEnvironment
    }

    private fun defaultAllureExecutable(homeDir: File): File {
        val binDir = homeDir.resolve("bin")
        return if (!Os.isFamily(Os.FAMILY_WINDOWS)) {
            binDir.resolve("allure")
        } else {
            binDir.resolve("allure.bat")
        }
    }

    private fun validateAllureExecutable(): File {
        val homeDir = allureHome.get().asFile
        val defaultExecutable = defaultAllureExecutable(homeDir)
        val allureExecutable = if (Os.isFamily(Os.FAMILY_WINDOWS) && !defaultExecutable.exists()) {
            homeDir.resolve("bin").resolve("allure.cmd")
        } else {
            defaultExecutable
        }
        if (!allureExecutable.exists()) {
            throw IllegalArgumentException("Cannot find allure commandline in $homeDir")
        }
        allureExecutable.setExecutable(true)
        return allureExecutable
    }
}
