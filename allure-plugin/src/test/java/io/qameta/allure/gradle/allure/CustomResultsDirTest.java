package io.qameta.allure.gradle.allure;

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

public class CustomResultsDirTest {

    @TempDir
    File tempDir;

    static Collection<String> getVersions() {
        return List.of("9.0.0", "8.14.3", "8.11.1");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getVersions")
    void customResultsDirIsPublishedByAdapterAndUsedByReportTask(String version) {
        GradleRunnerRule gradleRunner = new GradleRunnerRule()
                .rootDir(tempDir)
                .version(version)
                .project("src/it/custom-results-dir")
                .tasks("test", "allureReport")
                .build();

        BuildResult buildResult = gradleRunner.getBuildResult();

        assertThat(buildResult.getTasks()).as("Gradle build task statuses")
                .filteredOn(task -> task.getPath().equals(":test") || task.getPath().equals(":allureReport"))
                .extracting("outcome")
                .containsExactly(SUCCESS, SUCCESS);

        File projectDir = gradleRunner.getProjectDir();
        File defaultResultsDir = new File(projectDir, "build/allure-results");
        assertThat(defaultResultsDir).as("Default Allure results directory")
                .doesNotExist();

        File customResultsDir = new File(projectDir, "build/custom-allure-results");
        assertThat(customResultsDir).as("Custom Allure results directory")
                .isNotEmptyDirectory();
        assertThat(customResultsDir.listFiles()).as("Allure results test cases")
                .filteredOn(file -> file.getName().endsWith("result.json"))
                .hasSize(1);

        File reportDir = new File(projectDir, "build/reports/allure-report/allureReport");
        assertThat(reportDir).as("Generated Allure report")
                .isNotEmptyDirectory();
    }
}
