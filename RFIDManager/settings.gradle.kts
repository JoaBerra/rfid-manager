pluginManagement {
    repositories {
        gradlePluginPortal()
        maven { url = uri("https://plugins.gradle.org/m2/") }
        google()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "RFIDManager"
include(":app")
