pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    id("com.gradle.develocity") version "4.2.2"
}

buildscript {
    repositories {
        gradlePluginPortal()
    }
}

develocity {
    buildScan {
        publishing.onlyIf { System.getenv().containsKey("CI") }
        if (System.getenv().containsKey("CI")) {
            tag("CI")
        }
        termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
        termsOfUseAgree.set("yes")
    }
}

rootProject.name = "allure-gradle"

includeBuild("build-logic-commons")
includeBuild("build-logic")

include("allure-base-plugin")
include("allure-adapter-plugin")
include("allure-report-plugin")
include("allure-plugin")
include("testkit-junit4")
