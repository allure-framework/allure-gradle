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

public class TestNgSpiOffTest {

    @TempDir
    File tempDir;

    static Collection<String> getFrameworks() {
        return List.of("9.4.1", "8.14.3", "8.11.1");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getFrameworks")
    void allureReportIsNotGenerated(String version) {
        GradleRunnerRule gradleRunner = new GradleRunnerRule()
                .rootDir(tempDir)
                .version(version)
                .project("src/it/testng-spi-off")
                .tasks("test")
                .build();

        BuildResult buildResult = gradleRunner.getBuildResult();
        File projectDir = gradleRunner.getProjectDir();

        assertThat(buildResult.getTasks())
                .as("Build task generateAllureReport should fail silently if no report is generated")
                .filteredOn(task -> task.getPath().equals(":test") || task.getPath().equals(":allureReport"))
                .extracting("outcome")
                .containsExactly(SUCCESS);
        File resultsDir = new File(projectDir.getAbsolutePath() + "/build/allure-results");
        assertThat(resultsDir.listFiles())
                .filteredOn(file -> !file.getName().equals("executor.json"))
                .isEmpty();
    }
}
