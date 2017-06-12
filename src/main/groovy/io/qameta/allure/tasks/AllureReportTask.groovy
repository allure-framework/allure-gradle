package io.qameta.allure.tasks

import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
class AllureReportTask extends AbstractAllureReportTask {

    static final String NAME = 'allureReport'

    @InputDirectory
    File resultsDir

    @TaskAction
    void exec() {
        Set<String> results = NON_EMPTY_DIR(resultsDir.absolutePath) ?
                [resultsDir.absolutePath].toSet() : []
        runReportGeneration(results)
    }
}
