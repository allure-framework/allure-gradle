package io.qameta.allure.gradle.adapter.autoconfigure

import org.gradle.api.Action
import org.gradle.api.artifacts.DependenciesMetadata
import org.gradle.api.artifacts.DependencyMetadata
import org.gradle.api.provider.Provider
import org.slf4j.LoggerFactory

class DependencyDeclaration(
    val id: Any,
    private val configureAction: Action<in DependencyMetadata<*>>? = null
) {
    companion object {
        private val logger = LoggerFactory.getLogger(DependencyDeclaration::class.java)
    }

    init {
        verifyDependency(id)
    }

    private fun Any.unwrapProviders(): Any = when (this) {
        is Provider<*> -> get().unwrapProviders()
        else -> this
    }

    fun addTo(deps: DependenciesMetadata<*>): String = when (val actualId = id.unwrapProviders()) {
        is Map<*, *> -> {
            @Suppress("unchecked_cast")
            val map = actualId as Map<String, String>
            logger.info("Allure: adding {}")
            if (configureAction == null) deps.add(map) else deps.add(map, configureAction)
            map.toString()
        }
        else -> {
            val dep = actualId.toString()
            if (configureAction == null) deps.add(dep) else deps.add(dep, configureAction)
            dep
        }
    }

    private fun verifyDependency(dependencyNotation: Any) {
        when (dependencyNotation) {
            is String, is Provider<*>, is Map<*, *> -> return
        }
        throw IllegalArgumentException(
            "Please use Provider<String> or String for dependency notation. " +
                    "Input object $dependencyNotation is ${dependencyNotation::class.java}"
        )
    }
}
