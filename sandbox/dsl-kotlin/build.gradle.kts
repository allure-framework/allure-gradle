plugins {
    id("java-library")
    id("io.qameta.allure")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
}

allure {
    version.set("2.15.0")
    adapter {
        allureJavaVersion.set("2.15.0")
    }
}
