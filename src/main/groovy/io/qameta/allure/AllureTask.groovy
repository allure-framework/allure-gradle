package io.qameta.allure

import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.internal.ClosureBackedAction
import org.gradle.api.reporting.Reporting
import org.gradle.api.tasks.AbstractExecTask
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.impldep.org.codehaus.plexus.util.Os
import org.gradle.tooling.BuildException

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
class AllureTask extends AbstractExecTask<AllureTask> implements Reporting<AllureReportContainer> {

    private final AllureReportContainer report

    public static String NAME = "allureReport"

    String allureVersion

    File inputDir

    File outputDir

    AllureTask() {
        super(AllureTask.class)
        dependsOn(DownloadAllureTask.NAME)
        report = new AllureReportContainer(this)
    }

    @Override
    AllureReportContainer getReports() {
        return report
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

    @TaskAction
    void exec() {
        workingDir = project.buildDir.absolutePath + "/allure-$allureVersion/bin"
        if(outputDir.exists() && outputDir.listFiles().length > 0){
            logger.warn("Output directory for allure report ${outputDir.absolutePath} already exists " +
                    "and contains some files")
            throw new GradleException("Couldn't generate allure report")
        }

        List args = ['generate', inputDir.absolutePath, '-o', outputDir.absolutePath]
        String executable
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            executable = "allure.bat"
        } else if (Os.isFamily(Os.FAMILY_UNIX)) {
            executable = "allure"
        } else {
            logger.warn("Not able to detect type of current OS for the allure commandline script")
            return
        }

        new File(workingDir, executable).setExecutable(true)
        commandLine = ["./$executable"] + args
        super.exec()
        execResult.assertNormalExitValue()
    }
}
