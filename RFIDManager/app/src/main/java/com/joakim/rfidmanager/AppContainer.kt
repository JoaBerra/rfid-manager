package com.joakim.rfidmanager

import android.content.Context
import com.joakim.rfidmanager.data.repository.PersistedReadingRepository

/**
 * Enkel "DI-container" för att hålla repositories.
 * Används istället för Hilt för att hålla det enkelt i det befintliga projektet.
 * Initieras i MainActivity.
 *
 * Current mode: in-memory repository (dao = null).
 * This bypasses the need for the Room annotation processor (room-compiler via kapt/ksp)
 * which was blocked by KSP Gradle plugin resolution issues earlier.
 * All Persisted UI (PERSISTED tab, list with metadata from Figma spec, Transmit) + auto-persist
 * after write + MqttSender (Sparkplug-style) will work for end-to-end testing.
 * Switch back to real DB by passing a dao when the processor builds cleanly again.
 */
class AppContainer(context: Context) {

    // In-memory mode for now (no Room processor required at build/runtime).
    // Data is lost when the process dies — sufficient for validating the full Fas 2 flow
    // against the local Docker MQTT test environment.
    val persistedReadingRepository: PersistedReadingRepository by lazy {
        PersistedReadingRepository(dao = null)
    }
}
