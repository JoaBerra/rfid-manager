# Jetpack Compose
-keep class * extends androidx.compose.runtime.Composable { *; }
-dontwarn androidx.compose.**

# Paho MQTT
-keep class org.eclipse.paho.** { *; }
-dontwarn org.eclipse.paho.**

# Room (runtime entities for JSON serialization)
-keep class com.joakim.rfidmanager.data.local.entities.** { *; }

# Domain models serialized via org.json
-keep class com.joakim.rfidmanager.domain.model.** { *; }
-keep class com.joakim.rfidmanager.ui.model.** { *; }

# App settings serialized via SharedPreferences
-keep class com.joakim.rfidmanager.data.settings.** { *; }

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
