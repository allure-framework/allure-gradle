package io.qameta.allure.gradle.adapter

import io.qameta.allure.gradle.rule.GradleRunnerRule
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class AspectjWeaverJvmArgsTest {
    @Rule
    @JvmField
    val gradleRunner = GradleRunnerRule()
        .version { version }
        .project { project }
        .tasks("test")

    @Parameterized.Parameter(0)
    lateinit var version: String

    @Parameterized.Parameter(1)
    lateinit var project: String

    @Parameterized.Parameter(2)
    @JvmField
    var expectedJavaAgent: Boolean = false

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{1} [{0}]")
        fun data() = listOf<Array<Any>>(
            arrayOf("9.0.0", "src/it/adapter-aspectj-weaver-disabled-kts", false),
            arrayOf("8.14.3", "src/it/adapter-aspectj-weaver-disabled-kts", false),
            arrayOf("8.11.1", "src/it/adapter-aspectj-weaver-disabled-kts", false),
            arrayOf("9.0.0", "src/it/adapter-aspectj-weaver-enabled-kts", true),
            arrayOf("8.14.3", "src/it/adapter-aspectj-weaver-enabled-kts", true),
            arrayOf("8.11.1", "src/it/adapter-aspectj-weaver-enabled-kts", true)
        )
    }

    @Test
    fun `aspectj weaver flag controls javaagent wiring`() {
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
