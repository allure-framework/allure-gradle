package io.qameta.allure.gradle.adapter

import io.qameta.allure.gradle.rule.GradleRunnerRule
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File

class CustomResultsDirTest {
    @TempDir
    lateinit var tempDir: File

    companion object {
        @JvmStatic
        fun versions() = listOf("9.0.0", "8.14.3", "8.11.1")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("versions")
    fun `adapter plugin writes raw results to custom directory`(version: String) {
        val gradleRunner = GradleRunnerRule()
            .rootDir(tempDir)
            .version(version)
            .project("src/it/adapter-custom-results-dir")
            .tasks("test")
            .build()

        assertThat(gradleRunner.buildResult.task(":test")?.outcome)
            .`as`("test task outcome")
            .isEqualTo(TaskOutcome.SUCCESS)

        assertThat(gradleRunner.projectDir.resolve("build/allure-results"))
            .`as`("default Allure results directory")
            .doesNotExist()

        val customResultsDir = gradleRunner.projectDir.resolve("build/custom-allure-results")
        assertThat(customResultsDir)
            .`as`("custom Allure results directory")
            .isNotEmptyDirectory()
        assertThat(customResultsDir.listFiles())
            .`as`("Allure results test cases")
            .filteredOn { file -> file.name.endsWith("result.json") }
            .hasSize(1)
    }
}
