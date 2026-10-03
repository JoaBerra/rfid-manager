package com.joakim.rfidmanager

import android.content.Context
import android.util.Log
import com.joakim.rfidmanager.data.local.DatabaseProvider
import com.joakim.rfidmanager.data.migration.AndroidMigrationLog
import com.joakim.rfidmanager.data.migration.JsonToRoomMigrator
import com.joakim.rfidmanager.data.migration.RoomMigrationStore
import com.joakim.rfidmanager.data.localization.LocalizationManager
import com.joakim.rfidmanager.data.mqtt.MqttConnectionManager
import com.joakim.rfidmanager.data.mqtt.MqttSender
import com.joakim.rfidmanager.data.repository.PersistedReadingRepository
import com.joakim.rfidmanager.data.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

class AppContainer(context: Context) {

    val settings: AppSettings by lazy { AppSettings(context) }

    val localizationManager: LocalizationManager by lazy { LocalizationManager(context) }

    val mqttManager: MqttConnectionManager by lazy {
        val host = settings.brokerHost.value
        val port = settings.brokerPort.value
        MqttConnectionManager(
            host = host,
            port = port,
            username = settings.mqttUsername.value,
            password = settings.getMqttPassword()
        ).also {
            MqttSender.init(it)
        }
    }

    /**
     * Room är enda lagringen. Ingen tyst reserv: misslyckas databasen loggas det tydligt
     * (tagg AppContainer) och felet kastas vidare i stället för att byta till annan lagring.
     */
    val persistedReadingRepository: PersistedReadingRepository by lazy {
        try {
            val db = DatabaseProvider.getDatabase(context)
            PersistedReadingRepository(
                dao = db.persistedReadingDao(),
                migrator = JsonToRoomMigrator(
                    jsonFile = File(context.filesDir, "readings.json"),
                    store = RoomMigrationStore(db),
                    log = AndroidMigrationLog
                )
            )
        } catch (e: Exception) {
            Log.e("AppContainer", "Room kunde inte initieras – ingen JSON-/minnesreserv används", e)
            throw e
        }
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // Starta ev. JSON->Room-migrering direkt vid appstart, oberoende av vilken skärm som öppnas.
        appScope.launch {
            try {
                persistedReadingRepository.load()
            } catch (e: Exception) {
                Log.e("AppContainer", "Start av lagring/migrering misslyckades", e)
            }
        }
    }
}
