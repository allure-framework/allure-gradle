package io.qameta.allure.gradle.report

import io.qameta.allure.gradle.rule.GradleRunnerRule
import org.apache.tools.ant.taskdefs.condition.Os
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class Allure3ReportIntegrationTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun `allureReport should use Allure 3 by default`() {
        assumeFalse(Os.isFamily(Os.FAMILY_WINDOWS), "Fake Allure 3 runtime tests currently support Unix-like systems only")

        val projectDir = createAllure3Project(singleFile = true)

        val buildResult = runBuild(projectDir, "allureReport")

        assertThat(buildResult.task(":downloadNode")?.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(buildResult.task(":installAllure3")?.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(buildResult.task(":downloadAllure")?.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(buildResult.task(":allureReport")?.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)

        val configFile = projectDir.resolve("build/tmp/allureReport/allurerc.json")
        assertThat(configFile)
            .exists()
        assertThat(configFile.readText())
            .contains("\"singleFile\": true")

        val reportDir = projectDir.resolve("build/reports/allure-report/allureReport")
        assertThat(reportDir.resolve("summary.json"))
            .exists()

        val invocations = projectDir.resolve("build/allure/commandline/node/invocations.txt").readText()
        assertThat(invocations)
            .contains("command=generate")
            .contains("cli=")
    }

    @Test
    fun `allureReport should accept single-file as a task option`() {
        assumeFalse(Os.isFamily(Os.FAMILY_WINDOWS), "Fake Allure 3 runtime tests currently support Unix-like systems only")

        val projectDir = createAllure3Project(singleFile = false)

        val buildResult = runBuild(projectDir, "allureReport", "--single-file=true")

        assertThat(buildResult.task(":allureReport")?.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)

        val configFile = projectDir.resolve("build/tmp/allureReport/allurerc.json")
        assertThat(configFile)
            .exists()
        assertThat(configFile.readText())
            .contains("\"singleFile\": true")
    }

    @Test
    fun `allureServe should run open for Allure 3`() {
        assumeFalse(Os.isFamily(Os.FAMILY_WINDOWS), "Fake Allure 3 runtime tests currently support Unix-like systems only")

        val projectDir = createAllure3Project(singleFile = false)

        val buildResult = runBuild(projectDir, "allureServe", "--port", "4567")

        assertThat(buildResult.task(":allureServe")?.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)

        val invocations = projectDir.resolve("build/allure/commandline/node/invocations.txt").readText()
        assertThat(invocations)
            .contains("command=generate")
            .contains("command=open")
            .contains("--port 4567")
    }

    @Test
    fun `allureServe should reject host for Allure 3`() {
        assumeFalse(Os.isFamily(Os.FAMILY_WINDOWS), "Fake Allure 3 runtime tests currently support Unix-like systems only")

        val projectDir = createAllure3Project(singleFile = false)

        val failure = runCatching {
            runBuild(projectDir, "allureServe", "--host", "127.0.0.1")
        }.exceptionOrNull() as? UnexpectedBuildFailure

        val buildFailure = requireNotNull(failure)
        assertThat(buildFailure.message)
            .contains("--host is not supported for Allure 3")
    }

    @Test
    fun `Allure 2 commandline customization should fail for Allure 3`() {
        assumeFalse(Os.isFamily(Os.FAMILY_WINDOWS), "Fake Allure 3 runtime tests currently support Unix-like systems only")

        val projectDir = createAllure3Project(
            singleFile = false,
            extraAllureBlock = """
                commandline {
                    downloadUrlPattern = "https://example.test/allure.zip"
                }
            """.trimIndent()
        )

        val failure = runCatching {
            runBuild(projectDir, "downloadAllure")
        }.exceptionOrNull() as? UnexpectedBuildFailure

        val buildFailure = requireNotNull(failure)
        assertThat(buildFailure.message)
            .contains("Allure 3 does not support Allure 2 allure.commandline")
    }

    private fun createAllure3Project(singleFile: Boolean, extraAllureBlock: String = ""): File {
        val projectDir = File(tempDir, "allure3-project-${System.nanoTime()}").apply { mkdirs() }
        projectDir.resolve("settings.gradle").createNewFile()

        createManualResults(projectDir)
        val nodeArchive = createFakeNodeArchive(projectDir)
        val fakePackage = projectDir.resolve("fake-allure.tgz").apply {
            writeText("fake")
        }

        projectDir.resolve("build.gradle").writeText(
            """
            plugins {
                id 'io.qameta.allure-report'
            }

            dependencies {
                allureReport(files("${'$'}buildDir/manual-allure-results"))
                allureNodeDistribution(files('${nodeArchive.absolutePath.replace("\\", "/")}'))
                allure3Package(files('${fakePackage.absolutePath.replace("\\", "/")}'))
            }

            allure {
                report {
                    singleFile = ${if (singleFile) "true" else "false"}
                }
                $extraAllureBlock
            }
            """.trimIndent()
        )
        return projectDir
    }

    private fun createManualResults(projectDir: File) {
        val resultsDir = projectDir.resolve("build/manual-allure-results")
        resultsDir.mkdirs()
        resultsDir.resolve("simple-result.json").writeText("{}")
    }

    private fun createFakeNodeArchive(projectDir: File): File {
        val rootDir = projectDir.resolve("fake-node")
        val nodeRoot = rootDir.resolve("node-v22.22.0-test")
        val binDir = nodeRoot.resolve("bin")
        binDir.mkdirs()

        binDir.resolve("node").writeText(
            """
            #!/bin/sh
            SCRIPT_DIR=${'$'}(CDPATH= cd -- "${'$'}(dirname "${'$'}0")" && pwd)
            LOG_FILE="${'$'}SCRIPT_DIR/../invocations.txt"
            CLI_FILE="${'$'}1"
            shift
            COMMAND="${'$'}1"
            shift
            {
              printf 'cli=%s\n' "${'$'}CLI_FILE"
              printf 'command=%s\n' "${'$'}COMMAND"
              printf 'args=%s\n' "${'$'}*"
            } >> "${'$'}LOG_FILE"
            if [ "${'$'}COMMAND" = "generate" ]; then
              CONFIG=""
              while [ "${'$'}#" -gt 0 ]; do
                if [ "${'$'}1" = "--config" ]; then
                  CONFIG="${'$'}2"
                  break
                fi
                shift
              done
              OUTPUT=${'$'}(sed -n 's/.*"output"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "${'$'}CONFIG" | head -n 1)
              mkdir -p "${'$'}OUTPUT"
              printf '{}' > "${'$'}OUTPUT/summary.json"
            fi
            exit 0
            """.trimIndent() + "\n"
        )
        binDir.resolve("npm").writeText(
            """
            #!/bin/sh
            PREFIX=""
            TARGET=""
            while [ "${'$'}#" -gt 0 ]; do
              case "${'$'}1" in
                --prefix)
                  PREFIX="${'$'}2"
                  shift 2
                  ;;
                *)
                  TARGET="${'$'}1"
                  shift
                  ;;
              esac
            done
            mkdir -p "${'$'}PREFIX/node_modules/allure"
            printf '%s\n' "${'$'}TARGET" > "${'$'}PREFIX/install-target.txt"
            printf 'console.log("fake allure");\n' > "${'$'}PREFIX/node_modules/allure/cli.js"
            """.trimIndent() + "\n"
        )
        binDir.resolve("node").setExecutable(true)
        binDir.resolve("npm").setExecutable(true)

        val archive = projectDir.resolve("fake-node.tar.gz")
        val process = ProcessBuilder(
            "tar",
            "-czf",
            archive.absolutePath,
            "-C",
            rootDir.absolutePath,
            nodeRoot.name
        ).inheritIO().start()
        check(process.waitFor() == 0) { "Failed to create fake Node.js archive" }
        return archive
    }

    private fun runner(projectDir: File): GradleRunner = GradleRunner.create()
        .withProjectDir(projectDir)
        .withGradleVersion("9.4.1")
        .withPluginClasspath()
        .withTestKitDir(GradleRunnerRule.testKitDirFor(projectDir))
        .forwardOutput()

    private fun commonArgs(vararg tasks: String): List<String> = listOf(
        "--stacktrace",
        "--info",
        "-Porg.gradle.daemon=false",
        "--no-watch-fs",
        *tasks
    )

    private fun runBuild(projectDir: File, vararg tasks: String) =
        GradleRunnerRule.runBuild(projectDir, "9.4.1", commonArgs(*tasks)) {
            runner(projectDir).withArguments(commonArgs(*tasks)).build()
        }
}
