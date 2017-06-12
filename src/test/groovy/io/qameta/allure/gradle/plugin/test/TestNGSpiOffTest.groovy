package io.qameta.allure.gradle.plugin.test

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

import static io.qameta.allure.gradle.plugin.test.util.TestUtil.copyDataFiles
import static io.qameta.allure.gradle.plugin.test.util.TestUtil.prepareClasspathFile
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
                .withArguments('test', 'allure')
                .withPluginClasspath(pluginClasspath)
                .build()
    }

    @Test
    void allureReportIsNotGenerated() {
        assertThat(buildResult.tasks)
                .as('Build tasks generateAllureReport should fail silently if no report is generated')
                .filteredOn({ task -> task.path in [':test', ':allure'] })
                .extracting('outcome')
                .containsExactly(SUCCESS, SUCCESS)
        File reportDir = new File(testProjectDirectory.absolutePath + '/build/reports/allure-report')
        assertThat(reportDir.list().toList()).isEmpty()
    }
}
