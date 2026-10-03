package com.joakim.rfidmanager.outbox.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RetryPolicyTest {
    private val backoff = ExponentialBackoff(baseMillis = 1_000, maxMillis = 8_000)
    private val pause = 60 * 60_000L

    // 3 försök per omgång, 60 min paus, 3 omgångar → FAILED efter försök 9
    private val policy = RetryPolicy(RoundsConfig(attemptsPerRound = 3, pauseMinutes = 60, rounds = 3), backoff)

    @Test fun standardvärden_är_12_försök_60_min_paus_3_omgångar() {
        val d = RoundsConfig()
        assertEquals(12, d.attemptsPerRound)
        assertEquals(60, d.pauseMinutes)
        assertEquals(3, d.rounds)
        assertEquals(36L, d.totalAttempts)
        assertEquals(pause, d.pauseMillis)
    }

    @Test fun omgångsgränser_paus_efter_varje_omgång_utom_den_sista() {
        val p = RetryPolicy(RoundsConfig(), ExponentialBackoff())
        val pauses = (1..40).filter { p.isPause(it) }
        assertEquals(listOf(12, 24), pauses)
        assertFalse(p.isPause(0))
        assertFalse(p.isPause(36)) // sista omgångens slut = ge upp, ingen paus
    }

    @Test fun FAILED_först_efter_sista_omgångens_sista_försök() {
        val p = RetryPolicy(RoundsConfig(), ExponentialBackoff())
        assertFalse(p.isGiveUp(11))
        assertFalse(p.isGiveUp(12)) // slut på omgång 1
        assertFalse(p.isGiveUp(24)) // slut på omgång 2
        assertFalse(p.isGiveUp(35))
        assertTrue(p.isGiveUp(36))
        assertTrue(p.isGiveUp(37))
    }

    @Test fun väntetid_backoff_i_omgång_paus_vid_omgångsslut_och_backoff_börjar_om() {
        assertEquals(0L, policy.delayMillis(0))
        assertEquals(1_000L, policy.delayMillis(1))
        assertEquals(2_000L, policy.delayMillis(2))
        assertEquals(pause, policy.delayMillis(3)) // omgång 1 slut
        assertEquals(1_000L, policy.delayMillis(4)) // omgång 2 börjar om
        assertEquals(2_000L, policy.delayMillis(5))
        assertEquals(pause, policy.delayMillis(6))
        assertEquals(1_000L, policy.delayMillis(7))
        assertEquals(4_000L, policy.delayMillis(9)) // ge upp-läge: ingen paus, backoff på plats 3
    }

    @Test fun en_omgång_är_identisk_med_det_gamla_beteendet() {
        val old = ExponentialBackoff()
        val p = RetryPolicy(RoundsConfig.singleRound(12), old)
        for (n in 1..11) {
            assertFalse("paus vid $n", p.isPause(n))
            assertFalse("ge upp vid $n", p.isGiveUp(n))
            assertEquals("backoff vid $n", old.delayMillis(n), p.delayMillis(n))
        }
        assertFalse(p.isPause(12))
        assertTrue(p.isGiveUp(12))
        assertEquals(old.delayMillis(12), p.delayMillis(12))
        assertNull(p.nextRoundStartsAt(12, 1_000L))
    }

    @Test fun nuvarande_omgång_räknas_från_försök_och_stannar_på_sista() {
        assertEquals(1, policy.currentRound(0))
        assertEquals(1, policy.currentRound(2))
        assertEquals(2, policy.currentRound(3)) // under paus: omgången som startar härnäst
        assertEquals(2, policy.currentRound(5))
        assertEquals(3, policy.currentRound(6))
        assertEquals(3, policy.currentRound(9))
        assertEquals(3, policy.currentRound(100))
    }

    @Test fun nästa_omgång_startar_vid_senaste_försök_plus_paus() {
        assertEquals(10_000L + pause, policy.nextRoundStartsAt(3, 10_000L))
        assertEquals(10_000L + pause, policy.nextRoundStartsAt(6, 10_000L))
        assertNull(policy.nextRoundStartsAt(2, 10_000L)) // mitt i omgång
        assertNull(policy.nextRoundStartsAt(9, 10_000L)) // sista = ge upp
        assertNull(policy.nextRoundStartsAt(3, null))
    }

    @Test fun minuter_kvar_rundas_uppåt_och_är_aldrig_negativa() {
        assertEquals(0L, RetryPolicy.minutesUntil(1_000, 1_000))
        assertEquals(0L, RetryPolicy.minutesUntil(1_000, 5_000))
        assertEquals(1L, RetryPolicy.minutesUntil(60_000, 1)) // 59,999 s
        assertEquals(1L, RetryPolicy.minutesUntil(1_000, 0))
        assertEquals(60L, RetryPolicy.minutesUntil(pause, 0))
        assertEquals(2L, RetryPolicy.minutesUntil(60_001, 0))
    }

    @Test fun ogiltig_konfiguration_nekas() {
        listOf(
            { RoundsConfig(0, 60, 3) }, { RoundsConfig(12, 0, 3) }, { RoundsConfig(12, 60, 0) },
            { RoundsConfig.singleRound(0) }
        ).forEach { make ->
            try { make(); fail("förväntade IllegalArgumentException") } catch (_: IllegalArgumentException) { }
        }
    }

    @Test fun stora_värden_räknas_utan_överflöde() {
        val p = RetryPolicy(RoundsConfig(attemptsPerRound = Int.MAX_VALUE, pauseMinutes = 1440, rounds = 20))
        assertFalse(p.isGiveUp(Int.MAX_VALUE - 1))
        assertEquals(Int.MAX_VALUE.toLong() * 20, p.config.totalAttempts)
    }
}
