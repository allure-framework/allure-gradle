package io.qameta.allure.gradle.report

import io.qameta.allure.gradle.base.metadata.AllureResultType
import io.qameta.allure.gradle.download.AllureDownloadPlugin
import io.qameta.allure.gradle.download.tasks.DownloadAllure
import io.qameta.allure.gradle.report.AllureAggregateReportPlugin.Companion.REPORT_TASK_NAME
import io.qameta.allure.gradle.report.AllureAggregateReportPlugin.Companion.SERVE_TASK_NAME
import io.qameta.allure.gradle.report.AllureReportPlugin.Companion.REPORT_TASK_NAME
import io.qameta.allure.gradle.report.AllureReportPlugin.Companion.SERVE_TASK_NAME
import io.qameta.allure.gradle.report.tasks.AllureReport
import io.qameta.allure.gradle.report.tasks.AllureServe
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.gradle.language.base.plugins.LifecycleBasePlugin

/**
 * The plugin adds tasks to build Allure reports for the current project ([REPORT_TASK_NAME] and [SERVE_TASK_NAME]).
 */
open class AllureReportPlugin : Plugin<Project> {
    companion object {
        const val PLUGIN_NAME = "io.qameta.allure-report"
        const val AGGREGATE_CONFIGURATION = "allureReport"
        const val REPORT_TASK_NAME = "allureReport"
        const val SERVE_TASK_NAME = "allureServe"
    }

    override fun apply(target: Project): Unit = target.run {
        apply<AllureReportBasePlugin>()

        registerReportTasks(
            AGGREGATE_CONFIGURATION,
            REPORT_TASK_NAME,
            SERVE_TASK_NAME,
            listOf(project)
        )
    }
}

/**
 * The plugin adds tasks to aggregate Allure reports for the current project and its subprojects (
 * [REPORT_TASK_NAME] and [SERVE_TASK_NAME]).
 * Note: if you need to collect the data from the current project, then you need [AllureAdapterPlugin.PLUGIN_NAME]
 */
open class AllureAggregateReportPlugin : Plugin<Project> {
    companion object {
        const val PLUGIN_NAME = "io.qameta.allure-aggregate-report"
        const val AGGREGATE_CONFIGURATION = "allureAggregateReport"
        const val REPORT_TASK_NAME = "allureAggregateReport"
        const val SERVE_TASK_NAME = "allureAggregateServe"
    }

    override fun apply(target: Project): Unit = target.run {
        apply<AllureReportBasePlugin>()

        registerReportTasks(
            AGGREGATE_CONFIGURATION,
            REPORT_TASK_NAME,
            SERVE_TASK_NAME,
            allprojects
        )
    }
}

internal fun Project.registerReportTasks(
    aggConfigurationName: String,
    reportTaskName: String,
    serveTaskName: String,
    projects: Iterable<Project>
) {
    val allureAggregate = configurations.create(aggConfigurationName) {
        description = "Contains all the projects for aggregating Allure results"
        isCanBeConsumed = false
        isCanBeResolved = true
        attributes {
            attribute(AllureResultType.ATTRIBUTE, AllureResultType.RAW)
        }
    }

    val allureGenerateCategories by configurations.creating {
        extendsFrom(allureAggregate)

        description = "Contains all the projects for aggregating Allure results"
        isCanBeConsumed = false
        isCanBeResolved = true
        attributes {
            attribute(AllureResultType.ATTRIBUTE, @Suppress("deprecation") AllureResultType.COPY_CATEGORIES)
        }
    }

    // It would be better to declare these dependencies via allureAggregate.defaultDependencies
    // However, it defeats Gradle's task dependency tracking: https://github.com/gradle/gradle/issues/16910
    dependencies {
        for (p in projects) {
            allureAggregate(create(p))
        }
    }

    // Ensure download task exists
    apply<AllureDownloadPlugin>()
    val download = tasks.named<DownloadAllure>(AllureDownloadPlugin.ALLURE_DOWNLOAD_TASK_NAME)

    tasks.register<AllureReport>(reportTaskName) {
        description = "Builds Allure report from $aggConfigurationName dependencies"
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        dependsOn(download)
        // This dependency ensures categories.json files are copied by the relevant copyCategories task
        // It enables users to update cagetories.json file in src/..., launch allureReport
        // and see the improved report without running the tests again
        dependsOn(allureGenerateCategories)
        allureHome.set(download.flatMap { it.destinationDir })
        // allureAggregate uses defaultDependencies, and Gradle seems to miss task dependencies there
        resultsDirs.set(allureAggregate)
    }
    tasks.register<AllureServe>(serveTaskName) {
        description = "Builds Allure report from $aggConfigurationName dependencies and launches Allure server"
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        dependsOn(download)
        // This dependency ensures categories.json files are copied by the relevant copyCategories task
        // It enables users to update cagetories.json file in src/..., launch allureReport
        // and see the improved report without running the tests again
        dependsOn(allureGenerateCategories)
        allureHome.set(download.flatMap { it.destinationDir })
        // allureAggregate uses defaultDependencies, and Gradle seems to miss task dependencies there
        resultsDirs.set(allureAggregate)
    }
}
