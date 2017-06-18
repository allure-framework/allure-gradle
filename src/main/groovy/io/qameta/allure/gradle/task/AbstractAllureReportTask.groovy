package io.qameta.allure.gradle.task

import io.qameta.allure.gradle.AllureReportContainer
import io.qameta.allure.gradle.util.BuildUtils
import org.gradle.api.Action
import org.gradle.api.internal.ClosureBackedAction
import org.gradle.api.reporting.Reporting
import org.gradle.api.tasks.AbstractExecTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
class AbstractAllureReportTask extends AbstractExecTask<AbstractAllureReportTask>
        implements Reporting<AllureReportContainer> {

    static Closure<Boolean> nonEmptyDir = { String path ->
        if (!path) {
            return false
        }
        File f = new File(path)
        f.directory && f.list().size() > 0
    }

    @Input
    Boolean clean

    @Input
    String version

    @OutputDirectory
    File reportDir

    AbstractAllureReportTask() {
        super(AbstractAllureReportTask)
        dependsOn(DownloadAllureTask.NAME)
    }

    @Internal
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

        workingDir = project.rootDir.toPath().resolve('.allure').resolve("allure-${version}")
                .resolve('bin').toFile()
        if (!workingDir.exists()) {
            logger.warn("Allure report is not generated, cannot find allure-commandline distribution in $workingDir")
            return
        }
        String output = reportDir.absolutePath
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
