plugins {
    id("io.qameta.allure-adapter")
}

repositories {
    mavenCentral()
}

allure {
    adapter {
        frameworks {
            junit5 {
                adapterVersion.set("42.0")
            }
            spock
        }
    }
}

val printAdapters by tasks.registering {
    doLast {
        buildDir.mkdirs()
        file("$buildDir/printAdapters.txt").writeText(
            allure.adapter.frameworks.toList().map { it.toString() }.sorted().toString()
        )
    }
}
