package io.qameta.allure.gradle;

import io.qameta.allure.gradle.rule.GradleRunnerRule;
import org.gradle.testkit.runner.BuildResult;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;

public class ReportOnlyTest {

    @Rule
    public GradleRunnerRule gradleRunner = new GradleRunnerRule()
            .version("5.0")
            .project("src/it/report-only")
            .tasks("allureReport");

    @Test
    public void allureReportGenerated() {
        BuildResult buildResult = gradleRunner.getBuildResult();

        File projectDir = gradleRunner.getProjectDir();

        assertThat(buildResult.getTasks())
                .as("allureReport task should work in projects without sourceSets")
                .extracting("outcome")
                .containsOnly(SUCCESS);

        File resultsDir = new File(projectDir.getAbsolutePath() + "/build/allure-results");
        assertThat(resultsDir.list()).isNotEmpty();
    }
}
