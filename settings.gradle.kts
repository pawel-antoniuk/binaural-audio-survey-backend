pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}


plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
    id("de.fayard.refreshVersions") version "0.60.5"
}

rootProject.name = "server"

