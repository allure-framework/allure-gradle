package io.qameta.allure.gradle.report.tasks

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AllureServeTest {
    @Test
    fun `windows command quotes batch path and args for cmd`() {
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
            "\"\"C:\\Users\\User with Space\\allure\\bin\\allure.bat\" \"serve\" \"--host\" \"127.0.0.1\" \"C:\\Users\\User with Space\\project\\build\\allure-results\"\""
        )
    }

    @Test
    fun `windows command escapes percent signs and embedded quotes`() {
        assertThat(
            buildWindowsCommandLine(
                allureExecutable = """C:\tools\allure.bat""",
                allureArgs = listOf(
                    """100% ready""",
                    "has \"quotes\""
                )
            )
        ).isEqualTo(
            "\"\"C:\\tools\\allure.bat\" \"100%%cd:~,% ready\" \"has \"\"quotes\"\"\"\""
        )
    }

    @Test
    fun `windows command rejects multiline args`() {
        assertThat(
            kotlin.runCatching {
                escapeWindowsCmdArg("bad\narg")
            }.exceptionOrNull()
        ).isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Invalid character in argument")
    }
}
