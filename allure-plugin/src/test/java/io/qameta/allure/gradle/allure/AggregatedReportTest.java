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
public class AggregatedReportTest {

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
                new Object[]{"7.5.1", "src/it/report-multi", new String[]{"allureAggregateReport"}},
                new Object[]{"7.0", "src/it/report-multi", new String[]{"allureAggregateReport"}},
                new Object[]{"6.0", "src/it/report-multi", new String[]{"allureAggregateReport"}}
        );
    }

    @Test
    public void shouldGenerateAllureReport() {
        BuildResult buildResult = gradleRunner.getBuildResult();

        assertThat(buildResult.getTasks()).as("Download allure task status")
                .filteredOn(task -> task.getPath().equals(":downloadAllure"))
                .extracting("outcome")
                .containsExactly(SUCCESS);

        File projectDir = gradleRunner.getProjectDir();
        File reportDir = new File(projectDir, "build/reports/allure-report/allureAggregateReport");
        assertThat(reportDir).as("Allure report directory")
                .exists();

        File testCasesDir = new File(reportDir, "data/test-cases");
        assertThat(testCasesDir.listFiles()).as("Allure test cases directory")
                .isNotEmpty();

    }
}
