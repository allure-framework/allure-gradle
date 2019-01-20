plugins {
    java
    id("io.qameta.allure")
}

allure {
    version = "2.8.1"
    autoconfigure = true

    useJUnit4 {
        version = "2.9.0"
    }
}

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    testCompile("junit:junit:4.12")
}
