package io.qameta.allure.tasks

import io.qameta.allure.AllureReportContainer
import io.qameta.allure.util.BuildUtils
import org.gradle.api.Action
import org.gradle.api.internal.ClosureBackedAction
import org.gradle.api.reporting.Reporting
import org.gradle.api.tasks.AbstractExecTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
abstract class AbstractAllureReportTask extends AbstractExecTask<AbstractAllureReportTask>
        implements Reporting<AllureReportContainer> {

    static Closure<Boolean> NON_EMPTY_DIR = { String path ->
        if (!path) {
            return false
        }
        File f = new File(path)
        f.directory && f.list().size() > 0
    }

    @Input
    String allureVersion

    @Input
    Boolean clean

    @OutputDirectory
    File outputDir

    AbstractAllureReportTask() {
        super(AbstractAllureReportTask)
        dependsOn(DownloadAllureTask.NAME)
    }

    @Override
    AllureReportContainer getReports() {
        return new AllureReportContainer(this)
    }

    @Override
    AllureReportContainer reports(Closure closure) {
        return reports(new ClosureBackedAction<AllureReportContainer>(closure))
    }

    @Override
    AllureReportContainer reports(Action<? super AllureReportContainer> configureAction) {
        configureAction.execute(reports)
        return reports
    }

    protected void runReportGeneration(Set<String> resutsFolders) {
        if (resutsFolders.isEmpty()) {
            logger.warn('Could not find any directories with Allure results, skipping report generation.')
            return
        }

        workingDir = project.rootDir.toPath().resolve('.allure').resolve("allure-${allureVersion}")
                .resolve('bin').toFile()
        if (!workingDir.exists()) {
            logger.warn("Allure report is not generated, cannot find allure-commandline distribution in $workingDir")
            return
        }
        String output = outputDir.getAbsolutePath()
        List args = ['generate'] + resutsFolders + ['-o', output]
        if (clean) {
            args += '--clean'
        }
        String executable = BuildUtils.allureExecutable
        new File(workingDir, executable).setExecutable(true)
        commandLine = ["./$executable"] + args
        super.exec()
        logger.info("Allure report generation is finished with exit code: ${execResult.exitValue}")
    }
}
