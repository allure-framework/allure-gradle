package io.qameta.allure.gradle.task

import io.qameta.allure.gradle.util.BuildUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

import java.nio.file.Files
import java.nio.file.Path

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
class AllureServe extends DefaultTask {

    static final String NAME = 'allureServe'

    @Input
    String version

    @Input
    File resultsDir

    @TaskAction
    void serveAllureReport() {
        Path allureHome = project.rootDir.toPath().resolve('.allure').resolve("allure-${version}")
        Path allureExecutable = allureHome.resolve('bin').resolve(BuildUtils.allureExecutable).toAbsolutePath()

        if (Files.notExists(allureExecutable)) {
            logger.warn("Cannot find allure commanline in $allureHome")
            return
        }

        project.exec {
            commandLine = "$allureExecutable"
            args = ['serve', "$resultsDir.absolutePath"]
        }
    }
}
