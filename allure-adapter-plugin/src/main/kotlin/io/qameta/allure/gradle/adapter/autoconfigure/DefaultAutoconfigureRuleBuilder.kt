package io.qameta.allure.gradle.adapter.autoconfigure

import org.gradle.api.Action
import org.gradle.api.artifacts.DependencyMetadata
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.provider.Provider
import org.gradle.api.specs.Spec

class DefaultAutoconfigureRuleBuilder(
    val triggerDependency: String,
    val enabled: Provider<Boolean>
) : AutoconfigureRuleBuilder {
    private val deps = mutableMapOf<String, MutableList<DependencyDeclaration>>()
    private var predicate: Spec<in ModuleVersionIdentifier>? = null

    override fun matching(predicate: Spec<in ModuleVersionIdentifier>) {
        this.predicate = predicate
    }

    override fun compileAndRuntime(dependencyNotation: Any) = compileAndRuntime(dependencyNotation, null)

    override fun compileAndRuntime(dependencyNotation: Any, configureAction: Action<in DependencyMetadata<*>>?) {
        compileOnly(dependencyNotation, configureAction)
        runtimeOnly(dependencyNotation, configureAction)
    }

    override fun compileOnly(dependencyNotation: Any) = compileOnly(dependencyNotation, null)

    override fun compileOnly(dependencyNotation: Any, configureAction: Action<in DependencyMetadata<*>>?) {
        val dependencyDeclaration = DependencyDeclaration(dependencyNotation, configureAction)
        addDependencyToVariant("compile", dependencyDeclaration)
        addDependencyToVariant("apiElements", dependencyDeclaration)
    }

    override fun runtimeOnly(dependencyNotation: Any) = runtimeOnly(dependencyNotation, null)

    override fun runtimeOnly(dependencyNotation: Any, configureAction: Action<in DependencyMetadata<*>>?) {
        val dependencyDeclaration = DependencyDeclaration(dependencyNotation, configureAction)
        addDependencyToVariant("runtime", dependencyDeclaration)
        addDependencyToVariant("runtimeElements", dependencyDeclaration)
    }

    private fun addDependencyToVariant(variantName: String, dependencyDeclaration: DependencyDeclaration) {
        deps.getOrPut(variantName) { mutableListOf() } +=
            dependencyDeclaration
    }

    fun build(): AutoconfigureRule = SimpleRule(
        triggerDependency,
        {
            enabled.get() && predicate?.isSatisfiedBy(it) != false
        },
        deps.ifEmpty {
            throw IllegalStateException("Please add at least one dependency via .compile(..) or .runtime(..) method")
        }
    )
}
