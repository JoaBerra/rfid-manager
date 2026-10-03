package com.joakim.rfidmanager

import android.content.Context
import android.util.Log
import com.joakim.rfidmanager.data.local.DatabaseProvider
import com.joakim.rfidmanager.data.migration.AndroidMigrationLog
import com.joakim.rfidmanager.data.migration.JsonToRoomMigrator
import com.joakim.rfidmanager.data.migration.RoomMigrationStore
import com.joakim.rfidmanager.data.localization.LocalizationManager
import com.joakim.rfidmanager.data.mqtt.MqttConnectionManager
import com.joakim.rfidmanager.data.repository.PersistedReadingRepository
import com.joakim.rfidmanager.data.settings.AppSettings
import com.joakim.rfidmanager.outbox.work.OutboxNetworkTrigger
import com.joakim.rfidmanager.outbox.work.OutboxScheduler
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

class AppContainer(context: Context) {

    /** Startar utkorgens utskick (WorkManager). Fungerar även när appen är stängd. */
    val outboxScheduler: OutboxScheduler by lazy { OutboxScheduler(context) }

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
        )
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
                // Spara först, skicka sedan: varje ny avläsning sparas som PENDING och triggar därefter utskick.
                onReadingSaved = { outboxScheduler.onReadingSaved() },
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
            // Utkorgen: töm väntande poster vid appstart (en gång per process, inte vid rotation) …
            if (outboxStartedForProcess.compareAndSet(false, true)) {
                try {
                    if (persistedReadingRepository.pendingCount() > 0) outboxScheduler.onAppStart()
                } catch (e: Exception) {
                    Log.e("AppContainer", "Kunde inte starta utkorgen vid appstart", e)
                }
            }
        }
        // … och när nätverk blir tillgängligt (när appen är stängd sköter WorkManagers nätverkskrav det).
        OutboxNetworkTrigger.register(context) {
            appScope.launch {
                if (persistedReadingRepository.pendingCount() > 0) outboxScheduler.onNetworkAvailable()
            }
        }
    }

    private companion object {
        val outboxStartedForProcess = AtomicBoolean(false)
    }
}
