package io.qameta.allure

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.internal.ClosureBackedAction
import org.gradle.api.reporting.Reporting
import org.gradle.api.tasks.AbstractExecTask
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.impldep.org.codehaus.plexus.util.Os

import java.nio.file.Paths

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
class AllureTask extends AbstractExecTask<AllureTask> implements Reporting<AllureReportContainer> {

    private static Closure<Boolean> NON_EMPTY_DIR = { String path ->
        if (!path) {
            return false
        }
        File f = new File(path)
        f.directory && f.list().size() > 0
    }

    public static final String NAME = "generateAllureReport"

    private final AllureReportContainer report

    String allureVersion

    Closure resultsGlob

    List<String> resultsDirs

    String outputDir

    boolean clean

    AllureTask() {
        super(AllureTask.class)
        dependsOn(DownloadAllureTask.NAME)
        report = new AllureReportContainer(this)
        this.outputs.upToDateWhen { false }
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
        Set<String> resultsPaths = getResultsPaths()
        if (resultsPaths.isEmpty()) {
            logger.warn("Couldn't find any directories with Allure results, skipping report generation.")
            return
        }
        workingDir = project.buildDir.toPath().resolve("allure-${allureVersion}").resolve('bin').toFile()
        if (!workingDir.exists()) {
            logger.warn("Allure report is not generated, cannot find allure-commandline distribution in $workingDir")
            return
        }
        String output = Paths.get(outputDir).toAbsolutePath().toString()
        List args = ['generate'] + resultsPaths + ['-o', output]
        if (clean) {
            args += '--clean'
        }
        String executable
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            executable = "allure.bat"
        } else if (Os.isFamily(Os.FAMILY_UNIX)) {
            executable = "allure"
        } else {
            logger.warn('Not able to detect type of current OS for the Allure commandline script')
            return
        }
        new File(workingDir, executable).setExecutable(true)
        commandLine = ["./$executable"] + args
        super.exec()
        logger.info('Allure report generation is finished with exit code: ' + execResult.exitValue)
    }

    private Set<String> getResultsPaths() {
        Set<String> results = []
        if (resultsGlob) {
            results += getResultDirectoriesByGlob()
        }
        if (resultsDirs) {
            results += resultsDirs.findAll(NON_EMPTY_DIR).toSet()
        }
        return !results.empty ? results : collectResultsDirsFromSelfAndChildren()
    }

    private Set<String> getResultDirectoriesByGlob() {
        FileTree reportDirs = project.fileTree(project.buildDir, resultsGlob)
        Set<String> resultsDirs = []
        reportDirs.visit { FileVisitDetails fileDetails ->
            if (fileDetails.isDirectory()) {
                project.logger.debug("Found results directory ${fileDetails.path} by glob")
                if (fileDetails.file.list().size() == 0) {
                    project.logger.debug("Skipping empty results directory ${fileDetails.path}")
                    return
                }
                resultsDirs << fileDetails.file.absolutePath
            }
        }
        return resultsDirs
    }

    private Set<String> collectResultsDirsFromSelfAndChildren() {
        List<Project> projects = project.childProjects.values().toList() + [project]
        return projects.collect({
            project.logger.debug("Collecting folder with Allure results from project ${it.name}")
            it.getExtensions().findByType(AdaptersExtension.class)?.resultsFolder
        }).findAll(NON_EMPTY_DIR).toSet()
    }
}
