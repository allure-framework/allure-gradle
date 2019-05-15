package io.qameta.allure.gradle.util

import groovy.json.JsonOutput
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
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

        Path resultsPath = Paths.get(resultsDir.absoluteFile.path)
        Files.createDirectories(resultsPath)

        Path executorPath = resultsPath.resolve(EXECUTOR_FILE_NAME)
        Files.write(executorPath, JsonOutput.toJson(executorInfo).getBytes(StandardCharsets.UTF_8))
    }

    static void copyCategoriesInfo(File resultsDir, Project project) {
        SourceSetContainer sourceSets = (SourceSetContainer) project.properties.get('sourceSets')
        List<File> resourcesCategoriesFiles = sourceSets.getByName('test').resources
                .findAll { file -> (file.name == CATEGORIES_FILE_NAME) }

        Path resultsPath = Paths.get(resultsDir.absoluteFile.path)
        Files.createDirectories(resultsPath)

        if (!resourcesCategoriesFiles.isEmpty()) {
            Path categoriesPath = resultsPath.resolve(CATEGORIES_FILE_NAME)
            Files.write(categoriesPath, resourcesCategoriesFiles.first().text.getBytes(StandardCharsets.UTF_8))
        }
    }

}
