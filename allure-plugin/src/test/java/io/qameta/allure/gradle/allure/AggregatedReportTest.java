package io.qameta.allure.gradle.allure;

import io.qameta.allure.gradle.rule.GradleRunnerRule;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class AggregatedReportTest {

    @TempDir
    File tempDir;

    static Collection<org.junit.jupiter.params.provider.Arguments> getFrameworks() {
        return List.of(
                arguments("9.4.1", "src/it/report-multi", new String[]{"allureAggregateReport"}),
                arguments("8.14.3", "src/it/report-multi", new String[]{"allureAggregateReport"}),
                arguments("8.11.1", "src/it/report-multi", new String[]{"allureAggregateReport"})
        );
    }

    @ParameterizedTest(name = "{1} [{0}]")
    @MethodSource("getFrameworks")
    void shouldGenerateAllureReport(String version, String project, String[] tasks) {
        GradleRunnerRule gradleRunner = new GradleRunnerRule()
                .rootDir(tempDir)
                .version(version)
                .project(project)
                .tasks(tasks)
                .build();

        BuildResult buildResult = gradleRunner.getBuildResult();

        assertThat(buildResult.getTasks()).as("Download allure task status")
                .filteredOn(task -> task.getPath().equals(":downloadAllure"))
                .extracting("outcome")
                .containsExactly(SUCCESS);

        File projectDir = gradleRunner.getProjectDir();
        File customResultsDir = new File(projectDir, "module1/build/custom-allure-results");
        assertThat(customResultsDir).as("Custom adapter Allure results directory")
                .isNotEmptyDirectory();
        File defaultResultsDir = new File(projectDir, "module2/build/allure-results");
        assertThat(defaultResultsDir).as("Default adapter Allure results directory")
                .isNotEmptyDirectory();
        File manualResultsDir = new File(projectDir, "module3/build/manual-allure-results");
        assertThat(manualResultsDir).as("Manually published Allure results directory")
                .isNotEmptyDirectory();

        File reportDir = new File(projectDir, "build/reports/allure-report/allureAggregateReport");
        assertThat(reportDir).as("Allure report directory")
                .exists();

        File testCasesDir = new File(reportDir, "data/test-cases");
        assertThat(testCasesDir.listFiles()).as("Allure test cases directory")
                .hasSize(3);

    }
}
