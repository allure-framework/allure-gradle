package io.qameta.allure.gradle.adapter

import io.qameta.allure.gradle.rule.GradleRunnerRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class AdaptersTest {
    @Rule
    @JvmField
    val gradleRunner = GradleRunnerRule()
        .version { version }
        .project { project }
        .tasks { tasks }


    @Parameterized.Parameter(0)
    lateinit var version: String

    @Parameterized.Parameter(1)
    lateinit var project: String

    @Parameterized.Parameter(2)
    lateinit var tasks: Array<String>

    @Parameterized.Parameter(3)
    lateinit var expected: String

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{1} [{0}]")
        fun getFrameworks() = listOf(
            arrayOf(
                "7.5.1",
                "src/it/adapter-junit5-spock-kts",
                arrayOf("printAdapters"),
                "[AdapterConfig{junit5}, AdapterConfig{spock}]"
            ),
            arrayOf(
                "7.0",
                "src/it/adapter-junit5-spock-kts",
                arrayOf("printAdapters"),
                "[AdapterConfig{junit5}, AdapterConfig{spock}]"
            ),
            arrayOf(
                "5.0",
                "src/it/adapter-junit5-spock-kts",
                arrayOf("printAdapters"),
                "[AdapterConfig{junit5}, AdapterConfig{spock}]"
            ),
            arrayOf(
                "7.5.1",
                "src/it/adapter-all",
                arrayOf("printAdapters"),
                "[AdapterConfig{cucumber2Jvm}, AdapterConfig{cucumber3Jvm}, AdapterConfig{cucumber4Jvm}, AdapterConfig{cucumber5Jvm}, AdapterConfig{cucumber6Jvm}, AdapterConfig{cucumberJvm}, AdapterConfig{junit4}, AdapterConfig{junit5}, AdapterConfig{spock}, AdapterConfig{testng}]"
            ),            arrayOf(
                "7.0",
                "src/it/adapter-all",
                arrayOf("printAdapters"),
                "[AdapterConfig{cucumber2Jvm}, AdapterConfig{cucumber3Jvm}, AdapterConfig{cucumber4Jvm}, AdapterConfig{cucumber5Jvm}, AdapterConfig{cucumber6Jvm}, AdapterConfig{cucumberJvm}, AdapterConfig{junit4}, AdapterConfig{junit5}, AdapterConfig{spock}, AdapterConfig{testng}]"
            ),
            arrayOf(
                "5.0",
                "src/it/adapter-all",
                arrayOf("printAdapters"),
                "[AdapterConfig{cucumber2Jvm}, AdapterConfig{cucumber3Jvm}, AdapterConfig{cucumber4Jvm}, AdapterConfig{cucumber5Jvm}, AdapterConfig{cucumber6Jvm}, AdapterConfig{cucumberJvm}, AdapterConfig{junit4}, AdapterConfig{junit5}, AdapterConfig{spock}, AdapterConfig{testng}]"
            )
        )
    }

    @Test
    fun `list of configured adapters changes on explicit adapter configuration`() {
        assertThat(gradleRunner.projectDir.resolve("build/printAdapters.txt"))
            .hasContent(expected)
    }
}
