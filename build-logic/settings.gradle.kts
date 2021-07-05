dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
    }
}

rootProject.name = "build-logic"
includeBuild("../build-logic-commons")

include("basics")
include("jvm")
include("publishing")
include("root-build")
