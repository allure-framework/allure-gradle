package io.qameta.allure.gradle.report

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class AllurePluginFeatureMatrixTest {
    @Rule
    @JvmField
    val tempDir = TemporaryFolder()

    @Parameterized.Parameter
    lateinit var runtime: TestAllureRuntime

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun runtimes() = listOf(
            arrayOf(TestAllureRuntime.ALLURE_2),
            arrayOf(TestAllureRuntime.ALLURE_3),
        )
    }

    @Test
    fun `categories are copied for both runtimes`() {
        val projectDir = AllureRuntimeMatrixSupport.copyFixture(tempDir, "src/it/categories")
        AllureRuntimeMatrixSupport.configureRuntime(projectDir, runtime, usesReportRuntime = true)

        val buildResult = AllureRuntimeMatrixSupport.runner(projectDir)
            .withArguments(AllureRuntimeMatrixSupport.commonArgs("allureReport"))
            .build()

        AllureRuntimeMatrixSupport.assertRuntimeTasks(buildResult, runtime)
        assertThat(buildResult.task(":allureReport")?.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(projectDir.resolve("build/allure-results").listFiles().orEmpty().map { it.name })
            .filteredOn { it.endsWith("categories.json") }
            .hasSize(1)
        assertThat(projectDir.resolve("build/reports/allure-report/allureReport"))
            .isNotEmptyDirectory()
    }

    @Test
    fun `custom results dir is reused by report task for both runtimes`() {
        val projectDir = AllureRuntimeMatrixSupport.copyFixture(tempDir, "src/it/custom-results-dir")
        AllureRuntimeMatrixSupport.configureRuntime(projectDir, runtime, usesReportRuntime = true)

        val buildResult = AllureRuntimeMatrixSupport.runner(projectDir)
            .withArguments(AllureRuntimeMatrixSupport.commonArgs("test", "allureReport"))
            .build()

        AllureRuntimeMatrixSupport.assertRuntimeTasks(buildResult, runtime)
        assertThat(buildResult.task(":test")?.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(buildResult.task(":allureReport")?.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(projectDir.resolve("build/allure-results"))
            .doesNotExist()
        assertThat(projectDir.resolve("build/custom-allure-results"))
            .isNotEmptyDirectory()
        assertThat(projectDir.resolve("build/reports/allure-report/allureReport"))
            .isNotEmptyDirectory()
    }

    @Test
    fun `allureReport respects depends-on-tests for both runtimes`() {
        val projectDir = AllureRuntimeMatrixSupport.copyFixture(tempDir, "src/it/junit5-5.8.1")
        AllureRuntimeMatrixSupport.configureRuntime(projectDir, runtime, usesReportRuntime = true)

        val buildResult = AllureRuntimeMatrixSupport.runner(projectDir)
            .withArguments(AllureRuntimeMatrixSupport.commonArgs("allureReport", "--depends-on-tests"))
            .build()

        AllureRuntimeMatrixSupport.assertRuntimeTasks(buildResult, runtime)
        assertThat(buildResult.task(":test")?.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(buildResult.task(":allureReport")?.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(projectDir.resolve("build/reports/allure-report/allureReport"))
            .isNotEmptyDirectory()
    }

    @Test
    fun `allureReport stays no-source without depends-on-tests for both runtimes`() {
        val projectDir = AllureRuntimeMatrixSupport.copyFixture(tempDir, "src/it/junit5-5.8.1")
        AllureRuntimeMatrixSupport.configureRuntime(projectDir, runtime, usesReportRuntime = true)

        val buildResult = AllureRuntimeMatrixSupport.runner(projectDir)
            .withArguments(AllureRuntimeMatrixSupport.commonArgs("allureReport"))
            .build()

        AllureRuntimeMatrixSupport.assertRuntimeTasks(buildResult, runtime)
        assertThat(buildResult.task(":test"))
            .isNull()
        assertThat(buildResult.task(":allureReport")?.outcome)
            .isEqualTo(TaskOutcome.NO_SOURCE)
    }

    @Test
    fun `allureReport reuses results from previous Kotlin DSL test run for both runtimes`() {
        val projectDir = AllureRuntimeMatrixSupport.copyFixture(tempDir, "src/it/junit4-kotlin")
        AllureRuntimeMatrixSupport.configureRuntime(projectDir, runtime, usesReportRuntime = true)

        val testResult = AllureRuntimeMatrixSupport.runner(projectDir)
            .withArguments(AllureRuntimeMatrixSupport.commonArgs("test"))
            .build()
        assertThat(testResult.task(":test")?.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(projectDir.resolve("build/allure-results"))
            .isNotEmptyDirectory()

        val reportResult = AllureRuntimeMatrixSupport.runner(projectDir)
            .withArguments(AllureRuntimeMatrixSupport.commonArgs("allureReport"))
            .build()

        AllureRuntimeMatrixSupport.assertRuntimeTasks(reportResult, runtime)
        assertThat(reportResult.task(":allureReport")?.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(projectDir.resolve("build/reports/allure-report/allureReport"))
            .isNotEmptyDirectory()
    }

    @Test
    fun `full DSL scripts compile for both runtimes`() {
        listOf("src/it/full-dsl-kotlin", "src/it/full-dsl-groovy").forEach { fixture ->
            val projectDir = AllureRuntimeMatrixSupport.copyFixture(tempDir, fixture)
            AllureRuntimeMatrixSupport.configureRuntime(
                projectDir = projectDir,
                runtime = runtime,
                stripLegacyCommandlineDsl = runtime == TestAllureRuntime.ALLURE_3,
            )

            val buildResult = AllureRuntimeMatrixSupport.runner(projectDir)
                .withArguments(AllureRuntimeMatrixSupport.commonArgs("testDsl"))
                .build()

            assertThat(buildResult.task(":testDsl")?.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        }
    }

    @Test
    fun `testng spi-off keeps raw results empty for both runtimes`() {
        val projectDir = AllureRuntimeMatrixSupport.copyFixture(tempDir, "src/it/testng-spi-off")
        AllureRuntimeMatrixSupport.configureRuntime(projectDir, runtime)

        val buildResult = AllureRuntimeMatrixSupport.runner(projectDir)
            .withArguments(AllureRuntimeMatrixSupport.commonArgs("test"))
            .build()

        assertThat(buildResult.task(":test")?.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(projectDir.resolve("build/allure-results").listFiles().orEmpty())
            .filteredOn { it.name != "executor.json" }
            .isEmpty()
    }

    @Test
    fun `adapter exposes artifacts for both runtimes`() {
        val projectDir = AllureRuntimeMatrixSupport.copyFixture(tempDir, "src/it/expose-artifacts")
        AllureRuntimeMatrixSupport.configureRuntime(projectDir, runtime)

        val buildResult = AllureRuntimeMatrixSupport.runner(projectDir)
            .withArguments(AllureRuntimeMatrixSupport.commonArgs("testCodeCoverageReport", "--dry-run"))
            .build()

        assertThat(buildResult.output)
            .contains("BUILD SUCCESSFUL")
    }
}
