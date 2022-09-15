package io.qameta.allure.gradle.report

import io.qameta.allure.gradle.rule.GradleRunnerRule
import org.assertj.core.api.Assertions
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class TestAndAllureReportTest {
    @Rule
    @JvmField
    val gradleRunner = GradleRunnerRule()
        .version { version }
        .project { project }
        .tasks {
            if (dependsOnTests) {
                arrayOf("allureReport", "--depends-on-tests")
            } else {
                arrayOf("allureReport")
            }
        }

    @Parameterized.Parameter(0)
    lateinit var version: String

    @Parameterized.Parameter(1)
    lateinit var project: String

    @JvmField
    @Parameterized.Parameter(2)
    var dependsOnTests: Boolean = false

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{1},  [{0}]")
        fun getFrameworks(): Array<Any> {
            val res = mutableListOf<Array<Any>>()
            for (gradleVersion in listOf("7.5.1", "7.2", "7.0", "6.8.3")) {
                for (project in listOf("src/it/junit5-5.8.1")) {
                    for (dependOnTests in listOf(true, false)) {
                        res.add(arrayOf(gradleVersion, project, dependOnTests))
                    }
                }
            }
            return res.toTypedArray()
        }
    }

    @Test
    fun `check allureReport outcome`() {
        if (dependsOnTests) {
            Assertions.assertThat(gradleRunner.buildResult.tasks)
                .`as`("allureReport --depends-on-tests should trigger test execution")
                .filteredOn { task -> task.path == ":test" }
                .extracting("outcome")
                .containsExactly(TaskOutcome.SUCCESS)

            Assertions.assertThat(gradleRunner.buildResult.tasks)
                .`as`("allureReport --depends-on-tests should result in SUCCESS for :allureReport")
                .filteredOn { task -> task.path == ":allureReport" }
                .extracting("outcome")
                .containsExactly(TaskOutcome.SUCCESS)
        } else {
            Assertions.assertThat(gradleRunner.buildResult.tasks)
                .`as`("allureReport without --depends-on-tests should not trigger test execution")
                .filteredOn { task -> task.path == ":test" }
                .isEmpty()

            Assertions.assertThat(gradleRunner.buildResult.tasks)
                .`as`("allureReport without --depends-on-tests should result in NO-SOURCE for :allureReport")
                .filteredOn { task -> task.path == ":allureReport" }
                .extracting("outcome")
                .containsExactly(TaskOutcome.NO_SOURCE)
        }
    }
}
