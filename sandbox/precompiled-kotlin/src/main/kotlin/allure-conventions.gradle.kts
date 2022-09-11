plugins {
    id("java")
    id("io.qameta.allure")
}

val allureVersion = "2.17.22"

allure {
    version.set(allureVersion)
}

allure.version.set(allureVersion)
allure.adapter.allureJavaVersion.set(allureVersion)
allure.adapter.aspectjWeaver.set(true)
