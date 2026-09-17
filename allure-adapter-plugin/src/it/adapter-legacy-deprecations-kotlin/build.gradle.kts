plugins {
    id("io.qameta.allure-adapter")
}

allure.adapter.frameworks {
    cucumber4Jvm
    cucumber5Jvm
    cucumber6Jvm
    jbehave
}

val legacyNames = allure.adapter.frameworks.names.joinToString(",")

tasks.register("writeLegacyConfigurations") {
    val destination = layout.buildDirectory.file("legacy-configurations.txt")
    doLast {
        destination.get().asFile.apply {
            parentFile.mkdirs()
            writeText(legacyNames)
        }
    }
}
