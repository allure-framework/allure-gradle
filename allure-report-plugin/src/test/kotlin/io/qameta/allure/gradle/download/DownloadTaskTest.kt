package io.qameta.allure.gradle.download

import commandline
import io.qameta.allure.gradle.base.AllureExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.gradle.kotlin.dsl.*
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

class DownloadTaskTest {
    @Test
    fun `default version should target Allure 3`() {
        ProjectBuilder.builder().build().run {
            apply(plugin = "io.qameta.allure-report")
            assertThat(the<AllureExtension>().version.get())
                .`as`("default Allure runtime version")
                .isEqualTo(AllureExtension.DEFAULT_VERSION)
        }
    }

    @Test
    fun `basic Allure 2 download`() {
        ProjectBuilder.builder().build().run {
            apply(plugin = "io.qameta.allure-download")
            val allureCommandline by configurations

            repositories {
                mavenCentral()
            }

            configure<AllureExtension> {
                version.set(AllureExtension.LAST_ALLURE_2_VERSION)
            }

            assertThat(allureCommandline.singleFile).`as`("allure-commandline binary")
                .isNotEmpty
        }
    }

    @Test
    fun `custom url`() {
        ProjectBuilder.builder().build().run {
            apply(plugin = "io.qameta.allure-report")
            val allureCommandline by configurations

            val customUrl = "https://download-test-[group].test/[module]-custom-[version].zip"

            configure<AllureExtension> {
                version.set("2.42.0")
                commandline.apply {
                    downloadUrlPattern.set(customUrl)
                }
            }

            assertThatThrownBy {
                allureCommandline.singleFile
            }.`as`("Custom URL configured as %s", customUrl)
                .hasStackTraceContaining("https://download-test-custom.io.qameta.allure.test/allure-commandline-custom-2.42.0.zip")
        }
    }

    @Test
    fun `illegal pattern in url`() {
        ProjectBuilder.builder().build().run {
            apply(plugin = "io.qameta.allure-report")
            val allureCommandline by configurations

            val customUrl = "https://localhost/[illegal-for-test].zip"

            configure<AllureExtension> {
                version.set("2.42.0")
                commandline.apply {
                    downloadUrlPattern.set("https://localhost/[illegal-for-test].zip")
                }
            }

            assertThatThrownBy {
                allureCommandline.singleFile
            }.`as`("Custom URL configured as %s", customUrl)
                .hasMessageContaining(
                    "Unexpected pattern [illegal-for-test] detected in allure.commandline.downloadUrlPattern " +
                            "https://localhost/[illegal-for-test].zip. The following patterns are supported: " +
                            "[organization] = custom.io.qameta.allure, " +
                            "[group] = custom.io.qameta.allure, " +
                            "[module] = allure-commandline, " +
                            "[version] = 2.42.0"
                )
        }
    }
}
