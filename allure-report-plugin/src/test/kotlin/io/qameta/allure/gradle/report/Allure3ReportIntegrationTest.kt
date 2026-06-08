package io.qameta.allure.gradle.report

import io.qameta.allure.gradle.rule.GradleTestVersion
import io.qameta.allure.gradle.rule.GradleRunnerRule
import org.apache.commons.compress.archivers.zip.UnixStat
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.tools.ant.taskdefs.condition.Os
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Path

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

        assertNpmSymlink(projectDir.resolve("build/allure/node/bin/npm"))
        assertNpmSymlink(projectDir.resolve("build/allure/commandline/node/bin/npm"))

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
    fun `downloadNode extracts tar gzip distributions with legacy buildscript commons io`() {
        assumeFalse(Os.isFamily(Os.FAMILY_WINDOWS), "Symlink assertions require Unix-like filesystem semantics")

        val projectDir = File(tempDir, "allure3-tar-project-${System.nanoTime()}").apply { mkdirs() }
        projectDir.resolve("settings.gradle").createNewFile()
        val nodeArchive = createFakeNodeArchive(projectDir)
        val legacyCommonsIo = createLegacyCommonsIoJar(projectDir)

        projectDir.resolve("build.gradle").writeText(
            """
            buildscript {
                dependencies {
                    classpath files('${legacyCommonsIo.absolutePath.replace("\\", "/")}')
                }
            }

            plugins {
                id 'io.qameta.allure-report'
            }

            dependencies {
                allureNodeDistribution(files('${nodeArchive.absolutePath.replace("\\", "/")}'))
            }
            """.trimIndent()
        )

        val buildResult = runBuild(projectDir, "downloadNode")

        assertThat(buildResult.task(":downloadNode")?.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(projectDir.resolve("build/allure/node/bin/node"))
            .exists()
        assertNpmSymlink(projectDir.resolve("build/allure/node/bin/npm"))
    }

    @Test
    fun `downloadNode preserves symlinks from zip distributions`() {
        assumeFalse(Os.isFamily(Os.FAMILY_WINDOWS), "Symlink assertions require Unix-like filesystem semantics")

        val projectDir = File(tempDir, "allure3-zip-project-${System.nanoTime()}").apply { mkdirs() }
        projectDir.resolve("settings.gradle").createNewFile()
        val nodeArchive = createFakeNodeZipArchive(projectDir)

        projectDir.resolve("build.gradle").writeText(
            """
            plugins {
                id 'io.qameta.allure-report'
            }

            dependencies {
                allureNodeDistribution(files('${nodeArchive.absolutePath.replace("\\", "/")}'))
            }
            """.trimIndent()
        )

        val buildResult = runBuild(projectDir, "downloadNode")

        assertThat(buildResult.task(":downloadNode")?.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)
        assertNpmSymlink(projectDir.resolve("build/allure/node/bin/npm"))
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
    fun `allureReport should use custom Allure 3 config file`() {
        assumeFalse(Os.isFamily(Os.FAMILY_WINDOWS), "Fake Allure 3 runtime tests currently support Unix-like systems only")

        val projectDir = createAllure3Project(
            singleFile = false,
            extraAllureBlock = """
                report.configFile = layout.projectDirectory.file('allurerc.mjs')
            """.trimIndent()
        )
        val customConfig = projectDir.resolve("allurerc.mjs").apply {
            writeText(
                """
                export default {
                    name: "Custom Allure Report",
                    environments: {
                        linux: { name: "Linux", matcher: () => true },
                    },
                };
                """.trimIndent()
            )
        }

        val buildResult = runBuild(projectDir, "allureReport")

        assertThat(buildResult.task(":allureReport")?.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)

        assertThat(projectDir.resolve("build/tmp/allureReport/allurerc.json"))
            .doesNotExist()

        val reportDir = projectDir.resolve("build/reports/allure-report/allureReport")
        assertThat(reportDir.resolve("summary.json"))
            .exists()

        val invocations = projectDir.resolve("build/allure/commandline/node/invocations.txt").readText()
        assertThat(invocations)
            .contains("command=generate")
            .contains("--config ${customConfig.canonicalPath}")
            .contains("--output ${reportDir.canonicalPath}")
    }

    @Test
    fun `allureReport should accept config-file as a task option`() {
        assumeFalse(Os.isFamily(Os.FAMILY_WINDOWS), "Fake Allure 3 runtime tests currently support Unix-like systems only")

        val projectDir = createAllure3Project(singleFile = false)
        val customConfig = projectDir.resolve("allurerc.mjs").apply {
            writeText("export default { name: \"Task option config\" };\n")
        }

        val buildResult = runBuild(projectDir, "allureReport", "--config-file", customConfig.absolutePath)

        assertThat(buildResult.task(":allureReport")?.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(projectDir.resolve("build/tmp/allureReport/allurerc.json"))
            .doesNotExist()

        val invocations = projectDir.resolve("build/allure/commandline/node/invocations.txt").readText()
        assertThat(invocations)
            .contains("--config ${customConfig.absolutePath}")
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
    fun `allureServe should use custom Allure 3 config file`() {
        assumeFalse(Os.isFamily(Os.FAMILY_WINDOWS), "Fake Allure 3 runtime tests currently support Unix-like systems only")

        val projectDir = createAllure3Project(
            singleFile = false,
            extraAllureBlock = """
                report.configFile = layout.projectDirectory.file('allurerc.mjs')
            """.trimIndent()
        )
        val customConfig = projectDir.resolve("allurerc.mjs").apply {
            writeText("export default { name: \"Serve config\" };\n")
        }

        val buildResult = runBuild(projectDir, "allureServe", "--port", "4567")

        assertThat(buildResult.task(":allureServe")?.outcome)
            .isEqualTo(TaskOutcome.SUCCESS)
        assertThat(projectDir.resolve("build/tmp/allureServe/allurerc.json"))
            .doesNotExist()

        val reportDir = projectDir.resolve("build/reports/allure-report/allureServe")
        val invocations = projectDir.resolve("build/allure/commandline/node/invocations.txt").readText()
        assertThat(invocations)
            .contains("command=generate")
            .contains("command=open")
            .contains("--config ${customConfig.canonicalPath}")
            .contains("--output ${reportDir.canonicalPath}")
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

    @Test
    fun `allureReport should reject custom config file for Allure 2`() {
        val projectDir = File(tempDir, "allure2-custom-config-${System.nanoTime()}").apply { mkdirs() }
        projectDir.resolve("settings.gradle").createNewFile()
        createManualResults(projectDir)
        val commandlineArchive = createFakeAllure2CommandlineZip(projectDir)
        projectDir.resolve("allurerc.mjs").writeText("export default { name: \"Allure 2 unsupported\" };\n")

        projectDir.resolve("build.gradle").writeText(
            """
            plugins {
                id 'io.qameta.allure-report'
            }

            dependencies {
                allureReport(files("${'$'}buildDir/manual-allure-results"))
                allureCommandline(files('${commandlineArchive.absolutePath.replace("\\", "/")}'))
            }

            allure {
                version = '2.42.1'
                report.configFile = layout.projectDirectory.file('allurerc.mjs')
            }
            """.trimIndent()
        )

        val failure = runCatching {
            runBuild(projectDir, "allureReport")
        }.exceptionOrNull() as? UnexpectedBuildFailure

        val buildFailure = requireNotNull(failure)
        assertThat(buildFailure.message)
            .contains("allure.report.configFile is supported only for Allure 3")
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
        val nodeRoot = rootDir.resolve("node-v26.3.0-test")
        val binDir = nodeRoot.resolve("bin")
        binDir.mkdirs()
        val npmCli = nodeRoot.resolve("lib/node_modules/npm/bin/npm-cli.js")
        npmCli.parentFile.mkdirs()

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
              OUTPUT=""
              while [ "${'$'}#" -gt 0 ]; do
                case "${'$'}1" in
                  --config)
                    CONFIG="${'$'}2"
                    shift 2
                    ;;
                  --output|-o)
                    OUTPUT="${'$'}2"
                    shift 2
                    ;;
                  *)
                    shift
                    ;;
                esac
              done
              if [ -z "${'$'}OUTPUT" ]; then
                OUTPUT=${'$'}(sed -n 's/.*"output"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' "${'$'}CONFIG" | head -n 1)
              fi
              mkdir -p "${'$'}OUTPUT"
              printf '{}' > "${'$'}OUTPUT/summary.json"
            fi
            exit 0
            """.trimIndent() + "\n"
        )
        npmCli.writeText(
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
        Files.createSymbolicLink(binDir.resolve("npm").toPath(), Path.of("../lib/node_modules/npm/bin/npm-cli.js"))
        binDir.resolve("node").setExecutable(true)
        npmCli.setExecutable(true)

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

    private fun createLegacyCommonsIoJar(projectDir: File): File {
        val sourceDir = projectDir.resolve("legacy-commons-io/src")
        val classesDir = projectDir.resolve("legacy-commons-io/classes")
        val sourceFile = sourceDir.resolve("org/apache/commons/io/IOUtils.java")
        sourceFile.parentFile.mkdirs()
        classesDir.mkdirs()
        sourceFile.writeText(
            """
            package org.apache.commons.io;

            public final class IOUtils {
                private IOUtils() {
                }

                public static long skip(java.io.InputStream input, long toSkip) throws java.io.IOException {
                    return input.skip(toSkip);
                }
            }
            """.trimIndent()
        )

        val javac = ProcessBuilder(
            "javac",
            "-d",
            classesDir.absolutePath,
            sourceFile.absolutePath
        ).inheritIO().start()
        check(javac.waitFor() == 0) { "Failed to compile legacy Commons IO fixture" }

        val archive = projectDir.resolve("legacy-commons-io.jar")
        val jar = ProcessBuilder(
            "jar",
            "cf",
            archive.absolutePath,
            "-C",
            classesDir.absolutePath,
            "."
        ).inheritIO().start()
        check(jar.waitFor() == 0) { "Failed to create legacy Commons IO fixture" }
        return archive
    }

    private fun createFakeAllure2CommandlineZip(projectDir: File): File {
        val archive = projectDir.resolve("fake-allure2.zip")
        ZipArchiveOutputStream(archive).use { zip ->
            val root = "allure-2.42.1"
            zip.addDirectory("$root/bin/")
            zip.addFile("$root/bin/allure", "#!/bin/sh\nexit 0\n")
        }
        return archive
    }

    private fun createFakeNodeZipArchive(projectDir: File): File {
        val archive = projectDir.resolve("fake-node.zip")
        ZipArchiveOutputStream(archive).use { zip ->
            val nodeRoot = "node-v26.3.0-test"
            zip.addDirectory("$nodeRoot/bin/")
            zip.addDirectory("$nodeRoot/lib/node_modules/npm/bin/")
            zip.addFile("$nodeRoot/lib/node_modules/npm/bin/npm-cli.js", "#!/bin/sh\n")
            zip.addSymlink("$nodeRoot/bin/npm", "../lib/node_modules/npm/bin/npm-cli.js")
        }
        return archive
    }

    private fun ZipArchiveOutputStream.addDirectory(name: String) {
        val entry = ZipArchiveEntry(name)
        entry.unixMode = UnixStat.DIR_FLAG or UnixStat.DEFAULT_DIR_PERM
        putArchiveEntry(entry)
        closeArchiveEntry()
    }

    private fun ZipArchiveOutputStream.addFile(name: String, content: String) {
        val entry = ZipArchiveEntry(name)
        entry.unixMode = UnixStat.FILE_FLAG or UnixStat.DEFAULT_FILE_PERM
        putArchiveEntry(entry)
        write(content.toByteArray(UTF_8))
        closeArchiveEntry()
    }

    private fun ZipArchiveOutputStream.addSymlink(name: String, target: String) {
        val entry = ZipArchiveEntry(name)
        entry.unixMode = UnixStat.LINK_FLAG or UnixStat.DEFAULT_LINK_PERM
        putArchiveEntry(entry)
        write(target.toByteArray(UTF_8))
        closeArchiveEntry()
    }

    private fun assertNpmSymlink(npm: File) {
        assertThat(Files.isSymbolicLink(npm.toPath()))
            .`as`("${npm.absolutePath} should be a symbolic link")
            .isTrue()
        assertThat(Files.readSymbolicLink(npm.toPath()).toString())
            .`as`("${npm.absolutePath} symbolic link target")
            .isEqualTo("../lib/node_modules/npm/bin/npm-cli.js")
    }

    private fun runner(projectDir: File): GradleRunner = GradleRunner.create()
        .withProjectDir(projectDir)
        .withGradleVersion(GradleTestVersion.current())
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
        GradleRunnerRule.runBuild(projectDir, GradleTestVersion.current(), commonArgs(*tasks)) {
            runner(projectDir).withArguments(commonArgs(*tasks)).build()
        }
}
