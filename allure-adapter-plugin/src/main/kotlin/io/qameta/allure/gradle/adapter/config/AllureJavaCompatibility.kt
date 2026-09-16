package io.qameta.allure.gradle.adapter.config

import org.gradle.api.GradleException
import org.gradle.api.artifacts.ModuleVersionIdentifier

/** SDK compatibility is independent of the Allure report generator version. */
internal enum class AllureJavaCompatibility {
    ALLURE_2,
    ALLURE_3;

    fun module(adapter: AllureJavaAdapter): String =
        if (this == ALLURE_3 && adapter == AllureJavaAdapter.junit5) "jupiter" else adapter.adapterName

    fun supportsKarateServiceLoading(): Boolean = this == ALLURE_2

    fun accepts(adapter: AllureJavaAdapter, framework: ModuleVersionIdentifier, version: String): Boolean {
        fun incompatible(requirement: String): Nothing = throw GradleException(
            "Allure Java $version cannot configure ${adapter.name} for $framework: $requirement. " +
                "Select a compatible framework/SDK using allure.adapter.allureJavaVersion or " +
                "allure.adapter.frameworks.${adapter.name}.adapterVersion, or disable this adapter with enabled.set(false)."
        )

        if (this == ALLURE_3) {
            when (adapter) {
                AllureJavaAdapter.cucumber4Jvm, AllureJavaAdapter.cucumber5Jvm, AllureJavaAdapter.cucumber6Jvm ->
                    incompatible("Cucumber 4–6 adapters are available only in Allure Java 2.x; use Cucumber 7 for 3.x")
                AllureJavaAdapter.jbehave ->
                    incompatible("JBehave 4 is available only in Allure Java 2.x; use JBehave 5 for 3.x")
                AllureJavaAdapter.testng -> if (!versionAtLeast(framework.version, 7, 10, 0)) {
                    incompatible("Allure Java 3 requires TestNG 7.10.0 or newer")
                }
                AllureJavaAdapter.karate -> if (framework.group != "io.karatelabs" ||
                    !framework.version.startsWith("2.")) {
                    incompatible("Allure Java 3 requires Karate 2 (io.karatelabs:karate-core) and Java 21 or newer")
                }
                else -> Unit
            }
        } else {
            when (adapter) {
                AllureJavaAdapter.testng -> return versionAtLeast(framework.version, 6, 14, 3)
                AllureJavaAdapter.karate -> if (framework.group == "io.karatelabs") {
                    incompatible("Karate 2 requires Allure Java 3.x and Java 21 or newer")
                }
                AllureJavaAdapter.scalatest -> if (framework.name == "scalatest_3") {
                    incompatible("ScalaTest for Scala 3 requires Allure Java 3.x")
                }
                else -> Unit
            }
        }
        return true
    }

    companion object {
        // Match the actual dependency coordinates without evaluating an unused adapter version.
        fun serviceModules(adapter: AllureJavaAdapter, platformListenerEnabled: Boolean): Set<String> =
            if (adapter == AllureJavaAdapter.junit5) {
                buildSet {
                    add("allure-junit5")
                    add("allure-jupiter")
                    if (!platformListenerEnabled) add("allure-junit-platform")
                }
            } else {
                setOf("allure-${adapter.adapterName}")
            }

        // From 2.35.0 the old coordinate is only a relocation POM, with no classifier artifacts.
        fun isJunit5Relocation(module: String, version: String): Boolean =
            module == "allure-junit5" && versionAtLeast(version, 2, 35, 0)

        fun of(version: String): AllureJavaCompatibility = when (version.substringBefore('.').toIntOrNull()) {
            2 -> ALLURE_2
            3 -> ALLURE_3
            else -> throw GradleException(
                "Unsupported Allure Java version '$version'. Select an explicit 2.x or 3.x SDK version " +
                    "with allure.adapter.allureJavaVersion or the framework's adapterVersion."
            )
        }

        private fun versionAtLeast(version: String, vararg minimum: Int): Boolean {
            val actual = version.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
            for (index in 0 until maxOf(actual.size, minimum.size)) {
                val comparison = actual.getOrElse(index) { 0 }.compareTo(minimum.getOrElse(index) { 0 })
                if (comparison != 0) return comparison > 0
            }
            return true
        }
    }
}
