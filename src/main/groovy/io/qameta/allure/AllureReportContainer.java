package io.qameta.allure;

import org.gradle.api.reporting.DirectoryReport;
import org.gradle.api.reporting.Report;
import org.gradle.api.reporting.ReportContainer;
import org.gradle.api.tasks.Internal;

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
public interface AllureReportContainer extends ReportContainer<Report> {

    @Internal
    DirectoryReport getAllureReport();
}
