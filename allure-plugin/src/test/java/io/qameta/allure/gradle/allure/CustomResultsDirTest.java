package io.qameta.allure.gradle.allure;

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
public class CustomResultsDirTest {
    @Parameterized.Parameter
    public String version;

    @Rule
    public GradleRunnerRule gradleRunner = new GradleRunnerRule()
            .version(() -> version)
            .project("src/it/custom-results-dir")
            .tasks("test", "allureReport");

    @Parameterized.Parameters(name = "{0}")
    public static Collection<String> getVersions() {
        return Arrays.asList("9.0.0", "8.14.3", "8.11.1");
    }

    @Test
    public void customResultsDirIsPublishedByAdapterAndUsedByReportTask() {
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
