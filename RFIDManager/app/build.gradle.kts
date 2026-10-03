import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Bygginfo som visas i Inställningar. Värdena beräknas vid konfigurering, dvs. vid varje
// Gradle-körning (konfigurations-cache är inte aktiverat). Ändras värdet ändras
// BuildConfig-indata, så generateXxxBuildConfig körs om och värdet fryses inte.
// OBS: aktiveras org.gradle.configuration-cache måste detta flyttas till en uppgift
// som alltid körs, annars återanvänds tidsstämpeln från cachen.
fun gitOutput(vararg args: String): String? = try {
    val process = ProcessBuilder(listOf("git") + args)
        .directory(rootDir)
        .redirectErrorStream(true)
        .start()
    val out = process.inputStream.bufferedReader().readText().trim()
    if (process.waitFor() == 0) out else null
} catch (e: Exception) {
    null
}

val buildTimeStamp: String = ZonedDateTime.now(ZoneId.of("Europe/Stockholm"))
    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

val gitCommitLabel: String = run {
    val hash = gitOutput("rev-parse", "--short", "HEAD")?.takeIf { it.isNotEmpty() }
    if (hash == null) {
        "okänd"
    } else {
        val dirty = gitOutput("status", "--porcelain")?.isNotEmpty() ?: false
        if (dirty) "$hash-dirty" else hash
    }
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
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "BUILD_TIME", "\"$buildTimeStamp\"")
        buildConfigField("String", "GIT_COMMIT", "\"$gitCommitLabel\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getProperty("user.home") + "/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isDebuggable = false
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
        buildConfig = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Room persistence (SQLite) – kod genereras med KSP
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Paho MQTT
    implementation(libs.paho.mqtt.client)

    testImplementation(libs.junit)
    // org.json för JVM-enhetstester (Androids stub-implementation kastar i rena JVM-tester)
    testImplementation(libs.org.json)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}