plugins {
    id("io.qameta.allure")
}

allure {
    gather.adapters.spock.enabled.set(true)
    gather {
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
