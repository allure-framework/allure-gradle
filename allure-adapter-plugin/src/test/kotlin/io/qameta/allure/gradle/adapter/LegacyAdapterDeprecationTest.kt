package io.qameta.allure.gradle.adapter

import io.qameta.allure.Allure
import io.qameta.allure.gradle.rule.GradleRunnerRule
import io.qameta.allure.gradle.rule.GradleTestVersion
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.io.File

class LegacyAdapterDeprecationTest {
    @TempDir
    lateinit var tempDir: File

    @ParameterizedTest(name = "SDK 2 retains {0} with migration guidance")
    @CsvSource(
        "cucumber4-jvm,cucumber4Jvm,Cucumber 7,cucumber7Jvm",
        "cucumber5-jvm,cucumber5Jvm,Cucumber 7,cucumber7Jvm",
        "cucumber6-jvm,cucumber6Jvm,Cucumber 7,cucumber7Jvm",
        "jbehave,jbehave,JBehave 5,jbehave5"
    )
    fun `legacy adapters warn once and still resolve with SDK 2`(
        fixture: String, adapter: String, framework: String, replacement: String
    ) {
        val runner = prepare(fixture)
        val result = runner.run("writeResolvedArtifacts")

        assertThat(warnings(result.output)).singleElement().asString()
            .contains("Adapter '$adapter' is deprecated", "Allure Java 2.x", framework, "frameworks.$replacement")
        assertThat(resolvedArtifacts(runner)).contains("io.qameta.allure:allure-$fixture:2.35.5")
    }

    @Test
    fun `multiple matching framework versions warn only once per adapter`() {
        val runner = prepare("cucumber6-jvm")
        runner.projectDir.resolve("build.gradle").appendText(
            """

            configurations { olderCucumber }
            dependencies { olderCucumber 'io.cucumber:cucumber-core:6.10.2' }
            tasks.register('writeOlderArtifacts') {
                doLast {
                    file('build/olderArtifacts.txt').text = configurations.olderCucumber.resolvedConfiguration
                        .resolvedArtifacts.collect { "${'$'}{it.moduleVersion.id.group}:${'$'}{it.name}:${'$'}{it.moduleVersion.id.version}" }
                        .sort().join('\n')
                }
            }
            """.trimIndent()
        )
        val result = runner.run("writeResolvedArtifacts", "writeOlderArtifacts")
        val older = runner.projectDir.resolve("build/olderArtifacts.txt").readText()
        Allure.attachment("Older Cucumber runtime", "text/plain", older)

        assertThat(warnings(result.output)).singleElement().asString().contains("Adapter 'cucumber6Jvm' is deprecated")
        assertThat(resolvedArtifacts(runner))
            .contains("io.cucumber:cucumber-core:6.11.0", "io.qameta.allure:allure-cucumber6-jvm:2.35.5")
        assertThat(older).contains("io.cucumber:cucumber-core:6.10.2", "io.qameta.allure:allure-cucumber6-jvm:2.35.5")
    }

    @ParameterizedTest(name = "SDK 2 override via {0}")
    @ValueSource(strings = [
        "cucumber4Jvm { adapterVersion = '2.35.5' }",
        "maybeCreate('cucumber4Jvm').adapterVersion = '2.35.5'",
        "cucumberJvm(4).adapterVersion = '2.35.5'"
    ])
    fun `legacy configuration paths honor an SDK 2 override`(configuration: String) {
        val runner = prepare("cucumber4-jvm", "3.0.0")
        runner.projectDir.resolve("build.gradle").appendText("\nallure.adapter.frameworks { $configuration }\n")
        val result = runner.run("writeResolvedArtifacts")

        assertThat(warnings(result.output)).singleElement().asString().contains("Adapter 'cucumber4Jvm' is deprecated")
        assertThat(resolvedArtifacts(runner)).contains("io.qameta.allure:allure-cucumber4-jvm:2.35.5")
    }

    @Test
    fun `SDK 3 overrides still fail with migration guidance`() {
        val runner = prepare("cucumber4-jvm")
        runner.projectDir.resolve("build.gradle").appendText(
            "\nallure.adapter.frameworks.cucumber4Jvm.adapterVersion = '3.0.0'\n"
        )
        val result = GradleRunnerRule.runBuild(
            runner.projectDir, GradleTestVersion.current(), listOf("writeResolvedArtifacts")
        ) { runner.newRunner("writeResolvedArtifacts").buildAndFail() }

        assertThat(result.output).contains("Allure Java 3.0.0 cannot configure cucumber4Jvm", "use Cucumber 7 for 3.x")
        assertThat(warnings(result.output)).isEmpty()
    }

    @ParameterizedTest(name = "Disabled {1} remains quiet")
    @CsvSource(
        "cucumber4-jvm,cucumber4Jvm", "cucumber5-jvm,cucumber5Jvm",
        "cucumber6-jvm,cucumber6Jvm", "jbehave,jbehave"
    )
    fun `disabled legacy adapters do not warn or validate their version`(fixture: String, adapter: String) {
        val runner = prepare(fixture)
        runner.projectDir.resolve("build.gradle").appendText(
            """

            allure.adapter.frameworks {
                $adapter {
                    adapterVersion = '42.0'
                    enabled = false
                }
            }
            """.trimIndent()
        )
        val result = runner.run("writeResolvedArtifacts")

        assertThat(warnings(result.output)).isEmpty()
        assertThat(resolvedArtifacts(runner)).doesNotContain("io.qameta.allure:")
    }

    @Test
    fun `unused legacy registrations stay quiet alongside a supported SDK 3 adapter`() {
        val runner = prepare("jupiter", "3.0.0")
        runner.projectDir.resolve("build.gradle").appendText(
            """

            allure.adapter.frameworks {
                jupiter
                maybeCreate('cucumber4Jvm').adapterVersion = '42.0'
                maybeCreate('cucumber5Jvm').adapterVersion = '42.0'
                maybeCreate('cucumber6Jvm').adapterVersion = '42.0'
                maybeCreate('jbehave').adapterVersion = '42.0'
            }
            """.trimIndent()
        )
        val result = runner.run("writeResolvedArtifacts")

        assertThat(warnings(result.output)).isEmpty()
        assertThat(resolvedArtifacts(runner)).contains("io.qameta.allure:allure-jupiter:3.0.0")
    }

    @Test
    fun `configuration without framework resolution does not warn`() {
        val result = prepare("cucumber4-jvm").run("help")
        assertThat(warnings(result.output)).isEmpty()
    }

    @Test
    fun `Kotlin DSL reports all four deprecations while preserving the configurations`() {
        val runner = GradleRunnerRule().rootDir(tempDir).version(GradleTestVersion.current())
            .project("src/it/adapter-legacy-deprecations-kotlin").prepare()
        val result = runner.run("writeLegacyConfigurations")
        val compilerWarnings = result.output.lines().filter { it.contains("is deprecated") }
        val configurations = runner.projectDir.resolve("build/legacy-configurations.txt").readText()
        Allure.attachment("Legacy configurations", "text/plain", configurations)

        assertThat(compilerWarnings).anySatisfy {
            assertThat(it).contains("cucumber4Jvm", "Allure Java 2.x", "Cucumber 7", "cucumber7Jvm")
        }.anySatisfy {
            assertThat(it).contains("cucumber5Jvm", "Allure Java 2.x", "Cucumber 7", "cucumber7Jvm")
        }.anySatisfy {
            assertThat(it).contains("cucumber6Jvm", "Allure Java 2.x", "Cucumber 7", "cucumber7Jvm")
        }.anySatisfy {
            assertThat(it).contains("jbehave", "Allure Java 2.x", "JBehave 5", "jbehave5")
        }
        assertThat(configurations).isEqualTo("cucumber4Jvm,cucumber5Jvm,cucumber6Jvm,jbehave")
        assertThat(warnings(result.output)).isEmpty()
    }

    private fun prepare(fixture: String, sdk: String = "2.35.5"): GradleRunnerRule = GradleRunnerRule()
        .rootDir(tempDir).version(GradleTestVersion.current())
        .project("src/it/adapter-resolution-$fixture").prepare().also {
            it.projectDir.resolve("build.gradle").appendText("\nallure.adapter.allureJavaVersion = '$sdk'\n")
        }

    private fun warnings(output: String) = output.lines().filter { it.startsWith("allure-gradle: Adapter '") }

    private fun resolvedArtifacts(runner: GradleRunnerRule): String =
        runner.projectDir.resolve("build/resolvedArtifacts.txt").readText().also {
            Allure.attachment("Resolved test runtime", "text/plain", it)
        }
}
