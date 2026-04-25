package io.qameta.allure.gradle.adapter

import io.qameta.allure.gradle.rule.GradleRunnerRule
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File

class MultipleTestTasksSharedResultsDirTest {
    @TempDir
    lateinit var tempDir: File

    companion object {
        @JvmStatic
        fun versions() = listOf("9.4.1", "8.14.3", "8.11.1")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("versions")
    fun `multiple realized test tasks can share the adapter results directory`(version: String) {
        val gradleRunner = GradleRunnerRule()
            .rootDir(tempDir)
            .version(version)
            .project("src/it/adapter-multiple-test-tasks-shared-results-dir")
            .tasks("test", "integrationTest")
            .build()

        assertThat(gradleRunner.buildResult.task(":test")?.outcome)
            .`as`("test task outcome")
            .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(gradleRunner.buildResult.task(":integrationTest")?.outcome)
            .`as`("integrationTest task outcome")
            .isEqualTo(TaskOutcome.SUCCESS)

        val resultsDir = gradleRunner.projectDir.resolve("build/allure-results")
        assertThat(resultsDir)
            .`as`("shared Allure results directory")
            .isNotEmptyDirectory()
        assertThat(resultsDir.listFiles())
            .`as`("Allure results test cases")
            .filteredOn { file -> file.name.endsWith("result.json") }
            .hasSize(2)
    }
}
