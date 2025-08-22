import buildlogic.embeddedKotlinDsl
import buildlogic.plugin
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("allure-gradle.kotlin-dsl-gradle-plugin")
}

group = "io.qameta.allure.buildlogic"

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(embeddedKotlinDsl())
    implementation(libs.vlciGradleExtensionsPlugin)
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin")
    implementation(libs.dokkaPlugin)
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        languageVersion = KotlinVersion.KOTLIN_1_8
        apiVersion = KotlinVersion.KOTLIN_1_8
    }
}
