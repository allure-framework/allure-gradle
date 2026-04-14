package io.qameta.allure.gradle.report

import org.apache.tools.ant.taskdefs.condition.Os
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assume.assumeFalse
import org.junit.rules.TemporaryFolder
import java.io.File

enum class TestAllureRuntime(val version: String) {
    ALLURE_2("2.38.1"),
    ALLURE_3("3.4.1"),
}

internal object AllureRuntimeMatrixSupport {
    fun copyFixture(tempDir: TemporaryFolder, fixturePath: String): File {
        val fixtureDir = File(fixturePath)
        require(fixtureDir.isDirectory) { "Fixture $fixturePath does not exist" }

        val projectDir = tempDir.newFolder("${fixtureDir.name}-${System.nanoTime()}")
        fixtureDir.copyRecursively(projectDir, overwrite = true)

        if (!projectDir.resolve("settings.gradle").exists() && !projectDir.resolve("settings.gradle.kts").exists()) {
            val settingsFile = if (projectDir.resolve("build.gradle.kts").exists()) {
                projectDir.resolve("settings.gradle.kts")
            } else {
                projectDir.resolve("settings.gradle")
            }
            settingsFile.createNewFile()
        }
        return projectDir
    }

    fun configureRuntime(
        projectDir: File,
        runtime: TestAllureRuntime,
        usesReportRuntime: Boolean = false,
        stripLegacyCommandlineDsl: Boolean = false,
    ) {
        val buildFile = findBuildFile(projectDir)
        if (stripLegacyCommandlineDsl && runtime == TestAllureRuntime.ALLURE_3) {
            stripLegacyCommandlineDsl(buildFile)
        }
        appendSnippet(buildFile, versionOverrideSnippet(buildFile, runtime))
        if (usesReportRuntime && runtime == TestAllureRuntime.ALLURE_3) {
            requireAllure3ReportRuntimeSupport()
            val fakeRuntime = createFakeAllure3Runtime(projectDir)
            appendSnippet(buildFile, allure3DependenciesSnippet(buildFile, fakeRuntime))
        }
    }

    fun runner(projectDir: File, gradleVersion: String = "9.0.0"): GradleRunner = GradleRunner.create()
        .withProjectDir(projectDir)
        .withGradleVersion(gradleVersion)
        .withPluginClasspath()
        .withTestKitDir(projectDir.resolve(".gradle-testkit"))
        .forwardOutput()

    fun commonArgs(vararg tasks: String): List<String> = listOf(
        "--stacktrace",
        "--info",
        "-Porg.gradle.daemon=false",
        "--no-watch-fs",
        *tasks
    )

    fun assertRuntimeTasks(buildResult: BuildResult, runtime: TestAllureRuntime) {
        if (runtime == TestAllureRuntime.ALLURE_2) {
            assertThat(buildResult.task(":downloadAllure")?.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
            assertThat(buildResult.task(":downloadNode"))
                .isNull()
            assertThat(buildResult.task(":installAllure3"))
                .isNull()
        } else {
            assertThat(buildResult.task(":downloadNode")?.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
            assertThat(buildResult.task(":installAllure3")?.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
            assertThat(buildResult.task(":downloadAllure")?.outcome)
                .isEqualTo(TaskOutcome.SUCCESS)
        }
    }

    private fun findBuildFile(projectDir: File): File = listOf(
        projectDir.resolve("build.gradle"),
        projectDir.resolve("build.gradle.kts"),
    ).firstOrNull(File::exists)
        ?: error("No build.gradle or build.gradle.kts found in ${projectDir.absolutePath}")

    private fun appendSnippet(buildFile: File, snippet: String) {
        buildFile.appendText("\n\n$snippet\n")
    }

    private fun versionOverrideSnippet(buildFile: File, runtime: TestAllureRuntime): String =
        if (buildFile.name.endsWith(".kts")) {
            """
            allure {
                version.set("${runtime.version}")
            }
            """.trimIndent()
        } else {
            """
            allure {
                version = '${runtime.version}'
            }
            """.trimIndent()
        }

    private fun stripLegacyCommandlineDsl(buildFile: File) {
        val text = buildFile.readText()
        val stripped = text
            .replace(Regex("""(?ms)\n\s*commandline\s*\{.*?\n\s*\}"""), "")
            .replace(Regex("""(?m)^\s*commandline\.group.*\n?"""), "")
            .replace(Regex("""(?m)^\s*allure\.commandline\.downloadUrlPattern.*\n?"""), "")
        buildFile.writeText(stripped)
    }

    private fun requireAllure3ReportRuntimeSupport() {
        assumeFalse(
            "Fake Allure 3 runtime tests currently support Unix-like systems only",
            Os.isFamily(Os.FAMILY_WINDOWS)
        )
    }

    private fun createFakeAllure3Runtime(projectDir: File): FakeAllure3Runtime {
        val nodeArchive = createFakeNodeArchive(projectDir)
        val fakePackage = projectDir.resolve("fake-allure.tgz").apply {
            writeText("fake")
        }
        return FakeAllure3Runtime(nodeArchive, fakePackage)
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

    private fun allure3DependenciesSnippet(buildFile: File, runtime: FakeAllure3Runtime): String =
        if (buildFile.name.endsWith(".kts")) {
            """
            dependencies {
                allureNodeDistribution(files("${runtime.nodeArchive.absolutePath.replace("\\", "/")}"))
                allure3Package(files("${runtime.packageFile.absolutePath.replace("\\", "/")}"))
            }
            """.trimIndent()
        } else {
            """
            dependencies {
                allureNodeDistribution(files('${runtime.nodeArchive.absolutePath.replace("\\", "/")}'))
                allure3Package(files('${runtime.packageFile.absolutePath.replace("\\", "/")}'))
            }
            """.trimIndent()
        }

    private data class FakeAllure3Runtime(
        val nodeArchive: File,
        val packageFile: File,
    )
}
