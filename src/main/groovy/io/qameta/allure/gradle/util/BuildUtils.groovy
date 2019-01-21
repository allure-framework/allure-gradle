package io.qameta.allure.gradle.util

import groovy.json.JsonOutput
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer

import java.nio.file.Paths

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
class BuildUtils {

    public static final String EXECUTOR_FILE_NAME = 'executor.json'

    public static final String CATEGORIES_FILE_NAME = 'categories.json'

    static boolean isWindows() {
        return System.getProperty('os.name').toLowerCase().contains('win')
    }

    static String getAllureExecutable() {
        return isWindows() ? 'allure.bat' : 'allure'
    }

    static void copyExecutorInfo(File resultsDir, Project project) {
        Map<String, String> executorInfo = [name: 'Gradle', type: 'gradle', buildName: project.displayName]
        File executorFile = Paths.get(resultsDir.absoluteFile.path).resolve(EXECUTOR_FILE_NAME).toFile()
        executorFile.text = JsonOutput.toJson(executorInfo)
    }

    static void copyCategoriesInfo(File resultsDir, Project project) {
        SourceSetContainer sourceSets = (SourceSetContainer) project.properties.get('sourceSets')

        if (sourceSets != null) {
            List<File> resourcesCategoriesFiles = sourceSets.getByName('test').resources
                    .findAll { file -> (file.name == CATEGORIES_FILE_NAME) }

            if (!resourcesCategoriesFiles.isEmpty()) {
                File categoriesFile = Paths.get(resultsDir.absoluteFile.path).resolve(CATEGORIES_FILE_NAME).toFile()
                categoriesFile.text = resourcesCategoriesFiles.first().text
            }
        }
    }

}
