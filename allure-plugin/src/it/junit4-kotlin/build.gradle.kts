plugins {
    java
    id("io.qameta.allure")
}

allure {
    version.set("2.8.1")

    useJUnit4 {
        version = "2.9.0"
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("junit:junit:4.12")
}
