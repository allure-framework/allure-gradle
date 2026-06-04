plugins {
    id("java")
    id("io.qameta.allure-adapter")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.1.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

allure {
    adapter {
        resultsDir.set(layout.buildDirectory.dir("custom-allure-results"))
    }
}

tasks.test {
    useJUnitPlatform()
}
