package io.qameta.allure.gradle.adapter

import io.qameta.allure.Allure
import io.qameta.allure.gradle.rule.GradleRunnerRule
import io.qameta.allure.gradle.rule.GradleTestVersion
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.io.File

class AllureJava3ResolutionTest {
    @TempDir
    lateinit var tempDir: File

    @ParameterizedTest(name = "SDK 3 resolves {0}")
    @CsvSource(
        "junit4,allure-junit4", "junit5,allure-jupiter", "junit-platform,allure-junit-platform",
        "testng,allure-testng", "assertj,allure-assertj", "spock,allure-spock2",
        "cucumber7-jvm,allure-cucumber7-jvm", "jbehave5,allure-jbehave5",
        "scalatest-212,allure-scalatest_2.12", "scalatest-213,allure-scalatest_2.13"
    )
    fun `supported frameworks resolve published SDK 3 adapters`(fixture: String, module: String) {
        val runner = prepare(fixture)
        runner.run("writeResolvedArtifacts")
        val resolved = resolvedArtifacts(runner)
        assertThat(resolved).contains("io.qameta.allure:$module:3.0.0")
            .doesNotContain("io.qameta.allure:allure-junit5:")
    }

    @ParameterizedTest(name = "SDK 3 detects {0}")
    @CsvSource(
        "karate,com.intuit.karate:karate-core:1.4.1,io.karatelabs:karate-core:2.1.2,allure-karate",
        "scalatest-213,org.scalatest:scalatest_2.13:3.2.20,org.scalatest:scalatest_3:3.2.20,allure-scalatest_3"
    )
    fun `new framework coordinates activate their adapter`(
        fixture: String, old: String, new: String, module: String
    ) {
        val runner = prepare(fixture)
        val build = runner.projectDir.resolve("build.gradle")
        build.writeText(build.readText().replace(old, new) + "\njava.targetCompatibility = JavaVersion.VERSION_21\n")
        runner.run("writeResolvedArtifacts")
        assertThat(resolvedArtifacts(runner)).contains("io.qameta.allure:$module:3.0.0")
    }

    @ParameterizedTest(name = "SDK 3 diagnoses {0}")
    @CsvSource(
        "cucumber4-jvm,Cucumber 4–6 adapters", "cucumber5-jvm,Cucumber 4–6 adapters",
        "cucumber6-jvm,Cucumber 4–6 adapters", "jbehave,JBehave 4", "karate,Karate 2"
    )
    fun `removed frameworks report migration guidance`(fixture: String, reason: String) {
        val runner = prepare(fixture)
        assertThat(failure(runner)).contains("Allure Java 3.0.0 cannot configure", reason, "adapterVersion")
    }

    @ParameterizedTest(name = "SDK 3 rejects TestNG {0}")
    @CsvSource("6.14.3", "7.9.0")
    fun `incompatible TestNG fails before service loading`(testngVersion: String) {
        val runner = prepare("testng")
        val build = runner.projectDir.resolve("build.gradle")
        build.writeText(build.readText().replace("7.12.0", testngVersion))
        assertThat(failure(runner)).contains("Allure Java 3 requires TestNG 7.10.0 or newer")
    }

    @ParameterizedTest(name = "global {0}, Jupiter override {1}")
    @CsvSource("2.35.5,3.0.0,allure-jupiter", "3.0.0,2.35.5,allure-junit5")
    fun `artifact mapping uses the effective adapter version`(global: String, override: String, module: String) {
        val runner = prepare("junit5", global)
        runner.projectDir.resolve("build.gradle").appendText(
            """

            allure.adapter.frameworks.junit5.adapterVersion.set(providers.provider { '$override' })
            tasks.named('writeResolvedArtifacts') {
                doLast {
                    file('build/adapterDependency.txt').text = allure.adapter.frameworks.junit5.adapterDependency.get()
                }
            }
            """.trimIndent()
        )
        runner.run("writeResolvedArtifacts")
        assertThat(runner.projectDir.resolve("build/adapterDependency.txt"))
            .hasContent("io.qameta.allure:$module:$override")
        // In 2.35.5 junit5 is a POM relocation to jupiter, so the resolved jar uses the new name in both cases.
        assertThat(resolvedArtifacts(runner)).contains("io.qameta.allure:allure-jupiter:$override")
    }

    @Test
    fun `disabled unsupported adapter does not reject the framework`() {
        val runner = prepare("cucumber4-jvm")
        runner.projectDir.resolve("build.gradle").appendText(
            "\nallure.adapter.frameworks.cucumber4Jvm.enabled = false\n"
        )
        runner.run("writeResolvedArtifacts")
        assertThat(resolvedArtifacts(runner)).doesNotContain("io.qameta.allure:allure-cucumber4-jvm:")
    }

    @ParameterizedTest(name = "SDK 2 rejects {0}")
    @CsvSource(
        "karate,com.intuit.karate:karate-core:1.4.1,io.karatelabs:karate-core:2.1.2,Karate 2 requires Allure Java 3.x",
        "scalatest-213,org.scalatest:scalatest_2.13:3.2.20,org.scalatest:scalatest_3:3.2.20,ScalaTest for Scala 3 requires Allure Java 3.x"
    )
    fun `new frameworks do not silently receive SDK 2 adapters`(fixture: String, old: String, new: String, reason: String) {
        val runner = prepare(fixture, "2.35.5")
        val build = runner.projectDir.resolve("build.gradle")
        build.writeText(build.readText().replace(old, new) + "\njava.targetCompatibility = JavaVersion.VERSION_21\n")
        assertThat(failure(runner)).contains(reason)
    }

    private fun prepare(fixture: String, sdk: String = "3.0.0"): GradleRunnerRule = GradleRunnerRule()
        .rootDir(tempDir).version(GradleTestVersion.current())
        .project("src/it/adapter-resolution-$fixture").prepare().also {
            it.projectDir.resolve("build.gradle").appendText("\nallure.adapter.allureJavaVersion = '$sdk'\n")
        }

    private fun resolvedArtifacts(runner: GradleRunnerRule): String =
        runner.projectDir.resolve("build/resolvedArtifacts.txt").readText().also {
            Allure.addAttachment("Resolved test runtime", "text/plain", it, ".txt")
        }

    private fun failure(runner: GradleRunnerRule): String = GradleRunnerRule.runBuild(
        runner.projectDir, GradleTestVersion.current(), listOf("writeResolvedArtifacts")
    ) { runner.newRunner("writeResolvedArtifacts").buildAndFail() }.output
}
