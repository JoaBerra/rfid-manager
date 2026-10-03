package com.joakim.rfidmanager.outbox.core

import kotlin.coroutines.cancellation.CancellationException

/** Utfall av en [OutboxDispatcher.drain]. [sent] = antal poster som bekräftades under körningen. */
sealed interface DrainResult {
    val sent: Int

    /** Kön är tom (inga PENDING kvar). */
    data class Drained(override val sent: Int) : DrainResult

    /**
     * Stannade med PENDING-poster kvar: antingen misslyckades ett försök ([error] satt) eller
     * så vilar första posten ännu i backoff ([error] = senaste felet). Försök igen om
     * [retryAfterMillis].
     */
    data class Blocked(
        override val sent: Int,
        val retryAfterMillis: Long,
        val error: String?
    ) : DrainResult
}

/**
 * Kärnlogiken: tar väntande poster i ordning och skickar dem en i taget.
 *
 * Regler:
 * 1. Äldsta PENDING-posten först. Posten markeras SENT först när transporten returnerat
 *    [SendResult.Acked]; därefter hämtas nästa.
 * 2. Vid misslyckat försök stannar körningen direkt (nästa post skickas inte förbi),
 *    posten får attempts+1 och felorsaken sparas ([DrainResult.Blocked]).
 * 3. Efter [maxAttempts] misslyckade försök markeras posten FAILED (lämnar kön; körningen
 *    stannar ändå). Den skickas bara igen efter [OutboxStore.requeue] / [OutboxStore.requeueFailed].
 * 4. Exponentiell (eller annan) backoff via [BackoffPolicy]: utan [force] skickas inte en post
 *    som misslyckats förrän `lastAttemptAt + backoff` passerat. Med `force = true` ignoreras backoff.
 * 5. At-least-once: kraschar processen efter ack men före [OutboxStore.markSent] skickas
 *    posten igen – mottagaren måste avduplicera på id.
 * 6. [CancellationException] kastas vidare orört (posten förblir PENDING).
 *
 * Klassen har inget eget tillstånd och är inte trådsäker mot parallella `drain` på samma
 * lager; låt anroparen serialisera (WorkManager unik arbetskö gör det i appen).
 */
class OutboxDispatcher<T>(
    private val store: OutboxStore<T>,
    private val transport: OutboxTransport<T>,
    private val backoff: BackoffPolicy = ExponentialBackoff(),
    private val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
    private val clock: () -> Long = System::currentTimeMillis
) {
    init {
        require(maxAttempts >= 1) { "maxAttempts måste vara >= 1" }
    }

    suspend fun drain(force: Boolean = false): DrainResult {
        var sent = 0
        while (true) {
            val head = store.nextPending(1).firstOrNull() ?: return DrainResult.Drained(sent)

            if (!force) {
                val last = head.lastAttemptAt
                if (last != null && head.attempts > 0) {
                    val remaining = last + backoff.delayMillis(head.attempts) - clock()
                    if (remaining > 0) return DrainResult.Blocked(sent, remaining, head.lastError)
                }
            }

            val result = try {
                transport.send(head)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                SendResult.Failed(e.message ?: e.javaClass.simpleName)
            }

            when (result) {
                SendResult.Acked -> {
                    store.markSent(head.id, clock())
                    sent++
                }
                is SendResult.Failed -> {
                    val attempts = head.attempts + 1
                    val now = clock()
                    if (attempts >= maxAttempts) {
                        store.markFailed(head.id, result.error, now)
                    } else {
                        store.markAttemptFailed(head.id, result.error, now)
                    }
                    return DrainResult.Blocked(sent, backoff.delayMillis(attempts), result.error)
                }
            }
        }
    }

    companion object {
        const val DEFAULT_MAX_ATTEMPTS = 12
    }
}
