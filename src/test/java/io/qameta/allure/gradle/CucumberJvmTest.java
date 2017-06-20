package io.qameta.allure.gradle;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static io.qameta.allure.gradle.util.TestUtil.copyProject;
import static io.qameta.allure.gradle.util.TestUtil.readPluginClasspath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;

public class CucumberJvmTest {

    private BuildResult buildResult;

    private File testProjectDirectory;

    @BeforeClass
    public void prepareBuild() throws IOException {
        testProjectDirectory = copyProject("cucumberjvm");
        List<File> pluginClasspath = readPluginClasspath();

        buildResult = GradleRunner.create()
                .withProjectDir(testProjectDirectory)
                .withTestKitDir(new File(testProjectDirectory.getParentFile().getAbsolutePath(), ".gradle"))
                .withArguments("test", "allureReport")
                .withPluginClasspath(pluginClasspath)
                .build();
    }

    @Test
    public void tasksAreSuccessfullyInvoked() {
        assertThat(buildResult.getTasks())
                .as("Build task test and allureReport should be successfully executed")
                .filteredOn(task -> task.getPath().equals(":test") || task.getPath().equals(":allureReport"))
                .extracting("outcome")
                .containsExactly(SUCCESS, SUCCESS);
    }

    @Test
    public void reportIsGenerated() {
        File reportDir = new File(testProjectDirectory.getAbsolutePath() + "/build/reports/allure-report");
        assertThat(reportDir.exists()).as("allure-report directory has not been generated");
        assertThat(reportDir.listFiles()).as("allure-report directory should not be empty")
                .isNotEmpty();
    }

    @Test
    public void attachmentsAreProcessed() {
        File reportDir = new File(testProjectDirectory.getAbsolutePath() + "/build/reports/allure-report");
        assertThat(reportDir.exists()).as("allure-report directory has not been generated");
        File attachmentsDir = new File(reportDir.getAbsolutePath(), "/data/attachments");
        assertThat(attachmentsDir.listFiles())
                .as("Attachments have not been processed")
                .hasSize(1);
    }
}

