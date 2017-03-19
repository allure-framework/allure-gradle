package io.qameta.allure

import org.gradle.api.Task
import org.gradle.api.reporting.ConfigurableReport
import org.gradle.api.reporting.DirectoryReport
import org.gradle.api.reporting.Report
import org.gradle.api.reporting.internal.TaskGeneratedSingleDirectoryReport
import org.gradle.api.reporting.internal.TaskReportContainer

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
class AllureReportContainerImpl extends TaskReportContainer<Report> implements AllureReportContainer {

    AllureReportContainerImpl(Task task) {
        super(ConfigurableReport.class, task)
        add(TaskGeneratedSingleDirectoryReport.class, "allure-report", task, "index.html")
    }

    @Override
    DirectoryReport getAllureReport() {
        return (DirectoryReport) getByName("allure-report")
    }

}
