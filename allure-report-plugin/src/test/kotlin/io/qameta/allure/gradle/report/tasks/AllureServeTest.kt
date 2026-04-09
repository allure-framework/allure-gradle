package io.qameta.allure.gradle.report.tasks

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AllureServeTest {
    @Test
    fun `windows command uses call for batch paths with spaces`() {
        val command = buildWindowsCommand(
            allureExecutable = """C:\Users\User with Space\allure\bin\allure.bat""",
            allureArgs = listOf(
                "serve",
                "--host",
                "127.0.0.1",
                """C:\Users\User with Space\project\build\allure-results"""
            )
        )

        assertThat(command).containsExactly(
            "cmd",
            "/c",
            "call",
            """C:\Users\User with Space\allure\bin\allure.bat""",
            "serve",
            "--host",
            "127.0.0.1",
            """C:\Users\User with Space\project\build\allure-results"""
        )
    }
}
