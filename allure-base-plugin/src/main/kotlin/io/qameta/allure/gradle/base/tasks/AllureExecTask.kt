package io.qameta.allure.gradle.base.tasks

import io.qameta.allure.gradle.util.conv
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import org.gradle.kotlin.dsl.property
import java.io.File

abstract class AllureExecTask constructor(objects: ObjectFactory) : DefaultTask() {
    @InputDirectory
    @PathSensitive(PathSensitivity.NONE)
    val allureHome = objects.directoryProperty()

    @Internal
    @Option(option = "verbose", description = "Switch on the verbose mode")
    val verbose = objects.property<Boolean>().conv(false)

    @get:Internal
    protected val allureExecutable: File
        get() {
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
            return allureExecutable
        }

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
    protected val rawResults: FileCollection
        get() =
            resultsDirs.get().filter { it.exists() && it.isDirectory }

    @InputFiles
    @SkipWhenEmpty
    @PathSensitive(PathSensitivity.RELATIVE)
    protected val inputFiles = project.files(resultsDirs.map { dirs -> dirs.map { project.fileTree(it) } })

    init {
        dependsOn(dependsOnTests.map { if (it) resultsDirs else emptyList<Any>() })
        // In any case, if user launches "./gradlew test allureReport" the report generation
        // should wait for test execution
        mustRunAfter(resultsDirs)
    }
}
