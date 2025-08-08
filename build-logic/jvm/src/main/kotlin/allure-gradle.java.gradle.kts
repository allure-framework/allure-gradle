import buillogic.filterEolSimple

plugins {
    `java-base`
}

tasks.withType<JavaCompile>().configureEach {
    inputs.property("java.version", System.getProperty("java.version"))
    inputs.property("java.vm.version", System.getProperty("java.vm.version"))
    options.apply {
        release = 8
        encoding = "UTF-8"
        compilerArgs.add("-Xlint:deprecation")
        // Suppress warnings about obsolete source/target options on newer JDKs while keeping -Werror
        compilerArgs.add("-Xlint:-options")
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
