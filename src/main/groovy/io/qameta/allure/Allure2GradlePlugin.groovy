package io.qameta.allure

import groovy.transform.CompileStatic
import io.qameta.allure.adapters.CucumberJVMAdapter
import io.qameta.allure.adapters.JUnit4Adapter
import io.qameta.allure.adapters.TestNGAdapter
import io.qameta.allure.tasks.AggregatedAllureReportTask
import io.qameta.allure.tasks.AllureReportTask
import io.qameta.allure.tasks.AllureServeTask
import io.qameta.allure.tasks.DownloadAllureTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.tasks.testing.junit.JUnitTestFramework
import org.gradle.api.internal.tasks.testing.testng.TestNGTestFramework
import org.gradle.api.tasks.testing.Test
import org.gradle.util.ConfigureUtil

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
@CompileStatic
class Allure2GradlePlugin implements Plugin<Project> {

    private static final String CONFIGURATION_ASPECTJWEAVER = 'aspectjweaverAgent'
    private static final String ALLURE_DIR_PROPERTY = 'allure.results.directory'
    private static final String JUNIT4_ASPECT_DEPENDENCY = 'io.qameta.allure:allure-junit4-aspect:'
    private static final String JUNIT4 = 'JUnit4'

    private static final Map<String, String> ADAPTER_DEPENDENCIES =
            ['TestNG'     : 'io.qameta.allure:allure-testng:',
             'JUnit4'     : 'io.qameta.allure:allure-junit4:',
             'CucumberJVM': 'io.qameta.allure:allure-cucumber-jvm:']

    private static final Map<String, Class> TEST_FRAMEWORKS =
            ['TestNG': TestNGTestFramework.class,
             'JUnit4': JUnitTestFramework.class]

    private Project project

    @Override
    void apply(Project project) {
        this.project = project
        AllureExtension extension = getAdaptersExtension(project)

        project.afterEvaluate {
            if (extension.autoconfigure) {
                autoconfigure(extension)
            }
            applyAdapters(extension)
            applyAspectjweaver(extension)
            configureTestTasks(extension)

            if (extension?.version) {
                project.evaluationDependsOnChildren()
                project.tasks.create(DownloadAllureTask.NAME, DownloadAllureTask.class)
                project.tasks.create(AggregatedAllureReportTask.NAME, AggregatedAllureReportTask.class)
                project.tasks.create(AllureServeTask.NAME, AllureServeTask.class)
                project.tasks.create(AllureReportTask.NAME, AllureReportTask.class)
                configureAllureDownload(extension)
                configureAggregatedReportTask(extension)
                configureAllureReportTask(extension)
                configureAllureServeTask(extension)
            }
        }
    }

    private static AllureExtension getAdaptersExtension(Project project) {
        project.extensions.create(AllureExtension.NAME, AllureExtension.class, project)
    }

    private void autoconfigure(AllureExtension extension) {
        TEST_FRAMEWORKS.each { name, framework ->
            boolean apply = project.tasks.withType(Test.class)
                    .any { task -> framework.isInstance(task.getTestFramework()) }
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
        if (ext.useTestNG) {
            TestNGAdapter testNGConfig = ConfigureUtil.configure(ext.useTestNG, new TestNGAdapter())
            addAdapterDependency(ext, testNGConfig.name, testNGConfig.adapterVersion, testNGConfig.spiOff)
        }
        if (ext.useJUnit4) {
            JUnit4Adapter junit4Config = ConfigureUtil.configure(ext.useJUnit4, new JUnit4Adapter())
            addAdapterDependency(ext, junit4Config.name, junit4Config.adapterVersion, false)
            project.dependencies.add(ext.configuration, JUNIT4_ASPECT_DEPENDENCY + junit4Config.adapterVersion)
        }
        if (ext.useCucumberJVM) {
            CucumberJVMAdapter cucumberConfig = ConfigureUtil.configure(ext.useCucumberJVM, new CucumberJVMAdapter())
            addAdapterDependency(ext, cucumberConfig.name, cucumberConfig.adapterVersion, false)
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
        project.tasks.withType(Test.class).each {
            it.outputs.files project.files(project.file(ext.resultsDirectory))
            it.systemProperty(ALLURE_DIR_PROPERTY, ext.resultsDirectory)
        }
    }

    private void applyAspectjweaver(AllureExtension ext) {
        if (ext.aspectjweaver || ext.autoconfigure) {
            def aspectjConfiguration = project.configurations.maybeCreate(
                    CONFIGURATION_ASPECTJWEAVER)

            project.dependencies.add(CONFIGURATION_ASPECTJWEAVER,
                    "org.aspectj:aspectjweaver:${ext.aspectjVersion}")

            project.tasks.withType(Test).each { test ->
                test.doFirst {
                    String javaAgent = "-javaagent:${aspectjConfiguration.singleFile}"
                    test.jvmArgs = [javaAgent] + test.jvmArgs
                }
                if (project.logger.debugEnabled) {
                    project.logger.debug "jvmArgs for task $test.name $test.jvmArgs"
                }
            }
        }
    }

    private void configureAllureDownload(AllureExtension extension) {
        DownloadAllureTask task = project.getTasks().withType(DownloadAllureTask.class)
                .getByName(DownloadAllureTask.NAME)
        task.allureVersion = extension.version
        File allureDest = new File(project.rootDir, '.allure')
        allureDest.mkdir()
        task.allureCliDest = project.file(allureDest.absolutePath)
        task.downloadAllureLink = extension.downloadLink ?: String.format(extension.downloadLinkFormat,
                extension.version)
    }

    private void configureAggregatedReportTask(AllureExtension extension) {
        AggregatedAllureReportTask task = project.getTasks().withType(AggregatedAllureReportTask.class)
                .getByName(AggregatedAllureReportTask.NAME)
        task.resultsDirs = extension.resultsDirectories
        task.resultsGlob = extension.resultsGlob
        task.allureVersion = extension.version
        task.clean = extension.clean
        task.outputDir = new File(extension.reportDir)
    }

    private void configureAllureReportTask(AllureExtension extension) {
        AllureReportTask task = project.getTasks().withType(AllureReportTask.class)
                .getByName(AllureReportTask.NAME)
        File resultsDir = new File(extension.resultsDirectory)
        resultsDir.mkdirs() //due to @InputFolder check, folder should exist
        task.resultsDir = resultsDir
        task.allureVersion = extension.version
        task.outputDir = new File(extension.reportDir)
        task.clean = extension.clean
    }

    private void configureAllureServeTask(AllureExtension extension) {
        AllureServeTask task = project.getTasks().withType(AllureServeTask.class)
                .getByName(AllureServeTask.NAME)
        task.allureVersion = extension.version
        task.resultsDir = new File(extension.resultsDirectory)
    }
}
