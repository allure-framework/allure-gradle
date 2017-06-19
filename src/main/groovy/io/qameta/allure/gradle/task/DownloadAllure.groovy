package io.qameta.allure.gradle.task

import io.qameta.allure.gradle.AllureExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
class DownloadAllure extends DefaultTask {

    static final String NAME = 'downloadAllure'

    @Input
    @Optional
    String version

    @Input
    String src

    @OutputDirectory
    File dest

    DownloadAllure() {
        configureDefaults()
    }

    @TaskAction
    downloadAllure() {
        File buildDir = project.buildDir
        dest.mkdirs()
        buildDir.mkdirs()
        String archiveDest = buildDir.absolutePath + "/allure-${version}.zip"
        project.ant {
            get(src: src, dest: archiveDest, skipexisting: 'true')
            unzip(src: archiveDest, dest: dest)
        }
    }

    private void configureDefaults() {
        AllureExtension extension = project.extensions.getByType(AllureExtension)
        if (Objects.nonNull(extensions)) {
            version = extension.version
            dest = project.file(new File(project.rootDir, '.allure').absolutePath)
            src = extension.downloadLink ?: String.format(extension.downloadLinkFormat, extension.version)
        }
    }
}
