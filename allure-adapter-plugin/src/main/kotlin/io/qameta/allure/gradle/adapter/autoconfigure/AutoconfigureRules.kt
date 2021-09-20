package io.qameta.allure.gradle.adapter.autoconfigure

import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.dsl.ComponentMetadataHandler
import org.gradle.api.specs.Spec
import org.slf4j.LoggerFactory

interface AutoconfigureRule {
    fun configure(context: ComponentMetadataHandler)
}

class SimpleRule(
    val triggerDependency: String,
    private val predicate: Spec<in ModuleVersionIdentifier>?,
    private val extraDependencies: Map<String, List<DependencyDeclaration>>
) : AutoconfigureRule {
    companion object {
        private val logger = LoggerFactory.getLogger(SimpleRule::class.java)
    }

    override fun configure(context: ComponentMetadataHandler) {
        logger.debug("Configuring Allure autoconfigure rule for {}", triggerDependency)
        context.withModule(triggerDependency) {
            logger.debug("allure-gradle: detected {}", triggerDependency)
            if (predicate?.isSatisfiedBy(id) == false) {
                return@withModule
            }
            for ((variantName, deps) in extraDependencies) {
                withVariant(variantName) {
                    withDependencies {
                        for (dependency in deps) {
                            val addedId = dependency.addTo(this)
                            logger.info(
                                "allure-gradle: added dependency {} to {} scope of {}",
                                addedId,
                                variantName,
                                id
                            )
                        }
                    }
                }
            }
        }
    }
}
