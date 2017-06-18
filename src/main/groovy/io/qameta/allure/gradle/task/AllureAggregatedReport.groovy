package io.qameta.allure.gradle.task

import io.qameta.allure.gradle.AllureExtension
import org.gradle.api.Project
import org.gradle.api.file.FileTree
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
class AllureAggregatedReport extends AbstractAllureReport {

    public static final String NAME = 'allureAggregatedReport'

    @Optional
    Closure resultsGlob

    @Input
    List<String> resultsDirs

    @TaskAction
    void exec() {
        Set<String> resultsPaths = collectResultsPaths()
        runReportGeneration(resultsPaths)
    }

    private Set<String> collectResultsPaths() {
        Set<String> results = []
        if (resultsGlob) {
            results += collectResultDirectoriesByGlob()
        }
        if (resultsDirs) {
            results += resultsDirs.findAll(nonEmptyDir).toSet()
        }
        return results.empty ? collectResultsDirsFromSelfAndChildren() : results
    }

    private Set<String> collectResultDirectoriesByGlob() {
        FileTree reportDirs = project.fileTree(project.rootDir, resultsGlob)
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
        return projects.collect {
            project.logger.debug("Collecting folder with Allure results from project ${it.name}")
            it.exten().findByType(AllureExtension)?.resultsDir
        }.findAll(nonEmptyDir).toSet()
    }
}
