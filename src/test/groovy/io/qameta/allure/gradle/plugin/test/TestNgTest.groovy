package io.qameta.allure.gradle.plugin.test

import groovy.transform.Memoized
import io.qameta.allure.gradle.plugin.test.extension.GradlePluginClasspath
import io.qameta.allure.gradle.plugin.test.extension.TestProjectDir
import org.gradle.testkit.runner.GradleRunner
import spock.lang.Shared
import spock.lang.Specification

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
class TestNgTest extends Specification {

    @TestProjectDir(dir = "testng")
    @Shared
    File testProjectDirectory

    @GradlePluginClasspath
    @Shared
    List<File> pluginClasspath

    @Memoized
    def getBuildResult() {
        GradleRunner.create()
                .withProjectDir(testProjectDirectory)
                .withTestKitDir(new File(testProjectDirectory.parentFile.absolutePath, '.gradle'))
                .withArguments('test', 'allureReport', '--debug', '--stacktrace')
                .withPluginClasspath(pluginClasspath)
                .build()
    }

    def 'task successfully executed'() {
        expect:
        buildResult.task(":test").outcome == SUCCESS
    }

}
