import buillogic.filterEolSimple

plugins {
    `java-base`
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<JavaCompile>().configureEach {
    inputs.property("java.version", System.getProperty("java.version"))
    inputs.property("java.vm.version", System.getProperty("java.vm.version"))
    options.apply {
        encoding = "UTF-8"
        compilerArgs.add("-Xlint:deprecation")
        compilerArgs.add("-Werror")
    }
}
// Add default license/notice when missing (e.g. see :src:config that overrides LICENSE)

tasks.withType<Jar>().configureEach {
    into("META-INF") {
        filterEolSimple("crlf")
        from("$rootDir/LICENSE")
        from("$rootDir/NOTICE")
    }
    manifest {
        attributes["Bundle-License"] = "Apache-2.0"
        attributes["Specification-Title"] = project.name + " " + project.description
        attributes["Specification-Vendor"] = "Qameta Software"
        attributes["Implementation-Vendor"] = "Qameta Software"
        attributes["Implementation-Vendor-Id"] = "io.qameta.allure"
        // Implementation-Version is not here to make jar reproducible across versions
    }
}
