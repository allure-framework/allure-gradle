package io.qameta.allure.gradle;

import io.qameta.allure.gradle.rule.GradleRunnerRule;
import org.gradle.testkit.runner.BuildResult;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;

public class MultiModuleTest {

    @ClassRule
    public static GradleRunnerRule gradleRunner = new GradleRunnerRule("src/it/multi-module", "test");

    @Test
    public void tasksAreSuccessfullyInvoked() {
        BuildResult buildResult = gradleRunner.getBuildResult();

        assertThat(buildResult.getTasks())
                .as("Build task test and allureReport should be successfully executed")
                .filteredOn(task -> task.getPath().equals(":module1:test") || task.getPath().equals(":module2:test")
                        || task.getPath().equals(":downloadAllure") || task.getPath().equals(":allureAggregatedReport"))
                .extracting("outcome")
                .containsExactly(SUCCESS, SUCCESS, SUCCESS, SUCCESS);
    }

    @Test
    public void reportIsGenerated() {
        File projectDir = gradleRunner.getProjectDir();

        File reportDir = new File(projectDir.getAbsolutePath() + "/build/reports/allure-report");
        assertThat(reportDir.exists()).as("allure-report directory has not been generated");
        assertThat(reportDir.listFiles()).as("allure-report directory should not be empty")
                .isNotEmpty();
    }
}
