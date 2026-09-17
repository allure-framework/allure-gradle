package io.qameta.allure.gradle.adapter

import io.qameta.allure.Allure
import io.qameta.allure.gradle.rule.GradleRunnerRule
import io.qameta.allure.gradle.rule.GradleTestVersion
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.io.File

class JupiterConfigurationTest {
    @TempDir
    lateinit var tempDir: File

    @ParameterizedTest(name = "{0} DSL shares Jupiter configuration with SDK {1}")
    @CsvSource(
        "kotlin,2.34.0,junit5", "groovy,2.34.0,junit5",
        "kotlin,2.35.0,jupiter", "groovy,2.35.0,jupiter",
        "kotlin,3.0.0,jupiter", "groovy,3.0.0,jupiter"
    )
    fun `junit5 is an alias for the single Jupiter configuration`(dsl: String, sdk: String, module: String) {
        val runner = GradleRunnerRule().rootDir(tempDir).version(GradleTestVersion.current())
            .project("src/it/adapter-jupiter-alias-$dsl").prepare()
        runner.run("writeJupiterConfiguration", "-Psdk=$sdk")
        val configuration = runner.projectDir.resolve("build/jupiter-configuration.txt").readText()
        Allure.attachment("Jupiter configuration", "text/plain", configuration)
        assertThat(configuration).isEqualTo(
            """
            name=jupiter
            sameInstance=true
            namedAlias=true
            adapters=jupiter
            module=$module
            dependency=io.qameta.allure:allure-$module:$sdk
            enabled=false
            listeners=false
            """.trimIndent()
        )
    }
}
