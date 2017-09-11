package io.qameta.allure.gradle;

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
public class SingleReportTest {

    @Parameterized.Parameter(0)
    public String version;

    @Parameterized.Parameter(1)
    public String project;

    @Parameterized.Parameter(2)
    public String[] tasks;

    @Rule
    public GradleRunnerRule gradleRunner = new GradleRunnerRule()
            .version(() -> version)
            .project(() -> project)
            .tasks(() -> tasks);

    @Parameterized.Parameters(name = "{1} [{0}]")
    public static Collection<Object[]> getFrameworks() {
        return Arrays.asList(
                new Object[]{"3.5", "src/it/test-finalized-by-report", new String[]{"test"}},
                new Object[]{"3.5", "src/it/report-task", new String[]{"allureReport"}},
                new Object[]{"4.0", "src/it/report-task", new String[]{"allureReport"}}
        );
    }

    @Test
    public void shouldGenerateAllureReport() {
        BuildResult buildResult = gradleRunner.getBuildResult();
        File resultsDir = new File(gradleRunner.getProjectDir(), "build/allure-results");

        assertThat(buildResult.getTasks()).as("Download allure task status")
                .filteredOn(task -> task.getPath().equals(":downloadAllure"))
                .extracting("outcome")
                .containsExactly(SUCCESS);

        assertThat(resultsDir.listFiles()).as("Allure executor info")
                .filteredOn(file -> file.getName().endsWith("executor.json"))
                .hasSize(1);

        File projectDir = gradleRunner.getProjectDir();
        File reportDir = new File(projectDir, "build/reports/allure-report");
        assertThat(reportDir).as("Allure report directory")
                .exists();

        File testCasesDir = new File(reportDir, "data/test-cases");
        assertThat(testCasesDir.listFiles()).as("Allure test cases directory")
                .isNotEmpty();

    }
}
