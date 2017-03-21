package io.qameta.allure

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
class DownloadAllureTask extends DefaultTask {

    static final String NAME = "downloadAllure"

    String allureVersion

    @OutputDirectory
    File allureCliDest

    String downloadAllureLink

    @TaskAction
    downloadAllure() {
        String buildDir = project.buildDir.absolutePath
        String archiveDest = buildDir + "/allure-${allureVersion}.zip"
        project.ant() {
            get(src: downloadAllureLink, dest: archiveDest, skipexisting: 'true')
            unzip(src: archiveDest, dest: buildDir)
        }
    }

}
