plugins {
    id("io.qameta.allure-adapter")
}

repositories {
    mavenCentral()
}

allure {
    adapter {
        adapters {
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
            allure.adapter.adapters.toList().map { it.toString() }.sorted().toString()
        )
    }
}
