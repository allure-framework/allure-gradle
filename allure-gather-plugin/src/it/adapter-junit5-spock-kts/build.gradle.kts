plugins {
    id("io.qameta.allure-gather")
}

repositories {
    mavenCentral()
}

allure {
    gather {
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
            allure.gather.adapters.toList().map { it.toString() }.sorted().toString()
        )
    }
}
