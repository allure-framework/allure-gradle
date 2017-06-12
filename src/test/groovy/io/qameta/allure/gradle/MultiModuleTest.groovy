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
class MultiModuleTest {

    private static String DATA_DIR = 'multi-module'

    private BuildResult buildResult

    private File testProjectDirectory

    @BeforeClass
    void prepareBuild() {
        testProjectDirectory = copyDataFiles(DATA_DIR)
        List<File> pluginClasspath = prepareClasspathFile()

        buildResult = GradleRunner.create()
                .withProjectDir(testProjectDirectory)
                .withTestKitDir(new File(testProjectDirectory.parentFile.absolutePath, '.gradle'))
                .withArguments('test', 'allureAggregatedReport')
                .withPluginClasspath(pluginClasspath)
                .build()
    }

    @Test
    void tasksAreSuccessfullyInvoked() {
        assertThat(buildResult.tasks)
                .as('Build task test and allureReport should be successfully executed')
                .filteredOn({task -> task.path in [':module1:test', ':module2:test', ':downloadAllure',
                                                   ':allureAggregatedReport']})
                .extracting('outcome')
                .containsExactly(SUCCESS, SUCCESS, SUCCESS, SUCCESS)
    }

    @Test
    void reportIsGenerated() {
        File reportDir = new File(testProjectDirectory.absolutePath + '/build/reports/allure-report')
        assertThat(reportDir.exists()).as('allure-report directory has not been generated')
        assertThat(reportDir.listFiles().toList()).as('allure-report directory should not be empty')
                .isNotEmpty()
    }
}
