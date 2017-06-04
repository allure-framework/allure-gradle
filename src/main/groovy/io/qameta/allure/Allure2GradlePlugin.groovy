package io.qameta.allure

import groovy.transform.CompileStatic
import io.qameta.allure.adapters.CucumberJVMAdapter
import io.qameta.allure.adapters.JUnit4Adapter
import io.qameta.allure.adapters.TestNGAdapter
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.internal.tasks.testing.junit.JUnitTestFramework
import org.gradle.api.internal.tasks.testing.testng.TestNGTestFramework
import org.gradle.api.tasks.testing.Test
import org.gradle.util.ConfigureUtil

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
@CompileStatic
class Allure2GradlePlugin implements Plugin<Project> {

    private static final String CONFIGURATION_ASPECTJWEAVER = "aspectjweaverAgent"
    private static final String ALLURE_DIR_PROPERTY = "allure.results.directory"
    private static final String JUNIT4_ASPECT_DEPENDENCY = "io.qameta.allure:allure-junit4-aspect:"
    private static final String JUNIT4 = "JUnit4"

    private static final Map<String, String> ADAPTER_DEPENDENCIES =
            ["TestNG"     : "io.qameta.allure:allure-testng:",
             "JUnit4"     : "io.qameta.allure:allure-junit4:",
             "CucumberJVM": "io.qameta.allure:allure-cucumber-jvm:"]

    private static final Map<String, Class> TEST_FRAMEWORKS =
            ["TestNG": TestNGTestFramework.class,
             "JUnit4": JUnitTestFramework.class]

    private Project project

    @Override
    void apply(Project project) {
        this.project = project
        AdaptersExtension adapters = getAdaptersExtension(project)
        AllureReportExtension reportExtension = getReportExtension(project)

        project.afterEvaluate {
            if (adapters.autoconfigure) {
                autoconfigure(adapters)
            }
            applyAdapters(adapters)
            applyAspectjweaver(adapters)
            configureTestTasks(adapters)

            if (reportExtension?.version) {
                project.evaluationDependsOnChildren()
                project.tasks.create(DownloadAllureTask.NAME, DownloadAllureTask.class)
                project.tasks.create(AllureTask.NAME, AllureTask.class)
                configureAllureDownload(reportExtension)
                configureReportTask(reportExtension)
            }
        }

    }

    private static AllureReportExtension getReportExtension(Project project) {
        project.extensions.create(AllureReportExtension.NAME, AllureReportExtension.class, project)
    }

    private static AdaptersExtension getAdaptersExtension(Project project) {
        project.extensions.create(AdaptersExtension.NAME, AdaptersExtension.class, project)
    }

    private void autoconfigure(AdaptersExtension extension) {
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

    private applyAdapters(AdaptersExtension ext) {
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

    private void addAdapterDependency(AdaptersExtension extension, String name, String version, boolean spiOff) {
        String dependencyString = ADAPTER_DEPENDENCIES[name] + version
        if (spiOff) {
            dependencyString += ":spi-off"
        }
        project.dependencies.add(extension.configuration, dependencyString)
    }

    private void configureTestTasks(AdaptersExtension ext) {
        project.tasks.withType(Test.class).each {
            it.outputs.files project.files(project.file(ext.resultsFolder))
            it.systemProperty(ALLURE_DIR_PROPERTY, ext.resultsFolder)
        }
    }

    private void applyAspectjweaver(AdaptersExtension ext) {
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

    private void configureAllureDownload(AllureReportExtension extension) {
        DownloadAllureTask task = project.getTasks().withType(DownloadAllureTask.class)
                .getByName(DownloadAllureTask.NAME)
        task.allureVersion = extension.version
        task.allureCliDest = project.file(project.buildDir.absolutePath + "/allure-${extension.version}")
        task.downloadAllureLink = extension.downloadLink ?: String.format(extension.downloadLinkFormat,
                extension.version)
    }

    private void configureReportTask(AllureReportExtension extension) {
        AllureTask task = project.getTasks().withType(AllureTask.class).getByName(AllureTask.NAME)
        task.resultsDirs = extension.resultsDirs
        task.resultsGlob = extension.resultsGlob
        task.allureVersion = extension.version
        task.clean = extension.clean
        task.outputDir = extension.reportDir
    }
}
