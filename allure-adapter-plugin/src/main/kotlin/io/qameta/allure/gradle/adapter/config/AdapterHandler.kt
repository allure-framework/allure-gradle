package io.qameta.allure.gradle.adapter.config

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import javax.inject.Inject
import kotlin.reflect.KProperty

open class AdapterHandler @Inject constructor(
    private val data: NamedDomainObjectContainer<AdapterConfig>
) : NamedDomainObjectContainer<AdapterConfig> by data {
    internal val configuredAdapters = mutableMapOf<AllureJavaAdapter, AdapterConfig>()

    val junit4 by lazyCreating
    val junit5 by lazyCreating
    val testng by lazyCreating
    val spock by lazyCreating
    val cucumberJvm by lazyCreating
    val cucumber2Jvm by lazyCreating
    val cucumber3Jvm by lazyCreating
    val cucumber4Jvm by lazyCreating
    val cucumber5Jvm by lazyCreating
    val cucumber6Jvm by lazyCreating

    fun cucumberJvm(majorVersion: Int) = maybeCreate(
        if (majorVersion == 1) "cucumberJvm" else "cucumber${majorVersion}Jvm"
    )

    operator fun AdapterConfig.invoke(configureAction: Action<in AdapterConfig>) {
        configureAction.execute(this)
    }

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
