import de.fayard.refreshVersions.bootstrapRefreshVersions

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    `gradle-enterprise`
}

buildscript {
    repositories {
        gradlePluginPortal()
    }
    // See https://jmfayard.github.io/refreshVersions/setup/
    dependencies.classpath("de.fayard.refreshVersions:refreshVersions:0.9.7")
}

bootstrapRefreshVersions()

val isCiServer = System.getenv().containsKey("CI")

if (isCiServer) {
    gradleEnterprise {
        buildScan {
            termsOfServiceUrl = "https://gradle.com/terms-of-service"
            termsOfServiceAgree = "yes"
            tag("CI")
        }
    }
}

rootProject.name = "allure-gradle"

