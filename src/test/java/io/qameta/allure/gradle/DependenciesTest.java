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
import java.util.stream.Collectors;

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

    private static final String[][] IT_MATRIX = {
        { "src/it/cucumber-jvm",         "3.5", "4.0",        "5.0" },
        { "src/it/junit4",               "3.5", "4.0",        "5.0" },
        { "src/it/junit4-autoconfigure", "3.5", "4.0",        "5.0" },
        { "src/it/junit4-kotlin",                             "5.0", "5.1" },
        { "src/it/junit5",               "3.5", "4.0",        "5.0" },
        { "src/it/testng",               "3.5", "4.0",        "5.0" },
        { "src/it/testng-autoconfigure", "3.5", "4.0",        "5.0" },
        { "src/it/spock",                "3.5", "4.0",        "5.0" },
        { "src/it/junit5-native",                      "4.6", "5.0" },
    };

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
        return Arrays.stream(IT_MATRIX)
                .flatMap(it -> Arrays.stream(it).skip(1).map(version -> new Object[] { version, it[0] }))
                .collect(Collectors.toList());
    }

    @Test
    public void shouldCreateAllureResults() {
        BuildResult buildResult = gradleRunner.getBuildResult();
        assertThat(buildResult.getTasks()).as("Gradle build tasks statuses")
                .filteredOn(task -> task.getPath().equals(":test"))
                .extracting("outcome")
                .containsExactly(SUCCESS);

        File resultsDir = new File(gradleRunner.getProjectDir(), "build/allure-results");
        assertThat(resultsDir).as("Allure results directory")
                .exists();

        assertThat(resultsDir.listFiles()).as("Allure results test cases")
                .filteredOn(file -> file.getName().endsWith("result.json"))
                .hasSize(1);

        assumeThat("not implemented", project, not(containsString("spock")));
        assumeThat("not implemented", project, not(containsString("junit5")));

        assertThat(resultsDir.listFiles()).as("Allure results attachments")
                .filteredOn(file -> file.getName().endsWith("attachment"))
                .hasSize(1);
    }

}
