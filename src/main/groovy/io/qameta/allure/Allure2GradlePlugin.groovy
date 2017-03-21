package io.qameta.allure

import org.gradle.api.Plugin
import org.gradle.api.Project
import groovy.transform.CompileStatic
import org.gradle.api.tasks.testing.Test

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
@CompileStatic
class Allure2GradlePlugin implements Plugin<Project> {

    private static final String CONFIGURATION_ASPECTJWEAVER = "aspectjweaverAgent"
    private static final String ALLURE_DIR_PROPERTY = "allure.results.directory"
    private static final String ALLURE_CONFIGURATION_NAME = "allure"


    Project project

    @Override
    void apply(Project project) {
        this.project = project
        project.configurations.create(ALLURE_CONFIGURATION_NAME)
        AllureExtension ext = configureAllureExtension(project)

        project.afterEvaluate {
            applyTestNgAdaptor(ext)
            configureAllureDownload(ext)
            configureAllureTask(ext)
            configureTestTasks(ext)
            applyAspectjweaver(ext)
        }
        project.tasks.create(DownloadAllureTask.NAME, DownloadAllureTask.class)
        project.tasks.create(AllureTask.NAME, AllureTask.class)
    }

    private static AllureExtension configureAllureExtension(Project project) {
        project.extensions.create(AllureExtension.NAME, AllureExtension.class, project)
    }

    private void configureAllureDownload(AllureExtension extension) {
        DownloadAllureTask task = project.getTasks().withType(DownloadAllureTask.class)
                .getByName(DownloadAllureTask.NAME)
        task.allureVersion = extension.allureVersion
        task.allureCliDest = project.file(project.buildDir.absolutePath + "/allure-${extension.allureVersion}")
        task.downloadAllureLink = String.format(extension.downloadLinkFormat, extension.allureVersion)
    }

    private void configureAllureTask(AllureExtension extension) {
        AllureTask task = project.getTasks().withType(AllureTask.class)
                .getByName(AllureTask.NAME)
        task.allureVersion = extension.allureVersion
        task.inputDir = new File(extension.allureResultsDir)
        task.outputDir = new File(extension.allureReportDir)
    }

    private void applyTestNgAdaptor(AllureExtension extension) {
        if (extension.testNG) {
            project.dependencies.add(extension.configuration,
                    "io.qameta.allure:allure-testng:$extension.testNGAdapterVersion")
        }
    }

    private void configureTestTasks(AllureExtension ext) {
        project.tasks.withType(Test.class).each {
            it.outputs.files project.files(project.file(ext.allureResultsDir))
            it.systemProperty(ALLURE_DIR_PROPERTY, ext.allureResultsDir)
        }
    }

    private void applyAspectjweaver(AllureExtension ext) {
        if (ext.aspectjweaver) {
            project.dependencies.add(
                    ext.configuration,
                    "io.qameta.allure:allure-java-commons:${ext.testNGAdapterVersion}")

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
}
