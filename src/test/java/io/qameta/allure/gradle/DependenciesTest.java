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
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assume.assumeThat;


/**
 * eroshenkoam
 * 20.06.17
 */
@RunWith(Parameterized.class)
public class DependenciesTest {

    @Parameterized.Parameter(0)
    public String version;

    @Parameterized.Parameter(1)
    public String project;

    @Rule
    public GradleRunnerRule gradleRunner = new GradleRunnerRule()
            .version(() -> version)
            .project(() -> project)
            .tasks("test");

    @Parameterized.Parameters(name = "{1} [{0}]")
    public static Collection<Object[]> getFrameworks() {
        return Arrays.asList(
                new Object[]{"3.5", "src/it/cucumber-jvm"},
                new Object[]{"4.0", "src/it/cucumber-jvm"},
                new Object[]{"3.5", "src/it/junit4"},
                new Object[]{"4.0", "src/it/junit4"},
                new Object[]{"3.5", "src/it/junit4-autoconfigure"},
                new Object[]{"4.0", "src/it/junit4-autoconfigure"},
                new Object[]{"3.5", "src/it/testng"},
                new Object[]{"4.0", "src/it/testng"},
                new Object[]{"3.5", "src/it/testng-autoconfigure"},
                new Object[]{"4.0", "src/it/testng-autoconfigure"},
                new Object[]{"3.5", "src/it/spock"},
                new Object[]{"4.0", "src/it/spock"}

        );
    }

    @Test
    public void shouldCreateAllureResults() {
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

        assumeThat("not implemented", project, not(containsString("spock")));
        assertThat(resultsDir.listFiles()).as("Allure results attachments")
                .filteredOn(file -> file.getName().endsWith("attachment"))
                .hasSize(1);
    }

}
