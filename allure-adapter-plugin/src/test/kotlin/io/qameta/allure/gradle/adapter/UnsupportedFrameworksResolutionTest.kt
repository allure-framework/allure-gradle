package io.qameta.allure.gradle.adapter

import io.qameta.allure.gradle.rule.GradleRunnerRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class UnsupportedFrameworksResolutionTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun `unsupported testng versions should not resolve allure adapter`() {
        val gradleRunner = GradleRunnerRule()
            .rootDir(tempDir)
            .version("9.4.1")
            .project("src/it/adapter-resolution-testng-unsupported")
            .tasks("writeResolvedArtifacts")
            .build()

        val resolved = gradleRunner.projectDir.resolve("build/resolvedArtifacts.txt").readText()

        assertThat(resolved)
            .doesNotContain("io.qameta.allure:allure-testng:")
    }
}
