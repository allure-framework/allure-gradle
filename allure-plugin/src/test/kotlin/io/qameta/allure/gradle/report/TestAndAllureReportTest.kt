package io.qameta.allure.gradle.report

import io.qameta.allure.gradle.rule.GradleRunnerRule
import org.assertj.core.api.Assertions
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import org.junit.jupiter.params.provider.Arguments.arguments

class TestAndAllureReportTest {
    @TempDir
    lateinit var tempDir: File

    companion object {
        @JvmStatic
        fun getFrameworks() = buildList {
            for (gradleVersion in listOf("9.4.1", "8.14.3", "8.11.1")) {
                for (project in listOf("src/it/junit5-5.8.1")) {
                    for (dependsOnTests in listOf(true, false)) {
                        add(arguments(gradleVersion, project, dependsOnTests))
                    }
                }
            }
        }
    }

    @ParameterizedTest(name = "{1},  [{0}]")
    @MethodSource("getFrameworks")
    fun `check allureReport outcome`(version: String, project: String, dependsOnTests: Boolean) {
        val gradleRunner = GradleRunnerRule()
            .rootDir(tempDir)
            .version(version)
            .project(project)
            .tasks(
                *if (dependsOnTests) {
                    arrayOf("allureReport", "--depends-on-tests")
                } else {
                    arrayOf("allureReport")
                }
            )
            .build()

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
