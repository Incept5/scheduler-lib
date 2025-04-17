rootProject.name = "scheduler-lib"
include("scheduler-core")
include("scheduler-db")
include("scheduler-quarkus")
include("test-quarkus-scheduling")

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
