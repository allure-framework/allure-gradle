package io.qameta.allure

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.reporting.ReportingExtension

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
@CompileStatic
class AllureReportExtension extends ReportingExtension {

    public static String NAME = "allureReport"

    AllureReportExtension(Project project) {
        super(project)
        project.logger.info("in extension constructor, setting report dir to ${new File(getBaseDir(), "/allure-report").absolutePath}")
        this.reportDir = new File(getBaseDir(), "/allure-report").absolutePath
    }

    String reportDir

    List<String> resultsDirs

    Closure resultsGlob

    String version

    String downloadLinkFormat = "https://dl.bintray.com/qameta/generic/io/qameta/allure/allure/%s/allure-%<s.zip"

    String downloadLink

    boolean clean = false
}
