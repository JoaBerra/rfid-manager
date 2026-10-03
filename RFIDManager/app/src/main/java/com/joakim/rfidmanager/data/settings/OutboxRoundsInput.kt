package com.joakim.rfidmanager.data.settings

import com.joakim.rfidmanager.outbox.core.RoundsConfig

/**
 * Validering av utkorgens omgångsinställningar (tre textfält i Inställningar). Ren Kotlin, enhetstestad.
 * Gränserna gäller appens inställningar; kärnan ([RoundsConfig]) är generisk och kräver bara >= 1.
 */
object OutboxRoundsInput {
    val ATTEMPTS_RANGE = 1..100
    val PAUSE_MINUTES_RANGE = 1..1440
    val ROUNDS_RANGE = 1..20

    /** Vilket fält ett fel gäller. */
    enum class Field { ATTEMPTS, PAUSE_MINUTES, ROUNDS }

    sealed interface Result {
        data class Valid(val config: RoundsConfig) : Result

        /** Fält som inte är heltal i sitt tillåtna intervall (tomt, bokstäver, för stort/litet). */
        data class Invalid(val fields: Set<Field>) : Result
    }

    fun validate(attempts: String, pauseMinutes: String, rounds: String): Result {
        val a = parse(attempts, ATTEMPTS_RANGE)
        val p = parse(pauseMinutes, PAUSE_MINUTES_RANGE)
        val r = parse(rounds, ROUNDS_RANGE)
        val bad = buildSet {
            if (a == null) add(Field.ATTEMPTS)
            if (p == null) add(Field.PAUSE_MINUTES)
            if (r == null) add(Field.ROUNDS)
        }
        return if (bad.isEmpty()) Result.Valid(RoundsConfig(a!!, p!!, r!!)) else Result.Invalid(bad)
    }

    private fun parse(text: String, range: IntRange): Int? =
        text.trim().toIntOrNull()?.takeIf { it in range }
}
