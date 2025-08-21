dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
includeBuild("../build-logic-commons")

include("basics")
include("jvm")
include("publishing")
include("root-build")
