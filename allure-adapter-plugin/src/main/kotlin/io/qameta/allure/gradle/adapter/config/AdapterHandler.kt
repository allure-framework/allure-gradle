package io.qameta.allure.gradle.adapter.config

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.internal.NamedDomainObjectContainerConfigureDelegate
import org.gradle.internal.metaobject.DynamicInvokeResult
import org.gradle.util.internal.ConfigureUtil
import javax.inject.Inject
import kotlin.reflect.KProperty

open class AdapterHandler @Inject constructor(
    private val data: NamedDomainObjectContainer<AdapterConfig>
) : NamedDomainObjectContainer<AdapterConfig> by data {
    internal val configuredAdapters = mutableMapOf<AllureJavaAdapter, AdapterConfig>()

    val junit4 by lazyCreating
    val jupiter by lazyCreating

    @Deprecated("Use jupiter", ReplaceWith("jupiter"))
    val junit5 get() = jupiter

    val junitPlatform by lazyCreating
    val jbehave by lazyCreating
    val jbehave5 by lazyCreating
    val karate by lazyCreating
    val scalatest by lazyCreating
    val testng by lazyCreating
    val assertj by lazyCreating
    val spock by lazyCreating
    val cucumber4Jvm by lazyCreating
    val cucumber5Jvm by lazyCreating
    val cucumber6Jvm by lazyCreating
    val cucumber7Jvm by lazyCreating

    fun cucumberJvm(majorVersion: Int) = maybeCreate(
        if (majorVersion == 1) "cucumberJvm" else "cucumber${majorVersion}Jvm"
    )

    operator fun AdapterConfig.invoke(configureAction: Action<in AdapterConfig>) {
        configureAction.execute(this)
    }

    override fun configure(configureClosure: Closure<*>): AdapterHandler =
        // Use Gradle's container DSL semantics, with alias lookups routed through this handler.
        ConfigureUtil.configureSelf(
            configureClosure, this, object : NamedDomainObjectContainerConfigureDelegate(configureClosure, this) {
                override fun _configure(name: String): DynamicInvokeResult =
                    DynamicInvokeResult.found(maybeCreate(name))

                override fun _configure(name: String, params: Array<out Any>): DynamicInvokeResult {
                    val action = params.singleOrNull() as? Closure<*> ?: return super._configure(name, params)
                    maybeCreate(name)
                    return DynamicInvokeResult.found(getByName(name, action))
                }
            }
        )

    // Preserve name-based access as well as the Kotlin/Groovy DSL alias, without a second adapter.
    private fun canonicalName(name: String) = if (name == "junit5") "jupiter" else name

    override fun maybeCreate(name: String) = data.maybeCreate(canonicalName(name))
    override fun create(name: String) = data.create(canonicalName(name))
    override fun create(name: String, configureAction: Action<in AdapterConfig>) =
        data.create(canonicalName(name), configureAction)
    override fun create(name: String, configureClosure: Closure<*>) =
        data.create(canonicalName(name), configureClosure)

    override fun register(name: String) = data.register(canonicalName(name))
    override fun register(name: String, configurationAction: Action<in AdapterConfig>) =
        data.register(canonicalName(name), configurationAction)

    override fun findByName(name: String) = data.findByName(canonicalName(name))
    override fun getByName(name: String) = data.getByName(canonicalName(name))
    override fun getByName(name: String, configureAction: Action<in AdapterConfig>) =
        data.getByName(canonicalName(name), configureAction)
    override fun getByName(name: String, configureClosure: Closure<*>) =
        data.getByName(canonicalName(name), configureClosure)
    override fun getAt(name: String) = data.getAt(canonicalName(name))

    override fun named(name: String) = data.named(canonicalName(name))
    override fun named(name: String, configurationAction: Action<in AdapterConfig>) =
        data.named(canonicalName(name), configurationAction)
    override fun <S : AdapterConfig> named(name: String, type: Class<S>) =
        data.named(canonicalName(name), type)
    override fun <S : AdapterConfig> named(name: String, type: Class<S>, configurationAction: Action<in S>) =
        data.named(canonicalName(name), type, configurationAction)

    init {
        whenObjectAdded {
            val newConfig = this
            val adapter = AllureJavaAdapter.find(name) ?: return@whenObjectAdded
            configuredAdapters[adapter] = newConfig
        }
        whenObjectRemoved {
            val adapter = AllureJavaAdapter.find(name) ?: return@whenObjectRemoved
            configuredAdapters.remove(adapter)
        }
    }

}

// Retrieves an element from AdapterHandler lazily using property name
private val AdapterHandler.lazyCreating get() = LazyCreating(this)

private class LazyCreating(val container: AdapterHandler)

private operator fun LazyCreating.provideDelegate(
    receiver: Any?,
    property: KProperty<*>
): Lazy<AdapterConfig> = lazy {
    // We return non-lazy object since it makes DSL simpler to use (less .get() calls)
    container.maybeCreate(property.name)
    // Here's maybeRegister alternative:
    // if (name in container.names) container.named(name) else container.register(name)
}
