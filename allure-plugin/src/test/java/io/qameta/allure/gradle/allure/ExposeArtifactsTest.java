package io.qameta.allure.gradle.allure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collection;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import io.qameta.allure.gradle.rule.GradleRunnerRule;

@RunWith(Parameterized.class)
public class ExposeArtifactsTest {
    @Parameterized.Parameter(0)
    public String version;

    @Rule
    public GradleRunnerRule gradleRunner = new GradleRunnerRule()
            .version(() -> version)
            .project("src/it/expose-artifacts")
            .tasks("testCodeCoverageReport", "--dry-run");

    @Parameterized.Parameters(name = "{0}")
    public static Collection<String> getFrameworks() {
        return Arrays.asList("9.0.0", "8.14.3", "8.11.1");
    }

    @Test
    public void allureExposesElements() {
        BuildResult buildResult = gradleRunner.getBuildResult();

        assertThat(buildResult.getOutput())
                .contains("BUILD SUCCESSFUL");
    }

}
