package io.qameta.allure.gradle.report

import io.qameta.allure.gradle.util.conv
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.reporting.ReportingExtension
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.the
import javax.inject.Inject

open class AllureReportExtension @Inject constructor(
    private val project: Project,
    objects: ObjectFactory
) {
    companion object {
        const val NAME = "report"
    }

    /**
     * Base directory for Allure report.
     * Note: since the same project can have both aggregate and regular reports,
     * the actual reports would be placed to `$reportDir/allureReport` and `$reportDir/allureAggregateReport`
     * folders.
     */
    val reportDir: DirectoryProperty = objects.directoryProperty().apply {
        conv(project.the<ReportingExtension>().baseDirectory.dir("allure-report"))
    }

    /**
     * By default, `allureReport` and `allureServe` tasks do not execute tests since it might be time-consuming.
     * However, in case tests are up-to-date or from-cache, it might be a nice idea to follow Gradle's default
     * approach: "the task should (re)build all its prerequisites", so the user could edit the source file,
     * launch `allureReport` and get the updated report with all the tests updated.
     */
    val dependsOnTests: Property<Boolean> = objects.property<Boolean>().conv(
        project.provider {
            false
        }
    )

    /**
     * By default, `allureReport` and `allureServe` tasks do not execute tests since it might be time-consuming.
     * However, in case tests are up-to-date or from-cache, it might be a nice idea to follow Gradle's default
     * approach: "the task should (re)build all its prerequisites", so the user could edit the source file,
     * launch `allureReport` and get the updated report with all the tests updated.
     */
    fun dependsOnTests() {
        dependsOnTests.set(true)
    }
}
