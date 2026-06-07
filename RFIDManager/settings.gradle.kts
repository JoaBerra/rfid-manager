pluginManagement {
    repositories {
        gradlePluginPortal()
        // Explicit portal maven (helps when the DSL alone + catalog doesn't pull KSP plugin artifacts reliably)
        maven { url = uri("https://plugins.gradle.org/m2/") }
        google()
        mavenCentral()
    }
}

plugins {
    // KSP declarations commented out for the kapt fallback (see app/build.gradle.kts).
    // This stops Gradle from trying to resolve the KSP plugin (which has been unreachable).
    // id("com.google.devtools.ksp") version "2.2.0-1.0.22" apply false
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
