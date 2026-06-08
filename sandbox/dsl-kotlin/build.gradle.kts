plugins {
    id("java-library")
    id("io.qameta.allure")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.0")
}

allure {
    environment.put("TZ", "UTC")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
