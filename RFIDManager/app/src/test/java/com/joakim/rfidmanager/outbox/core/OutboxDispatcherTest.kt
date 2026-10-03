package com.joakim.rfidmanager.outbox.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Transport som spelar upp förprogrammerade svar och loggar varje sändning. */
private class ScriptedTransport(private val script: (OutboxEntry<String>, Int) -> SendResult) : OutboxTransport<String> {
    val sentIds = mutableListOf<Long>()
    private val callsPerId = HashMap<Long, Int>()
    override suspend fun send(entry: OutboxEntry<String>): SendResult {
        sentIds.add(entry.id)
        val n = (callsPerId[entry.id] ?: 0) + 1
        callsPerId[entry.id] = n
        return script(entry, n)
    }
}

class OutboxDispatcherTest {

    private var now = 1_000_000L
    private fun store(vararg ids: Long) = InMemoryOutboxStore<String>().also { s ->
        // createdAt i omvänd id-ordning visar att ordningen är tid, inte id
        ids.forEachIndexed { i, id -> s.add(id, "post$id", createdAt = 100L + i) }
    }

    private fun dispatcher(
        s: InMemoryOutboxStore<String>,
        t: OutboxTransport<String>,
        backoff: BackoffPolicy = ExponentialBackoff(baseMillis = 1_000, maxMillis = 8_000),
        maxAttempts: Int = 3
    ) = OutboxDispatcher(s, t, backoff, maxAttempts) { now }

    @Test fun skickar_i_tidsordning_en_i_taget_och_markerar_SENT() = runBlocking {
        val s = store(30, 10, 20) // createdAt 100,101,102 → ordning 30,10,20
        val t = ScriptedTransport { _, _ -> SendResult.Acked }
        val r = dispatcher(s, t).drain()
        assertEquals(DrainResult.Drained(3), r)
        assertEquals(listOf(30L, 10L, 20L), t.sentIds)
        listOf(10L, 20L, 30L).forEach {
            assertEquals(OutboxStatus.SENT, s.statusOf(it))
            assertEquals(1, s.attemptsOf(it))
            assertEquals(now, s.sentAtOf(it))
        }
        assertEquals(0, s.countPending())
    }

    @Test fun tom_kö_ger_Drained_0() = runBlocking {
        val t = ScriptedTransport { _, _ -> SendResult.Acked }
        assertEquals(DrainResult.Drained(0), dispatcher(store(), t).drain())
        assertTrue(t.sentIds.isEmpty())
    }

    @Test fun stannar_vid_fel_och_skickar_inte_senare_poster_förbi() = runBlocking {
        val s = store(1, 2, 3)
        val t = ScriptedTransport { e, _ -> if (e.id == 2L) SendResult.Failed("broker nere") else SendResult.Acked }
        val r = dispatcher(s, t).drain() as DrainResult.Blocked
        assertEquals(1, r.sent)
        assertEquals("broker nere", r.error)
        assertEquals(listOf(1L, 2L), t.sentIds) // 3 skickades aldrig
        assertEquals(OutboxStatus.SENT, s.statusOf(1))
        assertEquals(OutboxStatus.PENDING, s.statusOf(2))
        assertEquals(OutboxStatus.PENDING, s.statusOf(3))
        assertEquals(1, s.attemptsOf(2))
        assertEquals("broker nere", s.lastErrorOf(2))
        assertEquals(0, s.attemptsOf(3))
    }

    @Test fun undantag_från_transport_blir_misslyckat_försök() = runBlocking {
        val s = store(1)
        val t = ScriptedTransport { _, _ -> throw IllegalStateException("kaboom") }
        val r = dispatcher(s, t).drain() as DrainResult.Blocked
        assertEquals("kaboom", r.error)
        assertEquals(OutboxStatus.PENDING, s.statusOf(1))
        assertEquals(1, s.attemptsOf(1))
    }

    @Test fun avbrott_kastas_vidare_och_posten_förblir_PENDING() {
        val s = store(1)
        val t = ScriptedTransport { _, _ -> throw CancellationException("avbruten") }
        try {
            runBlocking { dispatcher(s, t).drain() }
            fail("förväntade CancellationException")
        } catch (e: CancellationException) {
            // ok
        }
        assertEquals(OutboxStatus.PENDING, s.statusOf(1))
        assertEquals(0, s.attemptsOf(1))
    }

    @Test fun backoff_respekteras_utan_force_och_ignoreras_med_force() = runBlocking {
        val s = store(1)
        var ack = false
        val t = ScriptedTransport { _, _ -> if (ack) SendResult.Acked else SendResult.Failed("nere") }
        val d = dispatcher(s, t)

        val first = d.drain() as DrainResult.Blocked
        assertEquals(1_000L, first.retryAfterMillis) // 1 misslyckat försök → base
        assertEquals(1, t.sentIds.size)

        // Direkt igen: vilar i backoff, ingen sändning
        now += 400
        val waiting = d.drain() as DrainResult.Blocked
        assertEquals(600L, waiting.retryAfterMillis)
        assertEquals("nere", waiting.error)
        assertEquals(1, t.sentIds.size)

        // Med force skickas direkt
        ack = true
        assertEquals(DrainResult.Drained(1), d.drain(force = true))
        assertEquals(2, t.sentIds.size)
    }

    @Test fun backoff_gått_ut_ger_nytt_försök_utan_force() = runBlocking {
        val s = store(1)
        var ack = false
        val t = ScriptedTransport { _, _ -> if (ack) SendResult.Acked else SendResult.Failed("nere") }
        val d = dispatcher(s, t)
        d.drain()
        now += 1_000
        ack = true
        assertEquals(DrainResult.Drained(1), d.drain())
    }

    @Test fun max_försök_ger_FAILED_och_stannar_men_nästa_körning_går_vidare() = runBlocking {
        val s = store(1, 2)
        val t = ScriptedTransport { e, _ -> if (e.id == 1L) SendResult.Failed("avvisad") else SendResult.Acked }
        val d = dispatcher(s, t, maxAttempts = 3)

        d.drain(force = true); d.drain(force = true)
        assertEquals(OutboxStatus.PENDING, s.statusOf(1))
        assertEquals(2, s.attemptsOf(1))
        val third = d.drain(force = true) as DrainResult.Blocked
        assertEquals("avvisad", third.error)
        assertEquals(OutboxStatus.FAILED, s.statusOf(1))
        assertEquals(3, s.attemptsOf(1))
        assertEquals("avvisad", s.lastErrorOf(1))
        assertEquals(OutboxStatus.PENDING, s.statusOf(2)) // 2 har inte rörts

        // FAILED-posten lämnar kön; nästa körning skickar post 2
        assertEquals(DrainResult.Drained(1), d.drain())
        assertEquals(OutboxStatus.SENT, s.statusOf(2))
        assertEquals(OutboxStatus.FAILED, s.statusOf(1)) // skickas inte om automatiskt
    }

    @Test fun requeue_gör_FAILED_skickbar_igen_med_nollställda_försök() = runBlocking {
        val s = store(1)
        var ack = false
        val t = ScriptedTransport { _, _ -> if (ack) SendResult.Acked else SendResult.Failed("x") }
        val d = dispatcher(s, t, maxAttempts = 1)
        d.drain()
        assertEquals(OutboxStatus.FAILED, s.statusOf(1))

        assertEquals(DrainResult.Drained(0), d.drain(force = true)) // FAILED skickas inte av sig själv
        s.requeue(1)
        assertEquals(OutboxStatus.PENDING, s.statusOf(1))
        assertEquals(0, s.attemptsOf(1))
        ack = true
        assertEquals(DrainResult.Drained(1), d.drain())
        assertEquals(OutboxStatus.SENT, s.statusOf(1))
    }

    @Test fun requeueFailed_räknar_och_rör_inte_SENT() = runBlocking {
        val s = store(1, 2, 3)
        s.markFailed(1, "e", 1); s.markFailed(2, "e", 1); s.markSent(3, 1)
        assertEquals(2, s.requeueFailed())
        assertEquals(2, s.countPending())
        assertEquals(OutboxStatus.SENT, s.statusOf(3))
    }

    @Test fun lyckad_ack_skickar_aldrig_samma_post_två_gånger() = runBlocking {
        val s = store(1, 2)
        val t = ScriptedTransport { _, _ -> SendResult.Acked }
        val d = dispatcher(s, t)
        d.drain(); d.drain(); d.drain(force = true)
        assertEquals(listOf(1L, 2L), t.sentIds)
    }

    @Test fun lyckad_efter_fel_skickas_en_gång_till_efter_ack_och_aldrig_mer() = runBlocking {
        val s = store(1)
        val t = ScriptedTransport { _, n -> if (n == 1) SendResult.Failed("tillfälligt") else SendResult.Acked }
        val d = dispatcher(s, t)
        d.drain()
        now += 10_000
        assertEquals(DrainResult.Drained(1), d.drain())
        assertEquals(DrainResult.Drained(0), d.drain())
        assertEquals(listOf(1L, 1L), t.sentIds)
        assertEquals(2, s.attemptsOf(1))
        assertNull(s.lastErrorOf(1)) // felet rensas vid SENT
    }

    @Test fun idempotens_samma_post_kan_skickas_igen_efter_att_markSent_uteblev() = runBlocking {
        // Simulerar krasch efter ack men före markSent: lagret fick aldrig veta om acken.
        val s = store(1)
        val deliveries = mutableListOf<Long>() // vad mottagaren faktiskt tar emot
        val failMarkSent = object : OutboxStore<String> by s {
            override suspend fun markSent(id: Long, at: Long) = throw IllegalStateException("krasch")
        }
        val t = ScriptedTransport { e, _ -> deliveries.add(e.id); SendResult.Acked }
        try {
            runBlocking { OutboxDispatcher(failMarkSent, t, NoBackoff, 3) { now }.drain() }
            fail("förväntade krasch")
        } catch (e: IllegalStateException) { /* ok */ }
        assertEquals(OutboxStatus.PENDING, s.statusOf(1))
        // Nästa körning skickar om samma id (mottagaren avdupliceras på id) och slutar sedan
        assertEquals(DrainResult.Drained(1), dispatcher(s, t).drain())
        assertEquals(listOf(1L, 1L), deliveries)
        assertEquals(DrainResult.Drained(0), dispatcher(s, t).drain())
    }

    @Test fun BackoffPolicy_exponentiell_med_tak() {
        val b = ExponentialBackoff(baseMillis = 1_000, maxMillis = 8_000)
        assertEquals(0L, b.delayMillis(0))
        assertEquals(1_000L, b.delayMillis(1))
        assertEquals(2_000L, b.delayMillis(2))
        assertEquals(4_000L, b.delayMillis(3))
        assertEquals(8_000L, b.delayMillis(4))
        assertEquals(8_000L, b.delayMillis(5))
        assertEquals(8_000L, b.delayMillis(10_000)) // ingen overflow
        assertEquals(30 * 60_000L, ExponentialBackoff().delayMillis(100))
        assertEquals(0L, NoBackoff.delayMillis(5))
    }

    @Test fun ogiltiga_parametrar_avvisas() {
        try { OutboxDispatcher(InMemoryOutboxStore<String>(), ScriptedTransport { _, _ -> SendResult.Acked }, maxAttempts = 0); fail() } catch (_: IllegalArgumentException) {}
        try { ExponentialBackoff(baseMillis = 10, maxMillis = 5); fail() } catch (_: IllegalArgumentException) {}
    }
}
