package io.qameta.allure.gradle.report

import io.qameta.allure.gradle.rule.GradleRunnerRule
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class AllureServeIntegrationTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun `allureServe works when project path contains spaces`() {
        val projectDir = File(tempDir, "report project with spaces").apply { mkdirs() }
        File("src/it/report-only").copyRecursively(projectDir, overwrite = true)
        projectDir.resolve("settings.gradle").createNewFile()

        val fakeZip = createFakeAllureZip(projectDir.resolve("fake allure commandline.zip"))
        projectDir.resolve("build.gradle").appendText(
            """

            dependencies {
                allureCommandline(files('${fakeZip.absolutePath.replace('\\', '/')}'))
            }
            """.trimIndent()
        )

        val arguments = listOf(
            "--stacktrace",
            "--info",
            "-Porg.gradle.daemon=false",
            "--no-watch-fs",
            "allureServe"
        )
        val buildResult = GradleRunnerRule.runBuild(projectDir, "9.4.1", arguments) {
            GradleRunner.create()
                .withProjectDir(projectDir)
                .withGradleVersion("9.4.1")
                .withPluginClasspath()
                .withTestKitDir(GradleRunnerRule.testKitDirFor(projectDir))
                .withArguments(arguments)
                .forwardOutput()
                .build()
        }

        assertThat(buildResult.task(":downloadAllure")?.outcome)
            .`as`("downloadAllure task outcome")
            .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(buildResult.task(":allureServe")?.outcome)
            .`as`("allureServe task outcome")
            .isEqualTo(TaskOutcome.SUCCESS)

        val invocationFile = projectDir.resolve("build/allure/commandline/bin/invocation.txt")
        val rawResultsDir = projectDir.resolve("build/manual-allure-results")
        assertThat(invocationFile)
            .`as`("Fake allure invocation marker")
            .exists()
        assertThat(invocationFile.readText())
            .`as`("Arguments passed to fake allure")
            .contains("serve")
            .contains(rawResultsDir.canonicalPath)
    }

    private fun createFakeAllureZip(target: File): File {
        ZipOutputStream(target.outputStream().buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("fake-allure/bin/allure"))
            zip.write(
                """
                #!/bin/sh
                SCRIPT_DIR=${'$'}(CDPATH= cd -- "${'$'}(dirname "${'$'}0")" && pwd)
                : > "${'$'}SCRIPT_DIR/invocation.txt"
                for arg in "${'$'}@"; do
                  printf '%s\n' "${'$'}arg" >> "${'$'}SCRIPT_DIR/invocation.txt"
                done
                exit 0
                """.trimIndent().toByteArray()
            )
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("fake-allure/bin/allure.bat"))
            zip.write(
                """
                @echo off
                >"%~dp0invocation.txt" echo %*
                exit /b 0
                """.trimIndent().replace("\n", "\r\n").toByteArray()
            )
            zip.closeEntry()
        }
        return target
    }
}
