package io.qameta.allure

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
class DownloadAllureTask extends DefaultTask {

    static final String NAME = "downloadAllure"
    static final String CONFIGURATION_NAME = "downloadAllure"

    String allureVersion

    String allureCliDest

    @TaskAction
    downloadAllure() {
        String buildDir = project.buildDir.absolutePath
        project.ant() {
            get(src: "https://bintray.com/qameta/generic/download_file?file_path=io%2Fqameta%2Fallure%2Fallure%2F2.0-BETA5%2Fallure-${allureVersion}.zip",
                    dest: buildDir + '/allure.zip', skipexisting: 'true')
            unzip(src: buildDir + '/allure.zip', dest: buildDir)
        }
    }

    @OutputDirectory
    private File getReportDir() {
        project.file(allureCliDest)
    }

}
