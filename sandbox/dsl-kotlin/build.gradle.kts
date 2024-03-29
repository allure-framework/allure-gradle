plugins {
    id("java-library")
    id("io.qameta.allure")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
}

allure {
    environment.put("TZ", "UTC")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
