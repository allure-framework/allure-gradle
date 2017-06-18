package io.qameta.allure.gradle.task

import io.qameta.allure.gradle.util.BuildUtils
import org.gradle.api.tasks.AbstractExecTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
class AllureServe extends AbstractExecTask<AllureServe> {

    static final String NAME = 'allureServe'

    @Input
    String version

    @Input
    File resultsDir

    AllureServe() {
        super(AllureServe)
    }

    @TaskAction
    void exec() {
        workingDir = project.rootDir.toPath().resolve('.allure').resolve("allure-${version}")
                .resolve('bin').toFile()
        if (!workingDir.exists()) {
            logger.warn("Cannot find allure-commandline distribution in $workingDir, serve command is skipped")
            return
        }
        String executable = BuildUtils.allureExecutable
        new File(workingDir, executable).setExecutable(true)
        commandLine = ["./$executable", 'serve', resultsDir.absolutePath]
        super.exec()
    }
}
