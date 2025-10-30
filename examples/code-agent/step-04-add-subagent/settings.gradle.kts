rootProject.name = "step-04-add-subagent"

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
        google()
    }
}

includeBuild("../../../.") {
    name = "koog"
}
