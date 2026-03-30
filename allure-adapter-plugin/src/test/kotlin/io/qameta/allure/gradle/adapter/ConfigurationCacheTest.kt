package io.qameta.allure.gradle.adapter

import io.qameta.allure.gradle.rule.GradleRunnerRule
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ConfigurationCacheTest {
    @Rule
    @JvmField
    val gradleRunner = GradleRunnerRule()
        .version { version }
        .project { "src/it/adapter-cache-junit5-kts" }
        .tasks { arrayOf("test", "--configuration-cache") }

    @Parameterized.Parameter(0)
    lateinit var version: String

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "configuration cache on {0}")
        fun data() = listOf(
            arrayOf("9.0.0"),
            arrayOf("8.14.3"),
            arrayOf("8.11.1")
        )
    }

    @Test
    fun `test task can reuse configuration cache when allure adapter is applied`() {
        val firstRun = gradleRunner.buildResult
        assertThat(firstRun.output)
            .contains("Configuration cache entry stored.")

        val projectDir = gradleRunner.projectDir
        val testKitHome = projectDir.parentFile.resolve(".gradle")

        val secondRun = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments(
                "--stacktrace",
                "--info",
                "-Porg.gradle.daemon=false",
                "--no-watch-fs",
                "test",
                "--configuration-cache"
            )
            .withGradleVersion(version)
            .withTestKitDir(testKitHome)
            .withPluginClasspath()
            .forwardOutput()
            .build()

        assertThat(secondRun.output)
            .containsPattern("(Reusing configuration cache\\.|Configuration cache entry reused\\.)")
    }
}
