package io.qameta.allure.gradle.allure;

import io.qameta.allure.gradle.rule.GradleTestVersion;
import io.qameta.allure.gradle.rule.GradleRunnerRule;
import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class DependenciesTest {

    private static final List<String> IT_PROJECTS = List.of(
            "src/it/cucumber7-jvm",
            "src/it/junit4",
            "src/it/junit4-autoconfigure",
            "src/it/junit4-kotlin",
            "src/it/junit5",
            "src/it/junit5-5.8.1",
            "src/it/testng",
            "src/it/testng-autoconfigure",
            "src/it/spock"
    );

    @TempDir
    File tempDir;

    static Collection<org.junit.jupiter.params.provider.Arguments> getFrameworks() {
        String gradleVersion = GradleTestVersion.current();
        return IT_PROJECTS.stream()
                .map(project -> arguments(gradleVersion, project))
                .collect(Collectors.toList());
    }

    @ParameterizedTest(name = "{1} [{0}]")
    @MethodSource("getFrameworks")
    void shouldCreateAllureResults(String version, String project) {
        GradleRunnerRule gradleRunner = new GradleRunnerRule()
                .rootDir(tempDir)
                .version(version)
                .project(project)
                .tasks("test")
                .build();

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

        assertThat(resultsDir.listFiles()).as("Allure results attachments")
                .filteredOn(file -> file.getName().endsWith("attachment"))
                .hasSize(1);
    }
}
