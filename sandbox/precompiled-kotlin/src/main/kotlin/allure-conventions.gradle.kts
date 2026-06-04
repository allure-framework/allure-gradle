plugins {
    id("java")
    id("io.qameta.allure")
}

val allureRuntimeVersion = "2.42.0"
val allureJavaVersion = "2.35.2"

allure {
    version.set(allureRuntimeVersion)
}

allure.version.set(allureRuntimeVersion)
allure.adapter.allureJavaVersion.set(allureJavaVersion)
allure.adapter.aspectjWeaver.set(true)
