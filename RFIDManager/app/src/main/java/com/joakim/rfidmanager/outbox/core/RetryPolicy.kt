package com.joakim.rfidmanager.outbox.core

/**
 * Mönster för omförsök i omgångar: [attemptsPerRound] försök med växande väntetid (backoff),
 * sedan [pauseMinutes] minuters paus, och så vidare i totalt [rounds] omgångar. Först efter sista
 * omgångens sista misslyckade försök blir posten FAILED.
 *
 * Ren konfiguration utan beroenden (generisk: appen väljer själv värdena). Gränserna som appens
 * inställningar tillåter (1–100, 1–1440, 1–20) hör till appen, inte kärnan.
 *
 * Med `rounds = 1` är beteendet identiskt med det gamla "max antal försök, sedan FAILED"
 * (se [singleRound]).
 */
data class RoundsConfig(
    val attemptsPerRound: Int = DEFAULT_ATTEMPTS_PER_ROUND,
    val pauseMinutes: Int = DEFAULT_PAUSE_MINUTES,
    val rounds: Int = DEFAULT_ROUNDS
) {
    init {
        require(attemptsPerRound >= 1) { "attemptsPerRound måste vara >= 1" }
        require(pauseMinutes >= 1) { "pauseMinutes måste vara >= 1" }
        require(rounds >= 1) { "rounds måste vara >= 1" }
    }

    /** Pausens längd i ms. */
    val pauseMillis: Long get() = pauseMinutes * 60_000L

    /** Högsta antal försök innan FAILED: [attemptsPerRound] × [rounds]. */
    val totalAttempts: Long get() = attemptsPerRound.toLong() * rounds

    companion object {
        const val DEFAULT_ATTEMPTS_PER_ROUND = 12
        const val DEFAULT_PAUSE_MINUTES = 60
        const val DEFAULT_ROUNDS = 3

        /** En enda omgång om [maxAttempts] försök: samma som det äldre beteendet utan paus. */
        fun singleRound(maxAttempts: Int) = RoundsConfig(attemptsPerRound = maxAttempts, rounds = 1)
    }
}

/**
 * Beslutar vad som händer efter ett visst antal misslyckade försök. Allt härleds ur antalet
 * misslyckade försök (`attempts`) – inget rundnummer lagras, så Room-schemat är oförändrat.
 *
 * - Försök n är det n:te misslyckade (n ≥ 1). Omgång r (1-baserad) innehåller försök
 *   (r-1)·A+1 … r·A, där A = [RoundsConfig.attemptsPerRound].
 * - Efter försök n som avslutar en omgång men inte sista omgången: **paus** ([RoundsConfig.pauseMillis]).
 * - Efter försök n ≥ A·R: **ge upp** (FAILED).
 * - Annars: [backoff] på försökets plats i omgången (1…A), så att väntetiden börjar om varje omgång.
 */
class RetryPolicy(
    val config: RoundsConfig = RoundsConfig.singleRound(DEFAULT_MAX_ATTEMPTS),
    private val backoff: BackoffPolicy = ExponentialBackoff()
) {
    /** Efter [failedAttempts] misslyckade försök är det dags att ge upp (FAILED). */
    fun isGiveUp(failedAttempts: Int): Boolean = failedAttempts.toLong() >= config.totalAttempts

    /** Efter [failedAttempts] följer en paus (omgången är slut men inte sista omgången). */
    fun isPause(failedAttempts: Int): Boolean =
        failedAttempts > 0 &&
            failedAttempts % config.attemptsPerRound == 0 &&
            failedAttempts.toLong() < config.totalAttempts

    /** Väntetid i ms efter [failedAttempts] misslyckade försök: paus vid omgångsslut, annars backoff. 0 om inga försök gjorts. */
    fun delayMillis(failedAttempts: Int): Long = when {
        failedAttempts <= 0 -> 0L
        isPause(failedAttempts) -> config.pauseMillis
        else -> backoff.delayMillis((failedAttempts - 1) % config.attemptsPerRound + 1)
    }

    /**
     * Pågående omgång, 1-baserad, för en post med [failedAttempts] misslyckade försök
     * (under paus: omgången som startar härnäst). Aldrig större än antalet omgångar.
     */
    fun currentRound(failedAttempts: Int): Int =
        minOf(failedAttempts.coerceAtLeast(0) / config.attemptsPerRound + 1, config.rounds)

    /**
     * Tidpunkt (ms sedan epoch) då nästa omgång startar för en post som står i paus, annars null.
     * Pausen räknas från [lastAttemptAt]; saknas den kan inget klockslag anges (null).
     */
    fun nextRoundStartsAt(failedAttempts: Int, lastAttemptAt: Long?): Long? =
        if (isPause(failedAttempts) && lastAttemptAt != null) lastAttemptAt + config.pauseMillis else null

    companion object {
        /** Hela minuter kvar (uppåtrundat, aldrig negativt) från [now] till [resumeAt]: 59 s kvar = 1 min. */
        fun minutesUntil(resumeAt: Long, now: Long): Long =
            if (resumeAt <= now) 0L else (resumeAt - now + 59_999L) / 60_000L

        /** Det äldre standardvärdet för max antal försök (en omgång). */
        const val DEFAULT_MAX_ATTEMPTS = 12
    }
}
