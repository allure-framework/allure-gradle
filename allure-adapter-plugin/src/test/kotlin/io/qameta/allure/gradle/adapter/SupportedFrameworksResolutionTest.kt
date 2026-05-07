package io.qameta.allure.gradle.adapter

import io.qameta.allure.gradle.rule.GradleRunnerRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import org.junit.jupiter.params.provider.Arguments.arguments

class SupportedFrameworksResolutionTest {
    @TempDir
    lateinit var tempDir: File

    companion object {
        private const val ALLURE_JAVA_VERSION = "2.33.0"

        @JvmStatic
        fun data() = listOf(
            arguments("src/it/adapter-resolution-junit4", "io.qameta.allure:allure-junit4:$ALLURE_JAVA_VERSION"),
            arguments("src/it/adapter-resolution-junit5", "io.qameta.allure:allure-junit5:$ALLURE_JAVA_VERSION"),
            arguments("src/it/adapter-resolution-junit-platform", "io.qameta.allure:allure-junit-platform:$ALLURE_JAVA_VERSION"),
            arguments("src/it/adapter-resolution-testng", "io.qameta.allure:allure-testng:$ALLURE_JAVA_VERSION"),
            arguments("src/it/adapter-resolution-assertj", "io.qameta.allure:allure-assertj:$ALLURE_JAVA_VERSION"),
            arguments("src/it/adapter-resolution-spock", "io.qameta.allure:allure-spock2:$ALLURE_JAVA_VERSION"),
            arguments("src/it/adapter-resolution-cucumber4-jvm", "io.qameta.allure:allure-cucumber4-jvm:$ALLURE_JAVA_VERSION"),
            arguments("src/it/adapter-resolution-cucumber5-jvm", "io.qameta.allure:allure-cucumber5-jvm:$ALLURE_JAVA_VERSION"),
            arguments("src/it/adapter-resolution-cucumber6-jvm", "io.qameta.allure:allure-cucumber6-jvm:$ALLURE_JAVA_VERSION"),
            arguments("src/it/adapter-resolution-cucumber7-jvm", "io.qameta.allure:allure-cucumber7-jvm:$ALLURE_JAVA_VERSION"),
            arguments("src/it/adapter-resolution-jbehave", "io.qameta.allure:allure-jbehave:$ALLURE_JAVA_VERSION"),
            arguments("src/it/adapter-resolution-jbehave5", "io.qameta.allure:allure-jbehave5:$ALLURE_JAVA_VERSION"),
            arguments("src/it/adapter-resolution-karate", "io.qameta.allure:allure-karate:$ALLURE_JAVA_VERSION"),
            arguments("src/it/adapter-resolution-scalatest-212", "io.qameta.allure:allure-scalatest_2.12:$ALLURE_JAVA_VERSION"),
            arguments("src/it/adapter-resolution-scalatest-213", "io.qameta.allure:allure-scalatest_2.13:$ALLURE_JAVA_VERSION"),
        )
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("data")
    fun `resolved runtime classpath should contain the expected adapter`(project: String, expectedDependency: String) {
        val gradleRunner = GradleRunnerRule()
            .rootDir(tempDir)
            .version("9.4.1")
            .project(project)
            .tasks("writeResolvedArtifacts")
            .build()

        val resolved = gradleRunner.projectDir.resolve("build/resolvedArtifacts.txt").readText()

        assertThat(resolved)
            .contains(expectedDependency)
            .doesNotContain("io.qameta.allure:allure-cucumber2-jvm:")
            .doesNotContain("io.qameta.allure:allure-cucumber3-jvm:")

        if (project.contains("spock")) {
            assertThat(resolved).doesNotContain("io.qameta.allure:allure-spock:")
        }
    }
}
