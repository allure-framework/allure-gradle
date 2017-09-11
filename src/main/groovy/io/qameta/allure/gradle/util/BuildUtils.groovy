package io.qameta.allure.gradle.util

import groovy.json.JsonOutput
import org.apache.commons.io.FileUtils

import java.nio.charset.StandardCharsets
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

    static void copyExecutorInfo(File resultsDir, Map<String, String> executorInfo) {
        File executorInfoFile = Paths.get(resultsDir.absoluteFile.path).resolve('executor.json').toFile()
        FileUtils.writeStringToFile(executorInfoFile, JsonOutput.toJson(executorInfo), StandardCharsets.UTF_8)
    }

}
