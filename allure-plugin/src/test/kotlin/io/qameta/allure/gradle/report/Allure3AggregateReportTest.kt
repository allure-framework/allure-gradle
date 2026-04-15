package io.qameta.allure.gradle.report

import io.qameta.allure.gradle.rule.GradleRunnerRule
import org.apache.tools.ant.taskdefs.condition.Os
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class Allure3AggregateReportTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun `allureAggregateReport should use Allure 3 by default`() {
        assumeFalse(Os.isFamily(Os.FAMILY_WINDOWS), "Fake Allure 3 runtime tests currently support Unix-like systems only")

        val projectDir = File(tempDir, "allure3-aggregate").apply { mkdirs() }
        File("src/it/report-multi").copyRecursively(projectDir, overwrite = true)

        val nodeArchive = createFakeNodeArchive(projectDir)
        val fakePackage = projectDir.resolve("fake-allure.tgz").apply {
            writeText("fake")
        }

        projectDir.resolve("build.gradle").writeText(
            """
            plugins {
                id 'io.qameta.allure-aggregate-report'
            }

            configurations.allureAggregateReport.dependencies.clear()
            dependencies {
                allureAggregateReport(project(":module1"))
                allureAggregateReport(project(":module2"))
                allureAggregateReport(project(":module3"))
                allureNodeDistribution(files('${nodeArchive.absolutePath.replace("\\", "/")}'))
                allure3Package(files('${fakePackage.absolutePath.replace("\\", "/")}'))
            }
            """.trimIndent()
        )

        val buildResult = GradleRunner.create()
            .withProjectDir(projectDir)
            .withGradleVersion("9.0.0")
            .withPluginClasspath()
            .withTestKitDir(GradleRunnerRule.testKitDirFor(projectDir))
            .withArguments(
                "--stacktrace",
                "--info",
                "-Porg.gradle.daemon=false",
                "--no-watch-fs",
                "allureAggregateReport"
            )
            .forwardOutput()
            .build()

        assertThat(buildResult.task(":downloadNode")?.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(buildResult.task(":installAllure3")?.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(buildResult.task(":allureAggregateReport")?.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(projectDir.resolve("build/reports/allure-report/allureAggregateReport/summary.json"))
            .exists()
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
            CLI_FILE="${'$'}1"
            shift
            COMMAND="${'$'}1"
            shift
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
            while [ "${'$'}#" -gt 0 ]; do
              case "${'$'}1" in
                --prefix)
                  PREFIX="${'$'}2"
                  shift 2
                  ;;
                *)
                  shift
                  ;;
              esac
            done
            mkdir -p "${'$'}PREFIX/node_modules/allure"
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
}
