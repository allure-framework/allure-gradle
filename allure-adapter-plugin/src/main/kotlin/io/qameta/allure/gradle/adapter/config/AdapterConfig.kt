package io.qameta.allure.gradle.adapter.config

import io.qameta.allure.gradle.base.AllureExtension
import io.qameta.allure.gradle.adapter.AllureAdapterExtension
import io.qameta.allure.gradle.adapter.autoconfigure.AutoconfigureRule
import io.qameta.allure.gradle.adapter.autoconfigure.AutoconfigureRuleBuilder
import io.qameta.allure.gradle.adapter.autoconfigure.DefaultAutoconfigureRuleBuilder
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.domainObjectSet
import javax.inject.Inject

open class AdapterConfig @Inject constructor(
    val name: String,
    objects: ObjectFactory,
    allureAdapterExtension: AllureAdapterExtension
) {
    /**
     * Configures `allure-java` version for the current adapter.
     * The value defaults to [AllureExtension.allureJavaVersion]
     */
    val adapterVersion = objects.property<String>()
        .convention(allureAdapterExtension.allureJavaVersion)

    @Deprecated(
        level = DeprecationLevel.WARNING,
        message = "Use adapterVersion",
        replaceWith = ReplaceWith("adapterVersion")
    )
    var version: String
        get() = adapterVersion.get()
        set(value) = adapterVersion.set(value)

    /**
     * By default, the adapter is enabled. This property allows deactivating the adapter.
     */
    val enabled = objects.property<Boolean>().convention(true)

    val autoconfigureListeners = objects.property<Boolean>()
        .convention(
            enabled.map { it && allureAdapterExtension.autoconfigureListeners.get() }
        )

    @Deprecated(
        level = DeprecationLevel.WARNING, message = "Use autoconfigureListeners",
        replaceWith = ReplaceWith("autoconfigureListeners")
    )
    var spiOff: Boolean
        get() = !autoconfigureListeners.get()
        set(value) = autoconfigureListeners.set(!value)

    /**
     * Autoconfigure listeners is available only for the subset of adapters only (e.g [AdapterHandlerScope.testng],
     * [AdapterHandlerScope.junit5])
     */
    val supportsAutoconfigureListeners = objects.property<Boolean>().convention(false)

    /**
     * Returns `true` if `META-INF/services` should be removed from the dependency.
     */
    internal val trimServicesFromJar =
        supportsAutoconfigureListeners.map { it && !autoconfigureListeners.get() }

    internal val activateOn = objects.domainObjectSet(AutoconfigureRule::class)

    /**
     * Adds a basic autoconfigure rule: add [adapterDependency] to `compile` and `runtime` classpath
     * if [dependency] is detected.
     */
    fun activateOn(dependency: String) {
        activateOn(dependency) {
            compileOnly(adapterDependency)
            runtimeOnly(adapterDependency)
        }
    }

    /**
     * Adds an autoconfigure rule that triggers when [dependency] is detected.
     * Note: you need to add at least one dependency via [AutoconfigureRuleBuilder.compileOnly]
     * or [AutoconfigureRuleBuilder.runtimeOnly] methods.
     */
    fun activateOn(dependency: String, configureAction: Action<in AutoconfigureRuleBuilder>) {
        activateOn.add(
            DefaultAutoconfigureRuleBuilder(dependency, enabled).apply {
                configureAction.execute(this)
            }.build()
        )
    }

    /**
     * Dependency coordinates for the adapter (e.g. `io.qameta.allure:allure-junit5:2.8.0`)
     */
    val adapterDependency = adapterVersion.map { "io.qameta.allure:$module:$it" }

    internal val module get() = "allure-$adapterModule"

    /**
     * Name of the artifact (e.g. `allure-junit5`)
     */
    val adapterModule get() = AllureJavaAdapter.find(name)?.adapterName ?: name

    override fun toString() = "AdapterConfig{$name}"
}
