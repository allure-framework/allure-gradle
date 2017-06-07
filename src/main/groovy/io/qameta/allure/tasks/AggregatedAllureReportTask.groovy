package io.qameta.allure.tasks

import io.qameta.allure.AllureExtension
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
class AggregatedAllureReportTask extends AbstractAllureReportTask {

    public static final String NAME = 'aggregatedAllureReport'

    @Input
    Closure resultsGlob

    @Input
    List<String> resultsDirs

    @TaskAction
    void exec() {
        Set<String> resultsPaths = getResultsPaths()
        runReportGeneration(resultsPaths)
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
            it.getExtensions().findByType(AllureExtension.class)?.resultsFolder
        }).findAll(NON_EMPTY_DIR).toSet()
    }
}
