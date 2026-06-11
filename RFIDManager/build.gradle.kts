// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // KSP plugin declaration commented out for the kapt fallback (see app/build.gradle.kts).
    // Prevents Gradle from attempting to resolve the unreachable KSP plugin artifact.
    // id("com.google.devtools.ksp") version "2.2.0-1.0.22" apply false
}