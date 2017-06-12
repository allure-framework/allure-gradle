package io.qameta.allure.gradle

import org.gradle.api.Task
import org.gradle.api.reporting.ConfigurableReport
import org.gradle.api.reporting.DirectoryReport
import org.gradle.api.reporting.Report
import org.gradle.api.reporting.internal.TaskGeneratedSingleDirectoryReport
import org.gradle.api.reporting.internal.TaskReportContainer

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
class AllureReportContainer extends TaskReportContainer<Report> {

    AllureReportContainer(Task task) {
        super(ConfigurableReport.class, task)
        add(TaskGeneratedSingleDirectoryReport.class, 'allure-report', task, 'index.html')
    }
}
