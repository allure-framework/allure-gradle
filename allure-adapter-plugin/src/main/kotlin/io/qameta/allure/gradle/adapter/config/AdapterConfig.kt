package io.qameta.allure.gradle.adapter.config

import io.qameta.allure.gradle.adapter.AllureAdapterExtension
import io.qameta.allure.gradle.adapter.autoconfigure.AutoconfigureRule
import io.qameta.allure.gradle.adapter.autoconfigure.AutoconfigureRuleBuilder
import io.qameta.allure.gradle.adapter.autoconfigure.DefaultAutoconfigureRuleBuilder
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.domainObjectSet
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

open class AdapterConfig @Inject constructor(
    val name: String,
    objects: ObjectFactory,
    allureAdapterExtension: AllureAdapterExtension
) {
    companion object {
        private val logger = LoggerFactory.getLogger(AdapterConfig::class.java)
    }

    private val deprecationWarningLogged = AtomicBoolean()

    /**
     * Configures `allure-java` version for the current adapter.
     * The value defaults to [AllureAdapterExtension.allureJavaVersion]
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
     * Autoconfigure listeners is available only for the subset of adapters only (e.g [AdapterHandler.testng],
     * [AdapterHandler.jupiter])
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
                AllureJavaAdapter.find(name)?.let { adapter ->
                    compatibility = org.gradle.api.specs.Spec { framework ->
                        val version = adapterVersion.get()
                        val accepted = AllureJavaCompatibility.of(version).accepts(adapter, framework, version)
                        val deprecation = adapter.deprecationMessage
                        if (accepted && deprecation != null && deprecationWarningLogged.compareAndSet(false, true)) {
                            logger.warn("allure-gradle: Adapter '$name' is deprecated. $deprecation")
                        }
                        accepted
                    }
                }
            }.build()
        )
    }

    /**
     * Dependency coordinates for the adapter (e.g. `io.qameta.allure:allure-jupiter:2.35.5`)
     */
    val adapterDependency = adapterVersion.map { version ->
        val module = AllureJavaAdapter.find(name)?.let { AllureJavaCompatibility.of(version).module(it, version) } ?: name
        "io.qameta.allure:allure-$module:$version"
    }

    internal val module get() = "allure-$adapterModule"

    /**
     * Name of the artifact without the `allure-` prefix (e.g. `jupiter`)
     */
    val adapterModule get() = AllureJavaAdapter.find(name)?.let {
        val version = adapterVersion.get()
        AllureJavaCompatibility.of(version).module(it, version)
    } ?: name

    override fun toString() = "AdapterConfig{$name}"
}
