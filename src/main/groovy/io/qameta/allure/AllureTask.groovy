package io.qameta.allure

import org.gradle.api.Action
import org.gradle.api.internal.ClosureBackedAction
import org.gradle.api.reporting.Reporting
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.impldep.org.codehaus.plexus.util.Os

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
class AllureTask extends Exec implements Reporting<AllureReportContainer> {

    private final AllureReportContainer report

    public static String CONFIGURATION_NAME = "allureReport"

    public static String NAME = "allureReport"

    String allureVersion

    @InputDirectory
    File inputDir

    @OutputDirectory
    File outputDir

    AllureTask() {
        super()
        dependsOn(DownloadAllureTask.NAME)
        report = new AllureReportContainerImpl(this)
    }

    @Override
    AllureReportContainer getReports() {
        return report
    }

    @Override
    AllureReportContainer reports(Closure closure) {
        return reports(new ClosureBackedAction<AllureReportContainer>(closure));
    }

    @Override
    AllureReportContainer reports(Action<? super AllureReportContainer> configureAction) {
        configureAction.execute(reports)
        return reports
    }

    @TaskAction
    void exec() {
        workingDir = project.buildDir + "/$allureVersion/bin"
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            executable = "allure.bat"
        } else if (Os.isFamily(Os.FAMILY_UNIX)) {
            executable = "allure.sh"
        } else {
            logger.warn("Not able to detect type of current OS for the allure commandline script")
            return
        }

        args = ['generate', inputDir.absolutePath, '-o', outputDir.absolutePath]
    }
}
