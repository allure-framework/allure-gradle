plugins {
    id("io.qameta.allure")
}

allure {
    adapter.frameworks.spock.enabled.set(true)
    adapter {
        allureJavaVersion.set("213")
        frameworks {
            junit5
            cucumber2Jvm {
            }
        }
    }
    report {
    }
}
