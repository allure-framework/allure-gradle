package io.qameta.allure.gradle;

import io.qameta.allure.gradle.util.TestUtil;
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

public class TestNgSpiOffTest {

    private BuildResult buildResult;

    private File testProjectDirectory;

    @BeforeClass
    public void prepareBuild() throws IOException {
        testProjectDirectory = copyProject("testng-spi-off");
        List<File> pluginClasspath = readPluginClasspath();

        buildResult = GradleRunner.create()
                .withProjectDir(testProjectDirectory)
                .withTestKitDir(new File(testProjectDirectory.getParentFile().getAbsolutePath(), ".gradle"))
                .withPluginClasspath(pluginClasspath)
                .withArguments("test")
                .build();
    }

    @Test
    public void allureReportIsNotGenerated() {
        assertThat(buildResult.getTasks())
                .as("Build task generateAllureReport should fail silently if no report is generated")
                .filteredOn(task -> task.getPath().equals(":test") || task.getPath().equals(":allureReport"))
                .extracting("outcome")
                .containsExactly(SUCCESS);
        File resultsDir = new File(testProjectDirectory.getAbsolutePath() + "/build/allure-results");
        assertThat(resultsDir.list()).isNull();
    }
}
