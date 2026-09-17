plugins {
    id("io.qameta.allure-adapter")
}

allure.adapter.frameworks.register("junit5")

allure {
    adapter {
        frameworks {
            junit5 {
                adapterVersion.set(providers.gradleProperty("sdk"))
                enabled.set(false)
            }
            jupiter {
                autoconfigureListeners.set(false)
            }
        }
    }
}

val frameworks = allure.adapter.frameworks
val configuration = listOf(
    "name=${frameworks.jupiter.name}",
    "sameInstance=${frameworks.junit5 === frameworks.jupiter}",
    "namedAlias=${frameworks.named("junit5").get() === frameworks.jupiter}",
    "adapters=${frameworks.names.joinToString()}",
    "module=${frameworks.jupiter.adapterModule}",
    "dependency=${frameworks.jupiter.adapterDependency.get()}",
    "enabled=${frameworks.jupiter.enabled.get()}",
    "listeners=${frameworks.junit5.autoconfigureListeners.get()}"
).joinToString("\n")

tasks.register("writeJupiterConfiguration") {
    val destination = layout.buildDirectory.file("jupiter-configuration.txt")
    doLast {
        destination.get().asFile.apply {
            parentFile.mkdirs()
            writeText(configuration)
        }
    }
}
