package io.qameta.allure.tasks

import io.qameta.allure.util.BuildUtils
import org.gradle.api.tasks.AbstractExecTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
class AllureServeTask extends AbstractExecTask<AllureServeTask> {

    static final String NAME = 'serve'

    AllureServeTask() {
        super(AllureServeTask)
    }

    @Input
    String allureVersion

    @Input
    File resultsDir

    @TaskAction
    void exec() {
        workingDir = project.rootDir.toPath().resolve('.allure').resolve("allure-${allureVersion}")
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
