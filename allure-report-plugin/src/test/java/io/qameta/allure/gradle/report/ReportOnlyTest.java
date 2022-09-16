package io.qameta.allure.gradle.report;

import io.qameta.allure.gradle.rule.GradleRunnerRule;
import org.gradle.testkit.runner.BuildResult;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;

@RunWith(Parameterized.class)
public class ReportOnlyTest {
    @Parameterized.Parameter(0)
    public String project;

    @Parameterized.Parameter(1)
    public String version;

    @Rule
    public GradleRunnerRule gradleRunner = new GradleRunnerRule()
            .version(() -> version)
            .project(() -> project)
            .tasks("allureReport");

    @Parameterized.Parameters(name = "{0} [{1}]")
    public static Collection<Object[]> getFrameworks() {
        return Arrays.asList(
                new Object[]{"src/it/report-only", "7.5.1"},
                new Object[]{"src/it/report-only", "7.0"},
                new Object[]{"src/it/report-only", "6.0"}
        );
    }

    @Test
    public void allureReportGenerated() {
        BuildResult buildResult = gradleRunner.getBuildResult();

        File projectDir = gradleRunner.getProjectDir();

        assertThat(buildResult.getTasks())
                .as("allureReport task should work in projects without sourceSets")
                .extracting("outcome")
                .containsOnly(SUCCESS);

        File resultsDir = new File(projectDir.getAbsolutePath() + "/build/reports/allure-report/allureReport");
        assertThat(resultsDir).as("folder with Allure reports")
                .isNotEmptyDirectory();
    }
}
