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


/**
 * eroshenkoam
 * 20.06.17
 */
@RunWith(Parameterized.class)
public class FrameworkIntegrationTest {

    @Parameterized.Parameter
    public String project;

    @Rule
    public GradleRunnerRule gradleRunner = new GradleRunnerRule(() -> project, "test");

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getFrameworks() {
        return Arrays.asList(
                new Object[]{"src/it/cucumber-jvm"},
                new Object[]{"src/it/junit4"},
                new Object[]{"src/it/junit4-autoconfigure"},
                new Object[]{"src/it/testng"},
                new Object[]{"src/it/testng-autoconfigure"}
        );
    }

    @Test
    public void shouldGenerateAllureResults() {
        BuildResult buildResult = gradleRunner.getBuildResult();
        assertThat(buildResult.getTasks()).as("Gradle build tasks statuses")
                .filteredOn(task -> task.getPath().equals(":test"))
                .extracting("outcome")
                .containsExactly(SUCCESS);

        File projectDir = gradleRunner.getProjectDir();
        File resultsDir = new File(projectDir, "build/allure-results");
        assertThat(resultsDir).as("Allure results directory")
                .exists();

        assertThat(resultsDir.listFiles()).as("Allure results test cases")
                .filteredOn(file -> file.getName().endsWith("result.json"))
                .hasSize(1);

        assertThat(resultsDir.listFiles()).as("Allure results attachments")
                .filteredOn(file -> file.getName().endsWith("attachment"))
                .hasSize(1);
    }

}
