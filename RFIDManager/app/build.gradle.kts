plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // KAPT fallback (instead of KSP) because the com.google.devtools.ksp Gradle plugin v2.2.0-1.0.22
    // cannot be resolved from any repo (repeated "Resource missing" for the POM + "plugin not found").
    // All standard fixes (pluginManagement order, explicit portal maven, explicit id+version in settings + root,
    // cache nukes, --refresh, --no-daemon) have been applied without success.
    // This unblocks compilation and testing of the Fas 2 code:
    //   - PersistedReadingEntity + DAO + Repository (with all Figma-spec metadata fields)
    //   - PersistedListItem, PersistedReadingsScreen, persistAfterWrite toggle
    //   - MqttSender (Sparkplug B style), MqttStatusScreen, Transmit flow
    // Revert to KSP + `ksp(libs.androidx.room.compiler)` + re-enable the ksp plugin lines
    // once the KSP artifact resolution works in this environment.
    //
    // Note: Do NOT apply 'org.jetbrains.kotlin.kapt' plugin id explicitly here — it conflicts with
    // AGP 9 built-in Kotlin. The main Kotlin plugin (kotlin-compose) provides the 'kapt' configuration.
}

android {
    namespace = "com.joakim.rfidmanager"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.joakim.rfidmanager"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Room persistence (RFID + EAN readings, housekeeping ready)
    // Processor temporarily disabled (kapt line commented) because of repeated KSP plugin resolution failures
    // ("plugin not found", POM resource missing even after repo order, portal maven, cache nukes etc.).
    // Full entity/DAO/Database + mappers stay in tree (match Figma spec exactly: uidOrCode, source, memoryBank/address/length/payload, sparkplugJson, transmitted etc.).
    //
    // Current: AppContainer + PersistedReadingRepository run in pure in-memory mode (dao=null).
    // This lets the app start without the generated *_Impl, and the complete PERSISTED tab + auto-persist after write
    // + Transmit ↑ (with mark + MqttSender Sparkplug JSON) can be exercised end-to-end against the test env.
    // Re-enable `kapt(libs.androidx.room.compiler)` + real dao when KSP/Gradle plugin issues are resolved.
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    // kapt(libs.androidx.room.compiler)  // or ksp(...) -- uncomment + clean/rebuild when processor available


    // Paho MQTT (simple functional start; Sparkplug B payload structure)
    implementation(libs.paho.mqtt.client)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}