package io.qameta.allure.gradle.report.tasks

import groovy.json.JsonOutput
import java.io.File

internal fun writeAllure3Config(
    file: File,
    outputDir: File,
    singleFile: Boolean,
) {
    file.parentFile.mkdirs()
    file.writeText(
        JsonOutput.prettyPrint(
            JsonOutput.toJson(
                mapOf(
                    "output" to outputDir.absolutePath,
                    "plugins" to mapOf(
                        "awesome" to mapOf(
                            "options" to mapOf(
                                "singleFile" to singleFile
                            )
                        )
                    )
                )
            )
        )
    )
}

