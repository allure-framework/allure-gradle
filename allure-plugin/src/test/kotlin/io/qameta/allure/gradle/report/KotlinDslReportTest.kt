package io.qameta.allure.gradle.report

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class KotlinDslReportTest {
    @Rule
    @JvmField
    val tempDir = TemporaryFolder()

    @Parameterized.Parameter
    lateinit var version: String

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "[{0}]")
        fun versions() = listOf(
            arrayOf("9.0.0"),
            arrayOf("8.14.3"),
            arrayOf("8.11.1"),
        )
    }

    @Test
    fun `allureReport should reuse results from a previous Kotlin DSL test run`() {
        val projectDir = tempDir.newFolder("junit4-kotlin")
        File("src/it/junit4-kotlin").copyRecursively(projectDir, overwrite = true)
        projectDir.resolve("settings.gradle.kts").createNewFile()

        val testResult = runner(projectDir).withArguments(commonArgs("test")).build()
        assertThat(testResult.task(":test")?.outcome)
            .`as`("test task outcome")
            .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(projectDir.resolve("build/allure-results"))
            .`as`("Allure results directory after test run")
            .isNotEmptyDirectory()

        val reportResult = runner(projectDir).withArguments(commonArgs("allureReport")).build()
        assertThat(reportResult.task(":allureReport")?.outcome)
            .`as`("allureReport should not become NO-SOURCE for Kotlin DSL projects")
            .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(projectDir.resolve("build/reports/allure-report/allureReport"))
            .`as`("Generated Allure report")
            .isNotEmptyDirectory()
    }

    private fun runner(projectDir: File) = GradleRunner.create()
        .withProjectDir(projectDir)
        .withGradleVersion(version)
        .withPluginClasspath()
        .withTestKitDir(projectDir.resolve(".gradle-testkit"))
        .forwardOutput()

    private fun commonArgs(vararg tasks: String) = listOf(
        "--stacktrace",
        "--info",
        "-Porg.gradle.daemon=false",
        "--no-watch-fs",
        *tasks
    )
}
