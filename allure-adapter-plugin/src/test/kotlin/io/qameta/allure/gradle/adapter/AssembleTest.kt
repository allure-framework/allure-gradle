package io.qameta.allure.gradle.adapter

import io.qameta.allure.gradle.rule.GradleRunnerRule
import org.assertj.core.api.Assertions
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class AssembleTest {
    @Rule
    @JvmField
    val gradleRunner = GradleRunnerRule()
        .version { version }
        .project { project }
        .tasks { tasks }

    @Parameterized.Parameter(0)
    lateinit var version: String

    @Parameterized.Parameter(1)
    lateinit var project: String

    @Parameterized.Parameter(2)
    lateinit var tasks: Array<String>

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{1} [{0}]")
        fun getFrameworks() = listOf(
            arrayOf(
                "7.0",
                "src/it/adapter-assemble",
                arrayOf("assemble")
            ),
            arrayOf(
                "6.0",
                "src/it/adapter-assemble",
                arrayOf("assemble")
            )
        )
    }

    @Test
    fun `assemble should not execute tests`() {
        Assertions.assertThat(gradleRunner.buildResult.tasks)
            .`as`("assemble should succeed, and test must not be executed")
            .filteredOn { task -> task.path == ":assemble" }
            .extracting("outcome")
            .containsAnyOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE)
    }
}
