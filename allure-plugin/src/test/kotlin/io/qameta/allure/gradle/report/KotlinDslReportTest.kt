package io.qameta.allure.gradle.report

import io.qameta.allure.gradle.rule.GradleRunnerRule
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File

class KotlinDslReportTest {
    @TempDir
    lateinit var tempDir: File

    companion object {
        @JvmStatic
        fun versions() = listOf("9.0.0", "8.14.3", "8.11.1")
    }

    @ParameterizedTest(name = "[{0}]")
    @MethodSource("versions")
    fun `allureReport should reuse results from a previous Kotlin DSL test run`(version: String) {
        val projectDir = File(tempDir, "junit4-kotlin-$version").apply { mkdirs() }
        File("src/it/junit4-kotlin").copyRecursively(projectDir, overwrite = true)
        projectDir.resolve("settings.gradle.kts").createNewFile()

        val testResult = runner(projectDir, version).withArguments(commonArgs("test")).build()
        assertThat(testResult.task(":test")?.outcome)
            .`as`("test task outcome")
            .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(projectDir.resolve("build/allure-results"))
            .`as`("Allure results directory after test run")
            .isNotEmptyDirectory()

        val reportResult = runner(projectDir, version).withArguments(commonArgs("allureReport")).build()
        assertThat(reportResult.task(":allureReport")?.outcome)
            .`as`("allureReport should not become NO-SOURCE for Kotlin DSL projects")
            .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(projectDir.resolve("build/reports/allure-report/allureReport"))
            .`as`("Generated Allure report")
            .isNotEmptyDirectory()
    }

    private fun runner(projectDir: File, version: String) = GradleRunner.create()
        .withProjectDir(projectDir)
        .withGradleVersion(version)
        .withPluginClasspath()
        .withTestKitDir(GradleRunnerRule.testKitDirFor(projectDir))
        .forwardOutput()

    private fun commonArgs(vararg tasks: String) = listOf(
        "--stacktrace",
        "--info",
        "-Porg.gradle.daemon=false",
        "--no-watch-fs",
        *tasks
    )
}
