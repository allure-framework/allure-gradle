package io.qameta.allure.gradle.report.tasks

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AllureServeTest {
    @Test
    fun `command line keeps a spaced Windows results path as a single argument`() {
        val commandLine = buildAllureCommandLine(
            allureExecutable = """C:\Users\User with Space\allure\bin\allure.bat""",
            allureArgs = listOf(
                "serve",
                "--host",
                "127.0.0.1",
                """C:\Users\User with Space\project\build\allure-results"""
            ),
            handleQuoting = true
        )

        assertThat(commandLine.arguments).containsExactly(
            "serve",
            "--host",
            "127.0.0.1",
            "\"C:\\Users\\User with Space\\project\\build\\allure-results\""
        )
    }

    @Test
    fun `command line leaves Unix paths raw when quoting is disabled`() {
        val commandLine = buildAllureCommandLine(
            allureExecutable = "/tmp/allure/bin/allure",
            allureArgs = listOf(
                "serve",
                "/tmp/report project/build/allure-results"
            ),
            handleQuoting = false
        )

        assertThat(commandLine.arguments).containsExactly(
            "serve",
            "/tmp/report project/build/allure-results"
        )
    }
}
