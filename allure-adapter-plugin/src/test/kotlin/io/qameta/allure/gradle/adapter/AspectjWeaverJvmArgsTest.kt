package io.qameta.allure.gradle.adapter

import io.qameta.allure.gradle.rule.GradleRunnerRule
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import org.junit.jupiter.params.provider.Arguments.arguments

class AspectjWeaverJvmArgsTest {
    @TempDir
    lateinit var tempDir: File

    companion object {
        @JvmStatic
        fun data() = listOf(
            arguments("9.0.0", "src/it/adapter-aspectj-weaver-disabled-kts", false),
            arguments("8.14.3", "src/it/adapter-aspectj-weaver-disabled-kts", false),
            arguments("8.11.1", "src/it/adapter-aspectj-weaver-disabled-kts", false),
            arguments("9.0.0", "src/it/adapter-aspectj-weaver-enabled-kts", true),
            arguments("8.14.3", "src/it/adapter-aspectj-weaver-enabled-kts", true),
            arguments("8.11.1", "src/it/adapter-aspectj-weaver-enabled-kts", true),
        )
    }

    @ParameterizedTest(name = "{1} [{0}]")
    @MethodSource("data")
    fun `aspectj weaver flag controls javaagent wiring`(version: String, project: String, expectedJavaAgent: Boolean) {
        val gradleRunner = GradleRunnerRule()
            .rootDir(tempDir)
            .version(version)
            .project(project)
            .tasks("test")
            .build()

        assertThat(gradleRunner.buildResult.task(":test")?.outcome)
            .`as`("test task outcome")
            .isEqualTo(TaskOutcome.SUCCESS)

        val jvmArgs = gradleRunner.projectDir.resolve("build/jvmArgs.txt")
        assertThat(jvmArgs)
            .`as`("captured JVM args")
            .exists()

        val argsText = jvmArgs.readText()
        if (expectedJavaAgent) {
            assertThat(argsText)
                .`as`("AspectJ javaagent")
                .contains("-javaagent:")
        } else {
            assertThat(argsText)
                .`as`("AspectJ javaagent")
                .doesNotContain("-javaagent:")
        }

        val resultsDir = gradleRunner.projectDir.resolve("build/allure-results")
        assertThat(resultsDir)
            .`as`("Allure results directory")
            .isNotEmptyDirectory()
        assertThat(resultsDir.listFiles())
            .`as`("Allure results test cases")
            .filteredOn { file -> file.name.endsWith("result.json") }
            .hasSize(1)
    }
}
