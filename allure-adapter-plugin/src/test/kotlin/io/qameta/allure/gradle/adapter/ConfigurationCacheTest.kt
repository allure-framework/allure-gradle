package io.qameta.allure.gradle.adapter

import io.qameta.allure.gradle.rule.GradleRunnerRule
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File

class ConfigurationCacheTest {
    @TempDir
    lateinit var tempDir: File

    companion object {
        @JvmStatic
        fun data() = listOf("9.0.0", "8.14.3", "8.11.1")
    }

    @ParameterizedTest(name = "configuration cache on {0}")
    @MethodSource("data")
    fun `test task can reuse configuration cache when allure adapter is applied`(version: String) {
        val gradleRunner = GradleRunnerRule()
            .rootDir(tempDir)
            .version(version)
            .project("src/it/adapter-cache-junit5-kts")
            .prepare()

        val firstRun = gradleRunner.run("test", "--configuration-cache")
        assertThat(firstRun.task(":test")?.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(firstRun.output)
            .contains("Configuration cache entry stored.")

        val secondRun = gradleRunner.newRunner("test", "--configuration-cache").build()

        assertThat(secondRun.output)
            .containsPattern("(Reusing configuration cache\\.|Configuration cache entry reused\\.)")
    }
}
