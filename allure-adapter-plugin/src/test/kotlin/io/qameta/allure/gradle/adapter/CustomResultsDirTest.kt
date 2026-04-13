package io.qameta.allure.gradle.adapter

import io.qameta.allure.gradle.rule.GradleRunnerRule
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class CustomResultsDirTest {
    @Rule
    @JvmField
    val gradleRunner = GradleRunnerRule()
        .version { version }
        .project("src/it/adapter-custom-results-dir")
        .tasks("test")

    @Parameterized.Parameter
    lateinit var version: String

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun versions() = listOf("9.0.0", "8.14.3", "8.11.1")
    }

    @Test
    fun `adapter plugin writes raw results to custom directory`() {
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
