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

/**
 * Verifies that plugin properly adds `spi-off` dependency (the one without META-INF/services)
 * when {@link io.qameta.allure.gradle.adapter.config.AdapterConfig#getAutoconfigureListeners} is {@code false}.
 */
@RunWith(Parameterized.class)
public class TestNgSpiOffTest {
    @Parameterized.Parameter(0)
    public String version;

    @Rule
    public GradleRunnerRule gradleRunner = new GradleRunnerRule()
            .version(() -> version)
            .project("src/it/testng-spi-off")
            .tasks("test");

    @Parameterized.Parameters(name = "{0}")
    public static Collection<String> getFrameworks() {
        return Arrays.asList("9.0.0", "8.14.3", "8.11.1");
    }

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
        // executor.json is always generated
        assertThat(resultsDir.listFiles())
                .filteredOn(file -> !file.getName().equals("executor.json"))
                .isEmpty();
    }
}
