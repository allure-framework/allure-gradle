package io.qameta.allure.gradle.util

import groovy.json.JsonOutput
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer

import java.nio.file.Paths

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
class BuildUtils {

    static boolean isWindows() {
        return System.getProperty('os.name').toLowerCase().contains('win')
    }

    static String getAllureExecutable() {
        return isWindows() ? 'allure.bat' : 'allure'
    }

    static void copyExecutorInfo(File resultsDir, Project project) {
        Map<String, String> executorInfo = [name: 'Gradle', type: 'gradle', buildName: project.displayName]
        File executorFile = Paths.get(resultsDir.absoluteFile.path).resolve('executor.json').toFile()
        executorFile.text = JsonOutput.toJson(executorInfo)
    }

    static void copyCategoriesInfo(File resultsDir, Project project) {
        SourceSetContainer sourceSets = (SourceSetContainer) project.getProperties().get('sourceSets')
        List<File> resourcesCategoriesFiles = sourceSets.getByName('test').getResources()
                .findAll { file -> (file.name == 'categories.json') }

        if (!resourcesCategoriesFiles.isEmpty()) {
            File categoriesFile = Paths.get(resultsDir.absoluteFile.path).resolve("categories.json").toFile()
            categoriesFile.text = resourcesCategoriesFiles.first().text
        }
    }

}
