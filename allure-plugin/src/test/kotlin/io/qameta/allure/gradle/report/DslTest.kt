package io.qameta.allure.gradle.report

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
        fun getFrameworks() = listOf(
            arguments("9.0.0", "src/it/full-dsl-kotlin"),
            arguments("8.14.3", "src/it/full-dsl-kotlin"),
            arguments("8.11.1", "src/it/full-dsl-kotlin"),
            arguments("9.0.0", "src/it/full-dsl-groovy"),
            arguments("8.14.3", "src/it/full-dsl-groovy"),
            arguments("8.11.1", "src/it/full-dsl-groovy"),
        )
    }

    @ParameterizedTest(name = "{1} [{0}]")
    @MethodSource("getFrameworks")
    fun `build script should compile`(version: String, project: String) {
        val gradleRunner = GradleRunnerRule()
            .rootDir(tempDir)
            .version(version)
            .project(project)
            .tasks("testDsl")
            .build()

        assertThat(gradleRunner.buildResult.tasks).`as`("testDsl task status")
            .filteredOn { task -> task.path == ":testDsl" }
            .extracting("outcome")
            .containsExactly(TaskOutcome.SUCCESS)
    }
}
