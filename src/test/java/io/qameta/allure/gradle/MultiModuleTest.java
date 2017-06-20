package io.qameta.allure.gradle;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static io.qameta.allure.gradle.util.TestUtil.copyProject;
import static io.qameta.allure.gradle.util.TestUtil.readPluginClasspath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;

public class MultiModuleTest {

    private static BuildResult buildResult;

    private static File testProjectDirectory;

    @BeforeClass
    public static void prepareBuild() throws IOException {
        testProjectDirectory = copyProject("multi-module");
        List<File> pluginClasspath = readPluginClasspath();

        buildResult = GradleRunner.create()
                .withProjectDir(testProjectDirectory)
                .withTestKitDir(new File(testProjectDirectory.getParentFile().getAbsolutePath(), ".gradle"))
                .withArguments("test", "allureAggregatedReport")
                .withPluginClasspath(pluginClasspath)
                .build();
    }

    @Test
    public void tasksAreSuccessfullyInvoked() {
        assertThat(buildResult.getTasks())
                .as("Build task test and allureReport should be successfully executed")
                .filteredOn(task -> task.getPath().equals(":module1:test") || task.getPath().equals(":module2:test")
                        || task.getPath().equals(":downloadAllure") || task.getPath().equals(":allureAggregatedReport"))
                .extracting("outcome")
                .containsExactly(SUCCESS, SUCCESS, SUCCESS, SUCCESS);
    }

    @Test
    public void reportIsGenerated() {
        File reportDir = new File(testProjectDirectory.getAbsolutePath() + "/build/reports/allure-report");
        assertThat(reportDir.exists()).as("allure-report directory has not been generated");
        assertThat(reportDir.listFiles()).as("allure-report directory should not be empty")
                .isNotEmpty();
    }
}
