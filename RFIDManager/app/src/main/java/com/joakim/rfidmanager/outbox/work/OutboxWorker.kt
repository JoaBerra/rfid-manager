package com.joakim.rfidmanager.outbox.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.joakim.rfidmanager.data.local.DatabaseProvider
import com.joakim.rfidmanager.data.settings.AppSettings
import com.joakim.rfidmanager.outbox.core.DrainResult
import com.joakim.rfidmanager.outbox.core.ExponentialBackoff
import com.joakim.rfidmanager.outbox.core.OutboxDispatcher
import com.joakim.rfidmanager.outbox.core.RetryPolicy
import com.joakim.rfidmanager.outbox.mqtt.MqttOutboxTransport
import com.joakim.rfidmanager.outbox.mqtt.PahoMqttLink
import com.joakim.rfidmanager.outbox.mqtt.ReadingMqttEncoder
import com.joakim.rfidmanager.outbox.room.ReadingOutboxStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

/**
 * Tömmer utkorgen mot MQTT-brokern. Oberoende av UI-processens tillstånd: bygger själv databas,
 * inställningar (värd, port, användarnamn, krypterat lösenord via AppSettings) och en egen
 * MQTT-anslutning, och kopplar ner den när kön är tömd (eller vid fel/avbrott).
 *
 * - Ingenting att skicka -> ingen anslutning alls.
 * - Alla poster skickas i tidsordning, en i taget; en post blir SENT först när brokern bekräftat (QoS 1).
 * - Misslyckat försök -> Result.retry() (WorkManager exponentiell backoff) – posten behålls.
 * - `force` (inputdata) tvingar fram första försöket utan att vänta ut postens backoff ('Skicka nu',
 *   nätverk tillbaka). Omkörningar efter retry använder aldrig force.
 * - `skipPause` (inputdata) kringgår även pausen mellan omgångar. Bara 'Skicka nu' sätter den; nätverk
 *   som kommer tillbaka väntar ut pausen.
 * - Omgångar: konfigurationen (försök per omgång, paus, antal omgångar) läses ur AppSettings vid varje körning.
 *   Står första posten i paus planeras nästa körning till pausens slut med [OutboxScheduler.scheduleAfter]
 *   (WorkManager, överlever omstart) i stället för Result.retry().
 */
class OutboxWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val force = inputData.getBoolean(KEY_FORCE, false) && runAttemptCount == 0
        val skipPause = inputData.getBoolean(KEY_SKIP_PAUSE, false) && runAttemptCount == 0
        val ctx = applicationContext
        val store = ReadingOutboxStore(DatabaseProvider.getDatabase(ctx).persistedReadingDao())

        var transport: MqttOutboxTransport<*>? = null
        return try {
            if (store.countPending() == 0) {
                Log.i(TAG, "Utkorgen tom – inget att skicka")
                return Result.success()
            }
            val settings = AppSettings(ctx)
            val link = PahoMqttLink(
                brokerUrl = "tcp://${settings.brokerHost.value}:${settings.brokerPort.value}",
                // Eget, unikt klient-id per körning: UI:ts anslutning (rfid-android-client) och ev. överlappande
                // körningar ska aldrig kasta ut varandra hos brokern.
                clientId = "rfid-outbox-${settings.deviceId}-${UUID.randomUUID().toString().take(6)}",
                username = settings.mqttUsername.value,
                password = settings.getMqttPassword() // lösenord loggas aldrig
            )
            val t = MqttOutboxTransport(link, ReadingMqttEncoder(settings.deviceId))
            transport = t
            val result = OutboxDispatcher(store, t, RetryPolicy(settings.outboxRounds.value, ExponentialBackoff())).drain(force, skipPause)
            when (result) {
                is DrainResult.Drained -> {
                    Log.i(TAG, "Utkorgen tömd, skickade ${result.sent}")
                    Result.success()
                }
                is DrainResult.Blocked -> if (result.paused) {
                    Log.i(TAG, "Paus mellan omgångar efter ${result.sent} skickade: ${result.error}; nästa omgång om ${result.retryAfterMillis} ms")
                    OutboxScheduler(ctx).scheduleAfter(result.retryAfterMillis)
                    Result.success()
                } else {
                    Log.w(TAG, "Utkorgen stannade efter ${result.sent} skickade: ${result.error} (nästa försök tidigast om ${result.retryAfterMillis} ms)")
                    Result.retry()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Oväntat fel i utkorgens utskick", e)
            Result.retry()
        } finally {
            // Koppla ner även vid avbrott (REPLACE/stopp), på IO-tråd.
            val toClose = transport
            if (toClose != null) {
                withContext(NonCancellable + Dispatchers.IO) { toClose.close() }
            }
        }
    }

    companion object {
        private const val TAG = "OutboxWorker"
        const val KEY_FORCE = "force"
        const val KEY_SKIP_PAUSE = "skipPause"
    }
}
