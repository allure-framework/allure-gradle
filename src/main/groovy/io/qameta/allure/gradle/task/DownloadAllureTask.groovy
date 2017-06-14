package io.qameta.allure.gradle.task

import io.qameta.allure.gradle.AllureExtension
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

    DownloadAllureTask() {
        AllureExtension extension = project.extensions.getByType(AllureExtension)
        allureVersion = extension.version
        allureCliDest = project.file(new File(project.rootDir, '.allure').absolutePath)
        downloadAllureLink = extension.downloadLink ?: String.format(extension.downloadLinkFormat,
                extension.version)
    }

    @TaskAction
    downloadAllure() {
        File buildDir = project.buildDir
        allureCliDest.mkdirs()
        buildDir.mkdirs()
        String archiveDest = buildDir.absolutePath + "/allure-${allureVersion}.zip"
        project.ant {
            get(src: downloadAllureLink, dest: archiveDest, skipexisting: 'true')
            unzip(src: archiveDest, dest: allureCliDest)
        }
    }

}
