package io.qameta.allure.gradle.report.tasks

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AllureServeTest {
    @Test
    fun `windows command calls executable with raw args`() {
        assertThat(
            buildWindowsCommand(
                allureExecutable = """C:\Users\User with Space\allure\bin\allure.bat""",
                allureArgs = listOf(
                    "serve",
                    "--host",
                    "127.0.0.1",
                    """C:\Users\User with Space\project\build\allure-results"""
                )
            )
        ).containsExactly(
            "cmd.exe",
            "/d",
            "/c",
            "call",
            """C:\Users\User with Space\allure\bin\allure.bat""",
            "serve",
            "--host",
            "127.0.0.1",
            """C:\Users\User with Space\project\build\allure-results"""
        )
    }

    @Test
    fun `windows command keeps args unchanged`() {
        assertThat(
            buildWindowsCommand(
                allureExecutable = """C:\tools\allure.bat""",
                allureArgs = listOf(
                    """100% ready""",
                    "has \"quotes\""
                )
            )
        ).containsExactly(
            "cmd.exe",
            "/d",
            "/c",
            "call",
            """C:\tools\allure.bat""",
            """100% ready""",
            "has \"quotes\""
        )
    }
}
