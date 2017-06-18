package io.qameta.allure.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

import static io.qameta.allure.gradle.util.TestUtil.copyDataFiles
import static io.qameta.allure.gradle.util.TestUtil.prepareClasspathFile
import static org.assertj.core.api.Assertions.assertThat
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
class TestNGSpiOffTest {

    private static String DATA_DIR = 'testng-spi-off'

    private BuildResult buildResult

    private File testProjectDirectory

    @BeforeClass
    void prepareBuild() {
        testProjectDirectory = copyDataFiles(DATA_DIR)
        List<File> pluginClasspath = prepareClasspathFile()

        buildResult = GradleRunner.create()
                .withProjectDir(testProjectDirectory)
                .withTestKitDir(new File(testProjectDirectory.parentFile.absolutePath, '.gradle'))
                .withPluginClasspath(pluginClasspath)
                .withArguments('test')
                .build()
    }

    @Test
    void allureReportIsNotGenerated() {
        assertThat(buildResult.tasks)
                .as('Build task generateAllureReport should fail silently if no report is generated')
                .filteredOn({ task -> task.path in [':test'] })
                .extracting('outcome')
                .containsExactly(SUCCESS)
        File resultsDir = new File(testProjectDirectory.absolutePath + '/build/allure-results')
        assertThat(resultsDir.list()).isNull()
    }
}
