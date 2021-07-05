package io.qameta.allure.gradle.allure

import io.qameta.allure.gradle.gather.AllureGatherPlugin
import io.qameta.allure.gradle.report.AllureReportPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

/**
 * This is a shortcut for [AllureGatherPlugin.PLUGIN_NAME] and [AllureReportPlugin.PLUGIN_NAME] plugins.
 * If you need an aggregate report, then use [AllureAggregateReportPlugin.PLUGIN_NAME] plugin.
 */
open class AllurePlugin : Plugin<Project> {
    companion object {
        const val PLUGIN_NAME = "io.qameta.allure"
    }

    override fun apply(target: Project): Unit = target.run {
        apply<AllureGatherPlugin>()
        apply<AllureReportPlugin>()
    }
}
