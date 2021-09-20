package io.qameta.allure.gradle.adapter.autoconfigure

import org.gradle.api.Action
import org.gradle.api.artifacts.DependencyMetadata
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.specs.Spec

interface AutoconfigureRuleBuilder {
    /**
     * By default the rule would apply to all modules specified in [triggerDependency].
     * This method sets a filter to distinguish adapters like `cucumber2-jvm` and `cucumber3-jvm`.
     * Note: each call overwrites the filter.
     */
    fun matching(predicate: Spec<in ModuleVersionIdentifier>)

    /**
     * Adds a dependency to the Maven's `compile` and `runtime` scopes.
     */
    fun compileAndRuntime(dependencyNotation: Any)

    /**
     * Adds a dependency to the Maven's `compile` and `runtime` scopes.
     */
    fun compileAndRuntime(dependencyNotation: Any, configureAction: Action<in DependencyMetadata<*>>?)

    /**
     * Adds a dependency to the Maven's `compile` scope only.
     */
    fun compileOnly(dependencyNotation: Any)

    /**
     * Adds a dependency to the Maven's `compile` scope only.
     */
    fun compileOnly(dependencyNotation: Any, configureAction: Action<in DependencyMetadata<*>>?)

    /**
     * Adds a dependency to the Maven's `runtime` scope only.
     */
    fun runtimeOnly(dependencyNotation: Any)

    /**
     * Adds a dependency to the Maven's `runtime` scope only.
     */
    fun runtimeOnly(dependencyNotation: Any, configureAction: Action<in DependencyMetadata<*>>? = null)
}
