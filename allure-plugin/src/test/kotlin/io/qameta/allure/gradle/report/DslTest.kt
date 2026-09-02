package io.qameta.allure.gradle.report

import io.qameta.allure.gradle.rule.GradleTestVersion
import io.qameta.allure.gradle.rule.GradleRunnerRule
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import org.junit.jupiter.params.provider.Arguments.arguments

class DslTest {
    @TempDir
    lateinit var tempDir: File

    companion object {
        @JvmStatic
        fun getFrameworks(): List<org.junit.jupiter.params.provider.Arguments> {
            val gradleVersion = GradleTestVersion.current()
            return listOf(
                arguments(gradleVersion, "src/it/full-dsl-kotlin"),
                arguments(gradleVersion, "src/it/full-dsl-groovy"),
            )
        }
    }

    @ParameterizedTest(name = "{1} [{0}]")
    @MethodSource("getFrameworks")
    fun `build script should compile without Allure deprecation warnings`(version: String, project: String) {
        val gradleRunner = GradleRunnerRule()
            .rootDir(tempDir)
            .version(version)
            .project(project)
            .tasks("--warning-mode=all", "testDsl")
            .build()

        assertThat(gradleRunner.buildResult.tasks).`as`("testDsl task status")
            .filteredOn { task -> task.path == ":testDsl" }
            .extracting("outcome")
            .containsExactly(TaskOutcome.SUCCESS)

        assertThat(gradleRunner.buildResult.output).`as`("Gradle deprecation warnings")
            .doesNotContain(
                "The 'val name by creating { }' property delegate syntax has been deprecated.",
                "The 'val name by registering(Type::class) { }' property delegate syntax has been deprecated.",
                "Using a Project object as a dependency notation has been deprecated.",
            )
    }
}
