package io.qameta.allure.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
class DownloadAllureTask extends DefaultTask {

    static final String NAME = 'downloadAllure'

    @Input
    String allureVersion

    @OutputDirectory
    File allureCliDest

    @Input
    String downloadAllureLink

    @TaskAction
    downloadAllure() {
        File buildDir = project.buildDir
        buildDir.mkdir()
        String archiveDest = buildDir.absolutePath + "/allure-${allureVersion}.zip"
        project.ant() {
            get(src: downloadAllureLink, dest: archiveDest, skipexisting: 'true')
            unzip(src: archiveDest, dest: allureCliDest)
        }
    }

}
