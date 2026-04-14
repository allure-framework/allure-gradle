plugins {
    id("java")
    id("io.qameta.allure-adapter")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

allure {
    adapter {
        aspectjWeaver.set(true)
    }
}

tasks.test {
    useJUnitPlatform()
    doFirst {
        val jvmArgsFile = layout.buildDirectory.file("jvmArgs.txt").get().asFile
        jvmArgsFile.parentFile.mkdirs()
        jvmArgsFile.writeText(allJvmArgs.joinToString(System.lineSeparator()))
    }
}
