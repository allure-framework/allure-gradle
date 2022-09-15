package io.qameta.allure.gradle.report

import io.qameta.allure.gradle.base.AllureBasePlugin
import io.qameta.allure.gradle.base.AllureExtension
import io.qameta.allure.gradle.base.dsl.extensions
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*

/**
 * The plugin adds [ALLURE_RAW_RESULT_ELEMENTS_CONFIGURATION_NAME] configuration so the project
 * can share raw Allure results for aggregation.
 */
open class AllureReportBasePlugin : Plugin<Project> {
    companion object {
        const val PLUGIN_NAME = "io.qameta.allure-report-base"
    }

    override fun apply(target: Project): Unit = target.run {
        // reporting-base adds ReportingExtension to project so users can configure default report folder
        apply(plugin = "reporting-base")

        apply<AllureBasePlugin>()
        the<AllureExtension>().extensions.create<AllureReportExtension>(
            AllureReportExtension.NAME,
            project
        )
    }
}
