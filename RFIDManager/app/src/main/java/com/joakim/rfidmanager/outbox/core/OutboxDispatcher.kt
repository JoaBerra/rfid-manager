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
     * [retryAfterMillis]. [paused] = true när väntan är en paus mellan omgångar (se [RetryPolicy]),
     * inte vanlig backoff.
     */
    data class Blocked(
        override val sent: Int,
        val retryAfterMillis: Long,
        val error: String?,
        val paused: Boolean = false
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
 * 3. Efter sista omgångens sista försök ([RetryPolicy.isGiveUp]; den äldre konstruktorn med maxAttempts = en omgång)
 *    markeras posten FAILED (lämnar kön; körningen stannar ändå). Den skickas bara igen efter
 *    [OutboxStore.requeue] / [OutboxStore.requeueFailed].
 * 4. Väntetid via [RetryPolicy]: backoff mellan försök i en omgång, [RoundsConfig.pauseMillis] när en
 *    omgång är slut. Utan [force] skickas inte en post som misslyckats förrän `lastAttemptAt + väntetid`
 *    passerat. Med `force = true` ignoreras backoff; pausen ignoreras också om `skipPause = true`
 *    ('Skicka nu'), men respekteras annars (t.ex. när nätverket kommer tillbaka).
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
    private val policy: RetryPolicy,
    private val clock: () -> Long = System::currentTimeMillis
) {
    /** Äldre form: en enda omgång om [maxAttempts] försök med [backoff] (ingen paus). */
    constructor(
        store: OutboxStore<T>,
        transport: OutboxTransport<T>,
        backoff: BackoffPolicy = ExponentialBackoff(),
        maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
        clock: () -> Long = System::currentTimeMillis
    ) : this(store, transport, RetryPolicy(RoundsConfig.singleRound(maxAttempts), backoff), clock)

    /**
     * @param force ignorera backoff mellan försök ('Skicka nu', nätverk tillbaka).
     * @param skipPause ignorera även pausen mellan omgångar. Standard = [force] ('Skicka nu' kringgår
     *   pausen). Anropare som vill tvinga fram ett försök men ändå vänta ut pausen (nätverk tillbaka)
     *   skickar `force = true, skipPause = false`.
     */
    suspend fun drain(force: Boolean = false, skipPause: Boolean = force): DrainResult {
        var sent = 0
        while (true) {
            val head = store.nextPending(1).firstOrNull() ?: return DrainResult.Drained(sent)

            val inPause = policy.isPause(head.attempts)
            if (!force || (!skipPause && inPause)) {
                val last = head.lastAttemptAt
                if (last != null && head.attempts > 0) {
                    val remaining = last + policy.delayMillis(head.attempts) - clock()
                    if (remaining > 0) return DrainResult.Blocked(sent, remaining, head.lastError, paused = inPause)
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
                    if (policy.isGiveUp(attempts)) {
                        store.markFailed(head.id, result.error, now)
                    } else {
                        store.markAttemptFailed(head.id, result.error, now)
                    }
                    return DrainResult.Blocked(sent, policy.delayMillis(attempts), result.error, paused = policy.isPause(attempts))
                }
            }
        }
    }

    companion object {
        const val DEFAULT_MAX_ATTEMPTS = RetryPolicy.DEFAULT_MAX_ATTEMPTS
    }
}
