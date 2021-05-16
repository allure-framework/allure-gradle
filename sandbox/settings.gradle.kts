import org.gradle.util.GradleVersion

rootProject.name = "allure-gradle-sandbox"

// Gradle 6.x needs to see all the included buidls :-/
// https://github.com/gradle/gradle/issues/17061#issuecomment-836330587
if (GradleVersion.current() < GradleVersion.version("7.0")) {
    includeBuild("../build-logic-commons")
    includeBuild("../build-logic")
}

includeBuild("../")

include("dsl-kotlin")
include("dsl-groovy")
