package io.qameta.allure.gradle.adapter

import io.qameta.allure.gradle.rule.GradleRunnerRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import org.junit.jupiter.params.provider.Arguments.arguments

class AdaptersTest {
    @TempDir
    lateinit var tempDir: File

    companion object {
        @JvmStatic
        fun getFrameworks() = listOf(
            arguments(
                "9.0.0",
                "src/it/adapter-junit5-spock-kts",
                arrayOf("printAdapters"),
                "[AdapterConfig{junit5}, AdapterConfig{spock}]",
            ),
            arguments(
                "8.14.3",
                "src/it/adapter-junit5-spock-kts",
                arrayOf("printAdapters"),
                "[AdapterConfig{junit5}, AdapterConfig{spock}]",
            ),
            arguments(
                "8.11.1",
                "src/it/adapter-junit5-spock-kts",
                arrayOf("printAdapters"),
                "[AdapterConfig{junit5}, AdapterConfig{spock}]",
            ),
            arguments(
                "9.0.0",
                "src/it/adapter-all",
                arrayOf("printAdapters"),
                "[AdapterConfig{cucumber4Jvm}, AdapterConfig{cucumber5Jvm}, AdapterConfig{cucumber6Jvm}, AdapterConfig{cucumber7Jvm}, AdapterConfig{jbehave5}, AdapterConfig{jbehave}, AdapterConfig{junit4}, AdapterConfig{junit5}, AdapterConfig{junitPlatform}, AdapterConfig{karate}, AdapterConfig{scalatest}, AdapterConfig{spock}, AdapterConfig{testng}]",
            ),
            arguments(
                "8.11.1",
                "src/it/adapter-all",
                arrayOf("printAdapters"),
                "[AdapterConfig{cucumber4Jvm}, AdapterConfig{cucumber5Jvm}, AdapterConfig{cucumber6Jvm}, AdapterConfig{cucumber7Jvm}, AdapterConfig{jbehave5}, AdapterConfig{jbehave}, AdapterConfig{junit4}, AdapterConfig{junit5}, AdapterConfig{junitPlatform}, AdapterConfig{karate}, AdapterConfig{scalatest}, AdapterConfig{spock}, AdapterConfig{testng}]",
            ),
            arguments(
                "8.14.3",
                "src/it/adapter-all",
                arrayOf("printAdapters"),
                "[AdapterConfig{cucumber4Jvm}, AdapterConfig{cucumber5Jvm}, AdapterConfig{cucumber6Jvm}, AdapterConfig{cucumber7Jvm}, AdapterConfig{jbehave5}, AdapterConfig{jbehave}, AdapterConfig{junit4}, AdapterConfig{junit5}, AdapterConfig{junitPlatform}, AdapterConfig{karate}, AdapterConfig{scalatest}, AdapterConfig{spock}, AdapterConfig{testng}]",
            ),
        )
    }

    @ParameterizedTest(name = "{1} [{0}]")
    @MethodSource("getFrameworks")
    fun `list of configured adapters changes on explicit adapter configuration`(
        version: String,
        project: String,
        tasks: Array<String>,
        expected: String,
    ) {
        val gradleRunner = GradleRunnerRule()
            .rootDir(tempDir)
            .version(version)
            .project(project)
            .tasks(*tasks)
            .build()

        assertThat(gradleRunner.projectDir.resolve("build/printAdapters.txt"))
            .hasContent(expected)
    }
}
