package io.qameta.allure.gradle

import groovy.transform.CompileStatic
import io.qameta.allure.gradle.task.AllureReport
import io.qameta.allure.gradle.task.AllureServe
import io.qameta.allure.gradle.task.DownloadAllure
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.tasks.testing.junit.JUnitTestFramework
import org.gradle.api.internal.tasks.testing.testng.TestNGTestFramework
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.testing.Test
import org.gradle.process.JavaForkOptions

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
@CompileStatic
class AllurePlugin implements Plugin<Project> {

    private static final String CONFIGURATION_ASPECTJ_WEAVER = 'aspectjWeaverAgent'
    private static final String ALLURE_DIR_PROPERTY = 'allure.results.directory'
    private static final String JUNIT4_ASPECT_DEPENDENCY = 'io.qameta.allure:allure-junit4-aspect:'
    private static final String JUNIT4 = 'JUnit4'

    // @formatter:off
    private static final Map<String, String> ADAPTER_DEPENDENCIES =
            [
                    'TestNG': 'io.qameta.allure:allure-testng:',
                    'JUnit4': 'io.qameta.allure:allure-junit4:',
                    'JUnit5': 'io.qameta.allure:allure-junit5:',
                    'Spock': 'io.qameta.allure:allure-spock:',
                    'CucumberJVM': 'io.qameta.allure:allure-cucumber-jvm:',
                    'Cucumber2JVM': 'io.qameta.allure:allure-cucumber2-jvm:',
            ]
    // @formatter:on

    // @formatter:off
    private static final Map<String, Class> TEST_FRAMEWORKS =
            [
                    'TestNG': TestNGTestFramework,
                    'JUnit4': JUnitTestFramework,
            ]
    // @formatter:on

    private Project project

    @Override
    void apply(Project project) {
        this.project = project
        AllureExtension extension = project.extensions.create(AllureExtension.NAME, AllureExtension, project)

        project.afterEvaluate {
            if (extension.autoconfigure) {
                autoconfigure(extension)
            }
            applyAdapters(extension)
            applyAspectjweaver(extension)

            configureTestTasks(extension)

            if (extension?.version) {
                project.evaluationDependsOnChildren()
                project.tasks.create(AllureServe.NAME, AllureServe)
                project.tasks.create(AllureReport.NAME, AllureReport)
                project.tasks.create(DownloadAllure.NAME, DownloadAllure)
            }
        }
    }

    private void autoconfigure(AllureExtension extension) {
        TEST_FRAMEWORKS.each { name, framework ->
            boolean apply = project.tasks.withType(Test).any { Test task -> framework.isInstance(task.testFramework) }
            if (apply) {
                project.logger.debug("Allure autoconfiguration: $name test framework found")
                String dependencyString = ADAPTER_DEPENDENCIES[name] + extension.allureJavaVersion
                project.dependencies.add(extension.configuration, dependencyString)
                if (name == JUNIT4) {
                    String aspectDependencyString = JUNIT4_ASPECT_DEPENDENCY + extension.allureJavaVersion
                    project.dependencies.add(extension.configuration, aspectDependencyString)
                }
            }
        }
    }

    private applyAdapters(AllureExtension ext) {
        if (ext.testNGConfig) {
            addAdapterDependency(ext, ext.testNGConfig.name, ext.testNGConfig.version, ext.testNGConfig.spiOff)
        }
        if (ext.junit4Config) {
            addAdapterDependency(ext, ext.junit4Config.name, ext.junit4Config.version, false)
            project.dependencies.add(ext.configuration, JUNIT4_ASPECT_DEPENDENCY + ext.junit4Config.version)
        }
        if (ext.junit5Config) {
            addAdapterDependency(ext, ext.junit5Config.name, ext.junit5Config.version, false)
        }
        if (ext.cucumberJVMConfig) {
            addAdapterDependency(ext, ext.cucumberJVMConfig.name, ext.cucumberJVMConfig.version, false)
        }
        if (ext.cucumber2JVMConfig) {
            addAdapterDependency(ext, ext.cucumber2JVMConfig.name, ext.cucumber2JVMConfig.version, false)
        }
        if (ext.spockConfig) {
            addAdapterDependency(ext, ext.spockConfig.name, ext.spockConfig.version, false)
        }
    }

    private void addAdapterDependency(AllureExtension extension, String name, String version, boolean spiOff) {
        String dependencyString = ADAPTER_DEPENDENCIES[name] + version
        if (spiOff) {
            dependencyString += ':spi-off'
        }
        project.dependencies.add(extension.configuration, dependencyString)
    }

    private void configureTestTasks(AllureExtension ext) {
        configureTestTasks { Task task, JavaForkOptions test ->
            task.outputs.dir(ext.resultsDir)
            test.systemProperty(ALLURE_DIR_PROPERTY, ext.resultsDir)
        }
    }

    private void applyAspectjweaver(AllureExtension ext) {
        if (ext.aspectjweaver || ext.autoconfigure) {
            Configuration aspectjConfiguration = project.configurations.maybeCreate(CONFIGURATION_ASPECTJ_WEAVER)

            project.dependencies.add(CONFIGURATION_ASPECTJ_WEAVER, "org.aspectj:aspectjweaver:${ext.aspectjVersion}")

            String javaAgent = "-javaagent:${aspectjConfiguration.singleFile}"

            configureTestTasks { Task task, JavaForkOptions test ->
                task.doFirst {
                    test.jvmArgs = [javaAgent] + test.jvmArgs as Iterable
                }
                if (project.logger.debugEnabled) {
                    project.logger.debug "jvmArgs for task $task.name $test.jvmArgs"
                }
            }
        }
    }

    private void configureTestTasks(Closure closure) {
        [project.tasks.withType(Test), junitPlatformPluginTestTasks()].flatten().each { closure(it, it) }
    }

    private Collection<JavaExec> junitPlatformPluginTestTasks() {
        project.tasks.withType(JavaExec).findAll { JavaExec task -> task.name == 'junitPlatformTest' }
    }
}
