package io.qameta.allure

import groovy.transform.CompileStatic
import org.gradle.api.Project

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
@CompileStatic
class AdaptersExtension {

    AdaptersExtension(Project project) {
        resultsFolder = new File(project.buildDir, "/allure-results").absolutePath
    }

    public static final String NAME = 'allure'

    boolean autoconfigure = false

    boolean aspectjweaver

    String allureJavaVersion = '2.0-BETA8'

    String configuration = 'testCompile'

    String aspectjVersion = '1.8.9'

    String resultsFolder

    Closure useTestNG

    Closure useJUnit4

    Closure useCucumberJVM
}
