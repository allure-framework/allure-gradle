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

public class ExposeArtifactsTest {

    @TempDir
    File tempDir;

    static Collection<String> getFrameworks() {
        return List.of("9.0.0", "8.14.3", "8.11.1");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getFrameworks")
    void allureExposesElements(String version) {
        GradleRunnerRule gradleRunner = new GradleRunnerRule()
                .rootDir(tempDir)
                .version(version)
                .project("src/it/expose-artifacts")
                .tasks("testCodeCoverageReport", "--dry-run")
                .build();

        BuildResult buildResult = gradleRunner.getBuildResult();

        assertThat(buildResult.getOutput())
                .contains("BUILD SUCCESSFUL");
    }

}
