package io.qameta.allure.gradle.task

import io.qameta.allure.gradle.AllureExtension
import io.qameta.allure.gradle.AllureReportContainer
import io.qameta.allure.gradle.util.BuildUtils
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.internal.ClosureBackedAction
import org.gradle.api.reporting.Reporting
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import java.nio.file.Files
import java.nio.file.Path

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
class AllureReport extends DefaultTask implements Reporting<AllureReportContainer> {

    static final String NAME = 'allureReport'

    static final String GENERATE_COMMAND = 'generate'

    @OutputDirectory
    File reportDir

    @Input
    boolean clean

    @Input
    String version

    @InputFiles
    List<File> resultsDirs = []

    AllureReport() {
        dependsOn(DownloadAllure.NAME)
        configureDefaults()
    }

    @TaskAction
    void generateAllureReport() {
        Path allureHome = project.rootDir.toPath().resolve('.allure').resolve("allure-${version}")
        Path allureExecutable = allureHome.resolve('bin').resolve(BuildUtils.allureExecutable).toAbsolutePath()

        if (Files.notExists(allureExecutable)) {
            logger.warn("Cannot find allure commanline in $allureHome")
            return
        }

        allureExecutable.toFile().setExecutable(true)
        project.exec {
            commandLine("$allureExecutable")
            args(GENERATE_COMMAND)
            resultsDirs.each {
                args("$it.absolutePath")
            }
            if (clean) {
                args('--clean')
            }
            args('-o', "$reportDir.absolutePath")
        }
    }

    private void configureDefaults() {
        AllureExtension allureExtension = project.extensions.findByType(AllureExtension)
        if (Objects.nonNull(extensions)) {
            resultsDirs.add(new File(allureExtension.resultsDir))
            reportDir = new File(allureExtension.reportDir)
            version = allureExtension.version
            clean = allureExtension.clean
        }
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

}
