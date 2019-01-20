package io.qameta.allure.gradle

import groovy.transform.CompileStatic
import io.qameta.allure.gradle.config.CucumberJVMConfig
import io.qameta.allure.gradle.config.JUnit4Config
import io.qameta.allure.gradle.config.JUnit5Config
import io.qameta.allure.gradle.config.SpockConfig
import io.qameta.allure.gradle.config.TestNGConfig
import org.gradle.api.Action
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

    protected TestNGConfig testNGConfig

    void useTestNG(Action<? super TestNGConfig> action) {
        testNGConfig = new TestNGConfig()
        action.execute(testNGConfig)
    }

    protected JUnit4Config junit4Config

    void useJUnit4(Action<? super JUnit4Config> action) {
        junit4Config = new JUnit4Config()
        action.execute(junit4Config)
    }

    protected JUnit5Config junit5Config

    void useJUnit5(Action<? super JUnit5Config> action) {
        junit5Config = new JUnit5Config()
        action.execute(junit5Config)
    }

    protected CucumberJVMConfig cucumberJVMConfig

    void useCucumberJVM(Action<? super CucumberJVMConfig> action) {
        cucumberJVMConfig = new CucumberJVMConfig()
        action.execute(cucumberJVMConfig)
    }

    protected SpockConfig spockConfig

    void useSpock(Action<? super SpockConfig> action) {
        spockConfig = new SpockConfig()
        action.execute(spockConfig)
    }

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
