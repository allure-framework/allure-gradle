package io.qameta.allure.gradle.adapter

import io.qameta.allure.gradle.rule.GradleRunnerRule
import org.assertj.core.api.Assertions
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import org.junit.jupiter.params.provider.Arguments.arguments

class AssembleTest {
    @TempDir
    lateinit var tempDir: File

    companion object {
        @JvmStatic
        fun getFrameworks() = listOf(
            arguments("9.4.1", "src/it/adapter-assemble", arrayOf("assemble")),
            arguments("8.14.3", "src/it/adapter-assemble", arrayOf("assemble")),
            arguments("8.11.1", "src/it/adapter-assemble", arrayOf("assemble")),
        )
    }

    @ParameterizedTest(name = "{1} [{0}]")
    @MethodSource("getFrameworks")
    fun `assemble should not execute tests`(version: String, project: String, tasks: Array<String>) {
        val gradleRunner = GradleRunnerRule()
            .rootDir(tempDir)
            .version(version)
            .project(project)
            .tasks(*tasks)
            .build()

        Assertions.assertThat(gradleRunner.buildResult.tasks)
            .`as`("assemble should succeed, and test must not be executed")
            .filteredOn { task -> task.path == ":assemble" }
            .extracting("outcome")
            .containsAnyOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE)
    }
}
