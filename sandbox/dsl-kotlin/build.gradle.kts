plugins {
    id("io.qameta.allure")
}

allure {
    adapter.adapters.spock.enabled.set(true)
    adapter {
        allureJavaVersion.set("213")
        adapters {
            junit5
            cucumber2Jvm {
            }
        }
    }
    report {
    }
}
