package io.qameta.allure.gradle.adapter

import io.qameta.allure.gradle.rule.GradleRunnerRule
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import org.junit.jupiter.params.provider.Arguments.arguments

class CacheabilityTest {
    @TempDir
    lateinit var tempDir: File

    companion object {
        @JvmStatic
        fun data() = listOf(
            arguments("9.0.0", "src/it/adapter-cache-junit5-kts"),
            arguments("8.14.3", "src/it/adapter-cache-junit5-kts"),
        )
    }

    @ParameterizedTest(name = "cacheable on {0}")
    @MethodSource("data")
    fun `test task is cacheable when allure adapter is applied`(version: String, project: String) {
        val gradleRunner = GradleRunnerRule()
            .rootDir(tempDir)
            .version(version)
            .project(project)
            .prepare()

        val projectDir = gradleRunner.projectDir
        val cacheDir = projectDir.resolve(".gradle-test-build-cache").absoluteFile
        val settings = File(projectDir, "settings.gradle.kts")
        settings.writeText(
            """
            buildCache {
                local {
                    directory = file("${cacheDir.absolutePath.replace('\\', '/')}")
                }
            }
            """.trimIndent()
        )

        val firstRun = gradleRunner.run("test", "--build-cache")
        assertThat(firstRun.task(":test")?.outcome)
            .isIn(TaskOutcome.SUCCESS, TaskOutcome.NO_SOURCE, TaskOutcome.FROM_CACHE)

        File(projectDir, "build").deleteRecursively()

        val second: BuildResult = GradleRunnerRule.runBuild(
            projectDir,
            version,
            listOf("test", "--build-cache")
        ) {
            gradleRunner.newRunner("test", "--build-cache").build()
        }

        assertThat(second.task(":test")?.outcome)
            .`as`("second run should be FROM_CACHE")
            .isEqualTo(TaskOutcome.FROM_CACHE)
    }
}
