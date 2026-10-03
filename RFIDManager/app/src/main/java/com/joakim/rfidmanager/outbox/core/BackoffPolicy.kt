package com.joakim.rfidmanager.outbox.core

import kotlin.math.min
import kotlin.math.pow

/** Bestämmer hur länge en post ska vila efter ett misslyckat försök. */
fun interface BackoffPolicy {
    /** Väntetid i ms efter [failedAttempts] misslyckade försök (>= 1). Aldrig negativ. */
    fun delayMillis(failedAttempts: Int): Long
}

/**
 * Exponentiell backoff: base, base*factor, base*factor^2 … men aldrig mer än [maxMillis].
 * Standard: 30 s, 60 s, 120 s … tak 30 min.
 */
class ExponentialBackoff(
    private val baseMillis: Long = 30_000L,
    private val maxMillis: Long = 30 * 60_000L,
    private val factor: Double = 2.0
) : BackoffPolicy {
    init {
        require(baseMillis >= 0) { "baseMillis måste vara >= 0" }
        require(maxMillis >= baseMillis) { "maxMillis måste vara >= baseMillis" }
        require(factor >= 1.0) { "factor måste vara >= 1" }
    }

    override fun delayMillis(failedAttempts: Int): Long {
        if (failedAttempts <= 0) return 0L
        val raw = baseMillis * factor.pow(failedAttempts - 1)
        return if (raw.isNaN() || raw >= maxMillis) maxMillis else min(raw.toLong(), maxMillis)
    }
}

/** Ingen väntetid (tester, eller när anroparen själv styr tidpunkten). */
object NoBackoff : BackoffPolicy {
    override fun delayMillis(failedAttempts: Int): Long = 0L
}
