package io.qameta.allure.gradle

import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.reporting.ReportingExtension

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
@CompileStatic
class AllureExtension extends ReportingExtension {

    public static final String NAME = 'allure'

    boolean autoconfigure = false

    boolean aspectjweaver

    File resultsDir

    File reportDir

    String allureJavaVersion = '2.0-BETA21'

    String configuration = 'testCompile'

    String aspectjVersion = '1.8.10'

    Closure useTestNG

    Closure useJUnit4

    Closure useJUnit5

    Closure useCucumberJVM

    Closure useSpock

    String version

    String downloadLinkFormat = 'https://dl.bintray.com/qameta/generic/io/qameta/allure/allure/%s/allure-%<s.zip'

    String downloadLink

    boolean clean = true

    AllureExtension(Project project) {
        super(project)
        this.resultsDir = new File(project.buildDir, '/allure-results')
        this.reportDir = new File(baseDir as File, '/allure-report')
    }

}
