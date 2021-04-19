plugins {
    id("io.qameta.allure-gather")
    id("io.qameta.allure-report")
}

// The folowing tests different syntax variations to verify if they compile

allure {
    version.set("42.0")
    gather {
        adapters {
            junit5 {
                adapterVersion.set("42.0")
                enabled.set(true)
            }
            spock
            testng.adapterVersion.set("43")
            testng.enabled.set(false)
            cucumberJvm(3).supportsAutoconfigureListeners.set(true)
            cucumber5Jvm {
                activateOn("com.example.custom.cucumber:cucumber-core")
            }
            cucumber6Jvm {
                activateOn("com.example.custom.cucumber:cucumber-core") {
                    compileOnly("com.acme:compile-dep")
                    runtimeOnly("com.acme:runtime-dep")
                }
            }
        }
        adapters.junit5
        adapters.junit5.enabled.set(false)
    }
    gather.adapters.spock.enabled.set(true)
    commandline {
        group.set("com.example")
        module.set("test")
        extension.set("jar")
        downloadUrlPattern.set("https://...")
    }
    commandline.group.set("abcd")
    report {
        reportDir.set(layout.buildDirectory.dir("allure/reports"))
        dependsOnTests.set(true)
        dependsOnTests()
    }
    report.reportDir.set(buildDir.resolve("allure/reports"))
    report.dependsOnTests.set(true)
}
allure.gather.adapters.cucumberJvm.enabled.set(true)
allure.commandline.downloadUrlPattern.set("localhost")
allure.report.dependsOnTests.set(true)

val testDsl by tasks.registering {
    doLast {
        println("kotlin-dsl is ok")
    }
}
