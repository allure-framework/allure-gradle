package io.qameta.allure.gradle.task

import io.qameta.allure.gradle.AllureExtension
import io.qameta.allure.gradle.util.BuildUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction

import java.nio.file.Files
import java.nio.file.Path

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
class AllureServe extends DefaultTask {

    static final String NAME = 'allureServe'

    static final String SERVE_COMMAND = 'serve'

    @Input
    String version

    @InputFiles
    List<File> resultsDirs = []

    AllureServe() {
        configureDefaults()
    }

    @TaskAction
    void serveAllureReport() {
        Path allureHome = project.rootDir.toPath().resolve('.allure').resolve("allure-${version}")
        Path allureExecutable = allureHome.resolve('bin').resolve(BuildUtils.allureExecutable).toAbsolutePath()

        if (Files.notExists(allureExecutable)) {
            logger.warn("Cannot find allure commanline in $allureHome")
            return
        }

        allureExecutable.toFile().setExecutable(true)
        project.exec {
            commandLine("$allureExecutable")
            args(SERVE_COMMAND)
            resultsDirs.each {
                args("$it.absolutePath")
            }
        }
    }

    void configureDefaults() {
        AllureExtension allureExtension = project.extensions.findByType(AllureExtension)
        if (Objects.nonNull(extensions)) {
            resultsDirs.add(new File(allureExtension.resultsDir))
            version = allureExtension.version
        }
    }
}
