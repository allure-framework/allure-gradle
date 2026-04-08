package io.qameta.allure.gradle.adapter

import io.qameta.allure.gradle.rule.GradleRunnerRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class AutoconfigureDisabledDirectDependencyResolutionTest {
    @Rule
    @JvmField
    val gradleRunner = GradleRunnerRule()
        .version { version }
        .project("src/it/adapter-resolution-direct-allure-testng-autoconfigure-off")
        .tasks("writeResolvedArtifacts")

    @Parameterized.Parameter
    lateinit var version: String

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            arrayOf("9.0.0"),
            arrayOf("8.14.3"),
            arrayOf("8.11.1"),
        )
    }

    @Test
    fun `autoconfigure false should not substitute direct allure adapter dependencies with spi-off`() {
        val resolved = gradleRunner.projectDir.resolve("build/resolvedArtifacts.txt").readText()

        assertThat(resolved)
            .contains("io.qameta.allure:allure-testng:2.33.0:allure-testng-2.33.0.jar")
            .doesNotContain("spi-off")
    }
}
