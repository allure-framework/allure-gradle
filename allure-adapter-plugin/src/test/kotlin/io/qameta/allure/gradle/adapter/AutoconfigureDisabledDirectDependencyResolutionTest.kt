package io.qameta.allure.gradle.adapter

import io.qameta.allure.gradle.rule.GradleRunnerRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File

class AutoconfigureDisabledDirectDependencyResolutionTest {
    @TempDir
    lateinit var tempDir: File

    companion object {
        @JvmStatic
        fun data() = listOf("9.0.0", "8.14.3", "8.11.1")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun `autoconfigure false should not substitute direct allure adapter dependencies with spi-off`(version: String) {
        val gradleRunner = GradleRunnerRule()
            .rootDir(tempDir)
            .version(version)
            .project("src/it/adapter-resolution-direct-allure-testng-autoconfigure-off")
            .tasks("writeResolvedArtifacts")
            .build()

        val resolved = gradleRunner.projectDir.resolve("build/resolvedArtifacts.txt").readText()

        assertThat(resolved)
            .contains("io.qameta.allure:allure-testng:2.33.0:allure-testng-2.33.0.jar")
            .doesNotContain("spi-off")
    }
}
