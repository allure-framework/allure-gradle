package io.qameta.allure.gradle.base

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Usage
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.named

/**
 * The plugin adds [AllureExtension] extension which is used by most of the other Allure plugins.
 */
open class AllureBasePlugin : Plugin<Project> {
    companion object {
        const val PLUGIN_NAME = "io.qameta.allure-base"
    }

    override fun apply(target: Project): Unit = target.run {
        // TODO: migrate to precompiled script plugin once Gradle 6.0 could be the minimal supported Gradle version
        // See https://docs.gradle.org/current/userguide/custom_plugins.html#sec:precompiled_plugins
        extensions.create<AllureExtension>(AllureExtension.NAME)

        configurations.register("allureWorkaroundGradleBug") {
            isCanBeConsumed = true
            isCanBeResolved = false
            isVisible = false
            description =
                "This configuration workarounds Gradle's <<None of the consumable configurations have attributes>> error"
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, objects.named("please_do_not_use_this"))
            }
        }
    }
}
