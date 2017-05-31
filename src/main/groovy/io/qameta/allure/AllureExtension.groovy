package io.qameta.allure

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.reporting.ReportingExtension

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
@CompileStatic
class AllureExtension extends ReportingExtension {

    public static String NAME = "allure"

    AllureExtension(Project project) {
        super(project)
        this.allureReportDir = new File(getBaseDir(), "/allure-report").absolutePath
        this.allureResultsDir = new File(project.getBuildDir(), "/allure-results").absolutePath
    }

    String allureReportDir

    String allureResultsDir

    String allureVersion

    String aspectjVersion = "1.8.9"

    String configuration = "testCompile"

    String testNGAdapterVersion

    String downloadLinkFormat = "https://dl.bintray.com/qameta/generic/io/qameta/allure/allure/%s/allure-%<s.zip"

    boolean testNG

    boolean aspectjweaver
}
