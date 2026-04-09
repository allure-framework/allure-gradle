package io.qameta.allure.gradle.report.tasks

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.Test

class AllureServeTest {
    @Test
    fun `windows command delegates executable and args via environment variables`() {
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
            "cmd.exe",
            "/E:ON",
            "/F:OFF",
            "/V:OFF",
            "/d",
            "/s",
            "/c",
            buildWindowsCommandLine(
                allureExecutable = """C:\Users\User with Space\allure\bin\allure.bat""",
                allureArgs = listOf(
                    "serve",
                    "--host",
                    "127.0.0.1",
                    """C:\Users\User with Space\project\build\allure-results"""
                )
            )
        )
        assertThat(command.last()).isEqualTo(
            "call \"%ALLURE_CMDLINE_EXECUTABLE%\" \"%ALLURE_CMDLINE_ARG_0%\" \"%ALLURE_CMDLINE_ARG_1%\" \"%ALLURE_CMDLINE_ARG_2%\" \"%ALLURE_CMDLINE_ARG_3%\""
        )
    }

    @Test
    fun `windows command environment keeps raw argument values`() {
        assertThat(
            buildWindowsCommandEnvironment(
                allureExecutable = """C:\tools\allure.bat""",
                allureArgs = listOf(
                    """100% ready""",
                    "has \"quotes\""
                )
            )
        ).containsExactly(
            entry("ALLURE_CMDLINE_EXECUTABLE", """C:\tools\allure.bat"""),
            entry("ALLURE_CMDLINE_ARG_0", """100% ready"""),
            entry("ALLURE_CMDLINE_ARG_1", "has \"quotes\"")
        )
    }

    @Test
    fun `windows command rejects multiline args`() {
        assertThat(
            kotlin.runCatching {
                validateWindowsCommandValue("bad\narg")
            }.exceptionOrNull()
        ).isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Invalid character in argument")
    }
}
