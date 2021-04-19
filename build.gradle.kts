plugins {
    java
    groovy
    `java-gradle-plugin`
    id("ru.vyarus.quality")
    id("com.gradle.plugin-publish")
    id("com.github.vlsi.gradle-extensions")
}

repositories {
    mavenCentral()
}

group = "io.qameta.allure"

//apply from: "${rootProject.projectDir}/gradle/plugin-publish.gradle"
//apply from: "${rootProject.projectDir}/gradle/maven-publish.gradle"
//apply from: "${rootProject.projectDir}/gradle/bintray.gradle"
//apply from: "${rootProject.projectDir}/gradle/release.gradle"

gradlePlugin {
    plugins {
        create("allurePlugin") {
            id = "io.qameta.allure"
            implementationClass = "io.qameta.allure.gradle.AllurePlugin"
        }
    }
}

plugins.withId("java") {
    configure<JavaPluginConvention> {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    tasks.withType<JavaCompile>().configureEach {
        inputs.property("java.version", System.getProperty("java.version"))
        inputs.property("java.vm.version", System.getProperty("java.vm.version"))
        options.encoding = "UTF-8"
        options.compilerArgs.add("-Xlint:deprecation")
        options.compilerArgs.add("-Werror")
    }
}

val junit5Plugin by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    implementation(localGroovy())

    testImplementation(gradleTestKit())
    testImplementation("junit:junit:4.12")
    testImplementation("commons-io:commons-io:2.5")
    testImplementation("org.assertj:assertj-core:3.6.2")
    testImplementation("org.apache.commons:commons-text:1.1")

    junit5Plugin("org.junit.platform:junit-platform-gradle-plugin:1.0.0")
    testRuntimeOnly(files(testPluginClasspath(sourceSets.main.get().runtimeClasspath.files + junit5Plugin.files)))
}

quality {
    pmdVersion = "5.5.4"
    checkstyleVersion = "7.6"
    spotbugsVersion = "4.2.3"
    // codenarc ruleset is not really configured
    codenarc = false
}

fun testPluginClasspath(collections: Collection<File>): File {
    val generatedDir = File(buildDir, "generated")
    val pluginsClasspathFile = File(generatedDir, "plugin-classpath.txt")

    generatedDir.mkdirs()

    pluginsClasspathFile.writeText(collections.joinToString("\n"))
    return generatedDir
}
