package io.qameta.allure.gradle.base

import io.qameta.allure.gradle.util.conv
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.provideDelegate

/**
 * Provides API for configuring common properties for Allure.
 */
open class AllureExtension(
    objects: ObjectFactory
) {
    companion object {
        const val NAME = "allure"
    }

    /**
     * `allure-commandline` version
     */
    val version: Property<String> = objects.property<String>().conv("2.13.6")

    // TODO: remove when deprecated [aspectjweaver] is removed
    private val aspectjWeaver by lazy {
        @Suppress("unchecked_cast")
        gatherExtension
            .let {
                it::class.java.getMethod("getAspectjWeaver").invoke(it)
            } as Property<Boolean>
    }

    @Deprecated(level = DeprecationLevel.WARNING, message = "Use gather.aspectjWeaver")
    var aspectjweaver: Boolean
        get() = aspectjWeaver.get()
        set(value) = aspectjWeaver.set(value)

    // TODO: remove when deprecated [aspectjweaver] is removed
    private val autoconfigureProperty by lazy {
        @Suppress("unchecked_cast")
        gatherExtension
            .let {
                it::class.java.getMethod("getAutoconfigure").invoke(it)
            } as Property<Boolean>
    }

    @Deprecated(level = DeprecationLevel.WARNING, message = "Use gather.autoconfigure")
    var autoconfigure: Boolean
        get() = autoconfigureProperty.get()
        set(value) = autoconfigureProperty.set(value)

    // visible for Groovy DSL
    @Deprecated(level = DeprecationLevel.HIDDEN, message = "Use adapters.cucumberJvm")
    fun useCucumberJVM(action: Action<in Any>) {
        action.execute(getAdapter("getCucumberJvm"))
    }

    // visible for Groovy DSL
    @Deprecated(level = DeprecationLevel.HIDDEN, message = "Use adapters.cucumber2Jvm")
    fun useCucumber2JVM(action: Action<in Any>) {
        action.execute(getAdapter("getCucumber2Jvm"))
    }

    // visible for Groovy DSL
    @Deprecated(level = DeprecationLevel.HIDDEN, message = "Use adapters.junit4")
    fun useJUnit4(action: Action<in Any>) {
        action.execute(getAdapter("getJunit4"))
    }

    // visible for Groovy DSL
    @Deprecated(level = DeprecationLevel.HIDDEN, message = "Use adapters.junit5")
    fun useJUnit5(action: Action<in Any>) {
        action.execute(getAdapter("getJunit5"))
    }

    // visible for Groovy DSL
    @Deprecated(level = DeprecationLevel.HIDDEN, message = "Use adapters.testng")
    fun useTestNG(action: Action<in Any>) {
        action.execute(getAdapter("getTestng"))
    }

    // visible for Groovy DSL
    @Deprecated(level = DeprecationLevel.HIDDEN, message = "Use adapters.spock")
    fun useSpock(action: Action<in Any>) {
        action.execute(getAdapter("getSpock"))
    }

    private val gatherExtension: Any
        get() = let { it as ExtensionAware }.extensions.getByName("gather")

    private fun getAdapter(adapterName: String) =
        gatherExtension
            .let { it::class.java.getMethod("getAdapters").invoke(it) }
            .let { it::class.java.getMethod(adapterName).invoke(it) }
}
