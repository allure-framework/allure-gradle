package io.qameta.allure.gradle.report

import io.qameta.allure.gradle.rule.GradleRunnerRule
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class DslTest {
    @Rule
    @JvmField
    val gradleRunner = GradleRunnerRule()
        .version { version }
        .project { project }
        .tasks("testDsl")


    @Parameterized.Parameter(0)
    lateinit var version: String

    @Parameterized.Parameter(1)
    lateinit var project: String

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{1} [{0}]")
        fun getFrameworks() = listOf(
            arrayOf("7.0", "src/it/full-dsl-kotlin"),
            arrayOf("6.0", "src/it/full-dsl-kotlin"),
            arrayOf("5.0", "src/it/full-dsl-kotlin"),
            arrayOf("7.0", "src/it/full-dsl-groovy"),
            arrayOf("6.0", "src/it/full-dsl-groovy"),
            arrayOf("5.0", "src/it/full-dsl-groovy")
        )
    }

    @Test
    fun `build script should compile`() {
        assertThat(gradleRunner.buildResult.tasks).`as`("testDsl task status")
            .filteredOn { task -> task.path == ":testDsl" }
            .extracting("outcome")
            .containsExactly(TaskOutcome.SUCCESS)
    }
}
