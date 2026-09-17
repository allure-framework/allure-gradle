plugins {
    id("java-library")
    id("io.qameta.allure")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.0")
}

allure {
    environment.put("TZ", "UTC")
    // Used by the compatibility smoke matrix; SDK and report versions are independent.
    providers.gradleProperty("allureJavaVersion").orNull?.let { adapter.allureJavaVersion.set(it) }
    providers.gradleProperty("allureReportVersion").orNull?.let { version.set(it) }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
