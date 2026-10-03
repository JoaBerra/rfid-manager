package com.joakim.rfidmanager.outbox.work

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Startar utkorgens utskick via WorkManager. Alla anrop går till EN unik arbetskö ([UNIQUE_NAME]) med
 * nätverkskrav CONNECTED och exponentiell backoff, så att utskick sker även när appen är stängd och
 * så att aldrig två körningar pågår samtidigt.
 *
 * Köpolicy:
 * - [onReadingSaved] / [onAppStart]: APPEND_OR_REPLACE – läggs sist i kön. Ingen risk att en avläsning
 *   som sparas medan en körning håller på att avsluta missas, och en körning i backoff trampas inte på.
 * - [sendNow]: REPLACE med force och skipPause – ersätter väntande/pågående körning och försöker direkt,
 *   kringgår både backoff och pausen mellan omgångar. (Pågående körning avbryts; posten förblir väntande
 *   och skickas om, vilket är ofarligt tack vare dubblettskyddet.)
 * - [onNetworkAvailable]: REPLACE med force men UTAN skipPause – hoppar över backoff men inte pausen mellan
 *   omgångar. Står första posten i paus planerar workern om körningen till pausens slut.
 * - [scheduleAfter]: planerar nästa körning efter en paus (initialDelay). WorkManager lagrar jobbet, så det
 *   överlever omstart av appen och telefonen. REPLACE, så att bara en väntande körning finns.
 */
class OutboxScheduler(context: Context) {
    private val appContext = context.applicationContext

    fun onReadingSaved() = enqueue(ExistingWorkPolicy.APPEND_OR_REPLACE, force = false)

    fun onAppStart() = enqueue(ExistingWorkPolicy.APPEND_OR_REPLACE, force = false)

    fun onNetworkAvailable() = enqueue(ExistingWorkPolicy.REPLACE, force = true, skipPause = false)

    fun sendNow() = enqueue(ExistingWorkPolicy.REPLACE, force = true, skipPause = true)

    /** Utkorgsinställningarna (omgångar/paus) ändrades: låt workern räkna om tidpunkten (ingen force). */
    fun onSettingsChanged() = enqueue(ExistingWorkPolicy.REPLACE, force = false)

    /** Nästa körning efter [delayMillis] (pausens slut). Ersätter tidigare väntande körning. */
    fun scheduleAfter(delayMillis: Long) =
        enqueue(ExistingWorkPolicy.REPLACE, force = false, delayMillis = delayMillis.coerceAtLeast(0L))

    private fun enqueue(
        policy: ExistingWorkPolicy,
        force: Boolean,
        skipPause: Boolean = false,
        delayMillis: Long = 0L
    ) {
        try {
            val request = OneTimeWorkRequestBuilder<OutboxWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
                .setInputData(workDataOf(OutboxWorker.KEY_FORCE to force, OutboxWorker.KEY_SKIP_PAUSE to skipPause))
                .apply { if (delayMillis > 0) setInitialDelay(delayMillis, TimeUnit.MILLISECONDS) }
                .addTag(UNIQUE_NAME)
                .build()
            WorkManager.getInstance(appContext).enqueueUniqueWork(UNIQUE_NAME, policy, request)
        } catch (e: Exception) {
            // Att schemalägga får aldrig krascha appen; posten ligger kvar som väntande och tas vid nästa trigger.
            Log.e(TAG, "Kunde inte schemalägga utskick", e)
        }
    }

    companion object {
        private const val TAG = "OutboxScheduler"
        const val UNIQUE_NAME = "rfid-outbox-send"
        private const val BACKOFF_SECONDS = 30L
    }
}

/**
 * Anropar [onAvailable] när ett nätverk blir tillgängligt (och en gång vid registrering om nätverk finns).
 * Registreras högst en gång per process. När appen är stängd sköter WorkManagers nätverkskrav detta.
 */
object OutboxNetworkTrigger {
    private const val TAG = "OutboxNetworkTrigger"
    private val registered = AtomicBoolean(false)

    fun register(context: Context, onAvailable: () -> Unit) {
        if (!registered.compareAndSet(false, true)) return
        try {
            val cm = context.applicationContext.getSystemService(ConnectivityManager::class.java) ?: return
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    try { onAvailable() } catch (e: Exception) { Log.e(TAG, "Fel i nätverkstrigger", e) }
                }
            })
        } catch (e: Exception) {
            registered.set(false)
            Log.e(TAG, "Kunde inte registrera nätverkstrigger", e)
        }
    }
}
