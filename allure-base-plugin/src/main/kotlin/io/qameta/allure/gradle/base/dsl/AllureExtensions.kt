package io.qameta.allure.gradle.base.dsl

import io.qameta.allure.gradle.base.AllureExtension
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtensionContainer

val AllureExtension.extensions: ExtensionContainer get() = (this as ExtensionAware).extensions
