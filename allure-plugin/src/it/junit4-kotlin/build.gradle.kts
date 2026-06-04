plugins {
    java
    id("io.qameta.allure")
}

allure {
    version.set("2.42.0")

    adapter {
        frameworks {
            junit4 {
                adapterVersion.set("2.35.2")
            }
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
