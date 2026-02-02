package io.qameta.allure.gradle.adapter

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

@RunWith(Parameterized::class)
class CacheabilityTest {

    @JvmField
    @org.junit.Rule
    val gradleRunner = io.qameta.allure.gradle.rule.GradleRunnerRule()
        .version { version }
        .project { project }
        .tasks { arrayOf("test", "--build-cache") }

    @Parameterized.Parameter(0)
    lateinit var version: String

    @Parameterized.Parameter(1)
    lateinit var project: String

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "cacheable on {0}")
        fun data() = listOf(
            arrayOf("9.0.0", "src/it/adapter-cache-junit5-kts"),
            arrayOf("8.14.3", "src/it/adapter-cache-junit5-kts")
        )
    }

    @Test
    fun `test task is cacheable when allure adapter is applied`() {
        // Enable local build cache in settings.gradle written to the prepared projectDir
        val projectDir = gradleRunner.projectDir
        val testKitHome = projectDir.parentFile.resolve(".gradle")
        val cacheDir = testKitHome.resolve("build-cache").absoluteFile
        val settings = File(projectDir, "settings.gradle.kts")
        settings.writeText(
            """
            buildCache {
                local {
                    directory = file("${'$'}{file("${cacheDir.absolutePath.replace('\\','/')}")}")
                    removeUnusedEntriesAfterDays = 1
                }
            }
            """.trimIndent()
        )

        // First run already executed by rule (test task)
        assertThat(gradleRunner.buildResult.task(":test")?.outcome)
            .isIn(TaskOutcome.SUCCESS, TaskOutcome.NO_SOURCE)

        // Delete build dir and run again with build cache
        File(projectDir, "build").deleteRecursively()

        val second: BuildResult = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("--stacktrace", "--info", "--build-cache", "-Porg.gradle.daemon=false", "--no-watch-fs", "test")
            .withGradleVersion(version)
            .withTestKitDir(testKitHome)
            .withPluginClasspath()
            .forwardOutput()
            .build()

        assertThat(second.task(":test")?.outcome)
            .`as`("second run should be FROM_CACHE")
            .isEqualTo(TaskOutcome.FROM_CACHE)
    }
}
