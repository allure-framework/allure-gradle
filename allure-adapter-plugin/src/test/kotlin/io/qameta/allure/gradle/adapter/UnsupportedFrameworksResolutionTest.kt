package io.qameta.allure.gradle.adapter

import io.qameta.allure.gradle.rule.GradleRunnerRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test

class UnsupportedFrameworksResolutionTest {
    @Rule
    @JvmField
    val gradleRunner = GradleRunnerRule()
        .version("9.0.0")
        .project("src/it/adapter-resolution-testng-unsupported")
        .tasks("writeResolvedArtifacts")

    @Test
    fun `unsupported testng versions should not resolve allure adapter`() {
        val resolved = gradleRunner.projectDir.resolve("build/resolvedArtifacts.txt").readText()

        assertThat(resolved)
            .doesNotContain("io.qameta.allure:allure-testng:")
    }
}
