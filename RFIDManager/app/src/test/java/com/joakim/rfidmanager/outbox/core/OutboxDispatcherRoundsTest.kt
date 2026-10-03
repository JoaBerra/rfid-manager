package com.joakim.rfidmanager.outbox.core

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Dispatchern med omgångar: 3 försök per omgång, 60 min paus, 3 omgångar → FAILED efter försök 9. */
class OutboxDispatcherRoundsTest {
    private var now = 1_000_000L
    private val pause = 60 * 60_000L
    private val backoff = ExponentialBackoff(baseMillis = 1_000, maxMillis = 8_000)
    private val config = RoundsConfig(attemptsPerRound = 3, pauseMinutes = 60, rounds = 3)

    private class FlakyTransport(var ack: Boolean = false, var error: String = "ingen täckning") : OutboxTransport<String> {
        var sends = 0
        override suspend fun send(entry: OutboxEntry<String>): SendResult {
            sends++
            return if (ack) SendResult.Acked else SendResult.Failed(error)
        }
    }

    private fun store() = InMemoryOutboxStore<String>().also { it.add(1, "post1", createdAt = 100) }
    private fun dispatcher(s: InMemoryOutboxStore<String>, t: OutboxTransport<String>, c: RoundsConfig = config) =
        OutboxDispatcher(s, t, RetryPolicy(c, backoff)) { now }

    /** Kör `drain()` (utan force) och väntar ut varje Blocked tills posten är FAILED/SENT. Returnerar alla Blocked. */
    private fun runToEnd(d: OutboxDispatcher<String>, s: InMemoryOutboxStore<String>): List<DrainResult.Blocked> {
        val blocked = mutableListOf<DrainResult.Blocked>()
        var guard = 0
        while (s.statusOf(1) == OutboxStatus.PENDING && guard++ < 100) {
            val r = runBlocking { d.drain() }
            if (r is DrainResult.Blocked) { blocked += r; now += r.retryAfterMillis }
        }
        return blocked
    }

    @Test fun tre_omgångar_9_försök_FAILED_först_efter_sista() {
        val s = store(); val t = FlakyTransport(); val d = dispatcher(s, t)
        val blocked = runToEnd(d, s)
        assertEquals(9, t.sends)
        assertEquals(OutboxStatus.FAILED, s.statusOf(1))
        assertEquals(9, s.attemptsOf(1))
        assertEquals(9, blocked.size)
        // paus efter försök 3 och 6, inte efter 9 (FAILED)
        assertEquals(listOf(false, false, true, false, false, true, false, false, false), blocked.map { it.paused })
        assertEquals(pause, blocked[2].retryAfterMillis)
        assertEquals(pause, blocked[5].retryAfterMillis)
        assertFalse(blocked[8].retryAfterMillis == pause)
    }

    @Test fun posten_är_väntar_med_felorsak_under_hela_pausen() = runBlocking {
        val s = store(); val t = FlakyTransport(error = "Not Authorised"); val d = dispatcher(s, t)
        repeat(3) { d.drain(force = true) } // 3 misslyckade = omgång 1 slut
        assertEquals(OutboxStatus.PENDING, s.statusOf(1))
        assertEquals(3, s.attemptsOf(1))
        assertEquals("Not Authorised", s.lastErrorOf(1))
        val r = d.drain() as DrainResult.Blocked
        assertTrue(r.paused)
        assertEquals(pause, r.retryAfterMillis)
        assertEquals("Not Authorised", r.error)
        assertEquals(3, t.sends) // ingen sändning under pausen
        now += 10 * 60_000
        val r2 = d.drain() as DrainResult.Blocked
        assertEquals(pause - 10 * 60_000, r2.retryAfterMillis)
        assertTrue(r2.paused)
        assertEquals(3, t.sends)
    }

    @Test fun efter_pausen_startar_nästa_omgång_och_backoff_börjar_om() = runBlocking {
        val s = store(); val t = FlakyTransport(); val d = dispatcher(s, t)
        repeat(3) { d.drain(force = true) }
        now += pause
        val r = d.drain() as DrainResult.Blocked // försök 4
        assertEquals(4, t.sends)
        assertFalse(r.paused)
        assertEquals(1_000L, r.retryAfterMillis) // första försöket i omgång 2 → bas-backoff
    }

    @Test fun nätverk_tillbaka_hoppar_över_backoff_men_inte_pausen() = runBlocking {
        val s = store(); val t = FlakyTransport(); val d = dispatcher(s, t)
        d.drain(force = true) // försök 1, vilar i backoff
        // nätverk tillbaka mitt i en omgång: backoff ignoreras
        d.drain(force = true, skipPause = false)
        assertEquals(2, t.sends)
        d.drain(force = true) // försök 3 → paus
        assertEquals(3, t.sends)
        t.ack = true
        val r = d.drain(force = true, skipPause = false) as DrainResult.Blocked
        assertTrue(r.paused)
        assertEquals(3, t.sends) // pausen hölls
        assertEquals(OutboxStatus.PENDING, s.statusOf(1))
    }

    @Test fun nätverk_tillbaka_efter_pausens_slut_skickar() = runBlocking {
        val s = store(); val t = FlakyTransport(); val d = dispatcher(s, t)
        repeat(3) { d.drain(force = true) }
        now += pause
        t.ack = true
        assertEquals(DrainResult.Drained(1), d.drain(force = true, skipPause = false))
        assertEquals(OutboxStatus.SENT, s.statusOf(1))
    }

    @Test fun skicka_nu_kringgår_pausen() = runBlocking {
        val s = store(); val t = FlakyTransport(); val d = dispatcher(s, t)
        repeat(3) { d.drain(force = true) }
        t.ack = true
        assertEquals(DrainResult.Drained(1), d.drain(force = true)) // skipPause = force
        assertEquals(OutboxStatus.SENT, s.statusOf(1))
        assertEquals(4, t.sends)
    }

    @Test fun skicka_nu_som_misslyckas_räknas_som_första_försöket_i_nästa_omgång() = runBlocking {
        val s = store(); val t = FlakyTransport(); val d = dispatcher(s, t)
        repeat(3) { d.drain(force = true) }
        val r = d.drain(force = true) as DrainResult.Blocked
        assertEquals(4, s.attemptsOf(1))
        assertFalse(r.paused)
        assertEquals(1_000L, r.retryAfterMillis)
        assertEquals(OutboxStatus.PENDING, s.statusOf(1))
    }

    @Test fun skicka_nu_köar_om_FAILED_och_omgångarna_börjar_om() = runBlocking {
        val s = store(); val t = FlakyTransport(); val d = dispatcher(s, t)
        repeat(9) { d.drain(force = true) }
        assertEquals(OutboxStatus.FAILED, s.statusOf(1))
        assertEquals(1, s.requeueFailed())
        assertEquals(0, s.attemptsOf(1))
        assertEquals(OutboxStatus.PENDING, s.statusOf(1))
        t.ack = true
        assertEquals(DrainResult.Drained(1), d.drain(force = true))
    }

    @Test fun lyckas_mitt_i_senare_omgång_ger_SENT() = runBlocking {
        val s = store(); val t = FlakyTransport(); val d = dispatcher(s, t)
        repeat(3) { d.drain(force = true) }
        now += pause
        repeat(3) { d.drain(force = true) } // omgång 2 slut
        now += pause
        t.ack = true
        assertEquals(DrainResult.Drained(1), d.drain())
        assertEquals(OutboxStatus.SENT, s.statusOf(1))
        assertEquals(7, s.attemptsOf(1))
    }

    @Test fun inställbart_en_omgång_ger_det_gamla_beteendet_utan_paus() {
        val s = store(); val t = FlakyTransport()
        val d = dispatcher(s, t, RoundsConfig(attemptsPerRound = 4, pauseMinutes = 60, rounds = 1))
        val blocked = runToEnd(d, s)
        assertEquals(4, t.sends)
        assertEquals(OutboxStatus.FAILED, s.statusOf(1))
        assertTrue(blocked.none { it.paused })
    }

    @Test fun gamla_konstruktorn_med_maxAttempts_är_en_omgång() {
        val s = store(); val t = FlakyTransport()
        val d = OutboxDispatcher(s, t, backoff, 3) { now }
        val blocked = runToEnd(d, s)
        assertEquals(3, t.sends)
        assertEquals(OutboxStatus.FAILED, s.statusOf(1))
        assertTrue(blocked.none { it.paused })
        assertEquals(backoff.delayMillis(1), blocked[0].retryAfterMillis) // backoff som förut
        assertEquals(backoff.delayMillis(3), blocked[2].retryAfterMillis)
    }

    @Test fun standardomgång_36_försök_innan_FAILED() {
        val s = store(); val t = FlakyTransport()
        val d = OutboxDispatcher(s, t, RetryPolicy(RoundsConfig(), ExponentialBackoff())) { now }
        val blocked = runToEnd(d, s)
        assertEquals(36, t.sends)
        assertEquals(OutboxStatus.FAILED, s.statusOf(1))
        assertEquals(listOf(12, 24), blocked.mapIndexedNotNull { i, b -> if (b.paused) i + 1 else null })
    }
}
