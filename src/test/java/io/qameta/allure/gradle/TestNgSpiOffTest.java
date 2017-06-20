package io.qameta.allure.gradle;

import io.qameta.allure.gradle.rule.GradleRunnerRule;
import org.gradle.testkit.runner.BuildResult;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;

public class TestNgSpiOffTest {

    @Rule
    public GradleRunnerRule gradleRunner = new GradleRunnerRule("src/it/testng-spi-off", "test");

    @Test
    public void allureReportIsNotGenerated() {
        BuildResult buildResult = gradleRunner.getBuildResult();

        File projectDir = gradleRunner.getProjectDir();

        assertThat(buildResult.getTasks())
                .as("Build task generateAllureReport should fail silently if no report is generated")
                .filteredOn(task -> task.getPath().equals(":test") || task.getPath().equals(":allureReport"))
                .extracting("outcome")
                .containsExactly(SUCCESS);
        File resultsDir = new File(projectDir.getAbsolutePath() + "/build/allure-results");
        assertThat(resultsDir.list()).isNull();
    }
}
