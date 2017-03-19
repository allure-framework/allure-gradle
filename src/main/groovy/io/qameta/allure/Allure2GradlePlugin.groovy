package io.qameta.allure

import org.gradle.api.Plugin
import org.gradle.api.Project
import groovy.transform.CompileStatic

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
@CompileStatic
class Allure2GradlePlugin implements Plugin<Project> {

    Project project

    @Override
    void apply(Project project) {
        this.project = project
        project.configurations.create(DownloadAllureTask.CONFIGURATION_NAME)
        project.configurations.create(AllureTask.CONFIGURATION_NAME)
        AllureExtension ext = configureAllureExtension(project)
        applyTestNgAdaptor(ext)
        project.afterEvaluate {
            configureAllureDownload(ext)
            configureAllureTask(ext)
        }

        project.tasks.create(DownloadAllureTask.NAME, DownloadAllureTask)
        project.tasks.create(AllureTask.NAME, AllureTask)
    }

    private static AllureExtension configureAllureExtension(Project project) {
        project.extensions.create(AllureExtension.NAME, AllureExtension.class, project)
    }

    private void configureAllureDownload(AllureExtension extension) {
        DownloadAllureTask task = project.getTasks().withType(DownloadAllureTask.class)
                .getByName(DownloadAllureTask.NAME)
        task.allureVersion = extension.allureVersion
        task.allureCliDest = project.buildDir.absolutePath + "/allure-${extension.allureVersion}.zip"
    }

    private void configureAllureTask(AllureExtension extension) {
        AllureTask task = project.getTasks().withType(AllureTask.class)
                .getByName(AllureTask.NAME)
        task.allureVersion = extension.allureVersion
        task.inputDir = new File(extension.allureReportDir)
        task.outputDir = new File(extension.allureResultsDir)
    }

    private void applyTestNgAdaptor(AllureExtension extension) {
        if (extension.testNG) {
            project.dependencies.add(extension.configuration,
                    "io.qameta.allure:allure-testng:$extension.allureVersion")
        }
    }
}
