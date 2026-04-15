package io.qameta.allure.gradle.report;

import io.qameta.allure.gradle.rule.GradleRunnerRule;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class ReportOnlyTest {

    @TempDir
    File tempDir;

    static Collection<org.junit.jupiter.params.provider.Arguments> getFrameworks() {
        return List.of(
                arguments("src/it/report-only", "9.0.0"),
                arguments("src/it/report-only", "8.14.3"),
                arguments("src/it/report-only", "8.11.1")
        );
    }

    @ParameterizedTest(name = "{0} [{1}]")
    @MethodSource("getFrameworks")
    void allureReportGenerated(String project, String version) {
        GradleRunnerRule gradleRunner = new GradleRunnerRule()
                .rootDir(tempDir)
                .version(version)
                .project(project)
                .tasks("allureReport")
                .build();

        BuildResult buildResult = gradleRunner.getBuildResult();

        File projectDir = gradleRunner.getProjectDir();
        File rawResultsDir = new File(projectDir, "build/manual-allure-results");
        assertThat(rawResultsDir).as("Manually registered Allure results")
                .isNotEmptyDirectory();

        assertThat(buildResult.getTasks())
                .as("allureReport task should work in projects without sourceSets")
                .extracting("outcome")
                .containsOnly(SUCCESS);

        File resultsDir = new File(projectDir.getAbsolutePath() + "/build/reports/allure-report/allureReport");
        assertThat(resultsDir).as("folder with Allure reports")
                .isNotEmptyDirectory();
    }
}
