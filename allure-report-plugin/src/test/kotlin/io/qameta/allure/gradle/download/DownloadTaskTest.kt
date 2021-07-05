package io.qameta.allure.gradle.download

import commandline
import io.qameta.allure.gradle.base.AllureExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.gradle.kotlin.dsl.*
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class DownloadTaskTest {
    @Test
    fun `basic download`() {
        ProjectBuilder.builder().build().run {
            apply(plugin = "io.qameta.allure-download")
            val allureCommandline by configurations

            // Repository is required to download allure-commandline
            repositories {
                mavenCentral()
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
                version.set("42.0")
                // sam-with-receiver does not work in IDEA :(
                commandline.apply {
                    // .test is reserved, see https://tools.ietf.org/html/rfc2606#section-2
                    downloadUrlPattern.set(customUrl)
                }
            }

            assertThatThrownBy {
                allureCommandline.singleFile
            }.`as`("Custom URL configured as %s", customUrl)
                .hasStackTraceContaining("https://download-test-custom.io.qameta.allure.test/allure-commandline-custom-42.0.zip")
        }
    }

    @Test
    fun `illegal pattern in url`() {

        ProjectBuilder.builder().build().run {
            apply(plugin = "io.qameta.allure-report")
            val allureCommandline by configurations

            val customUrl = "https://localhost/[illegal-for-test].zip"

            configure<AllureExtension> {
                version.set("42.0")
                // sam-with-receiver does not work in IDEA :(
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
                            "[version] = 42.0"
                )
        }
    }
}
