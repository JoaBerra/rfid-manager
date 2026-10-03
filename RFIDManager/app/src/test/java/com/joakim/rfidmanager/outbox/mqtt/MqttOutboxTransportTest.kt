package com.joakim.rfidmanager.outbox.mqtt

import com.joakim.rfidmanager.domain.model.PersistedReading
import com.joakim.rfidmanager.outbox.core.InMemoryOutboxStore
import com.joakim.rfidmanager.outbox.core.NoBackoff
import com.joakim.rfidmanager.outbox.core.OutboxDispatcher
import com.joakim.rfidmanager.outbox.core.OutboxEntry
import com.joakim.rfidmanager.outbox.core.OutboxStatus
import com.joakim.rfidmanager.outbox.core.SendResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

private class FakeLink(var behaviour: (String, ByteArray) -> Unit = { _, _ -> }) : MqttLink {
    val published = mutableListOf<Pair<String, String>>()
    var closed = 0
    override suspend fun publishAndAwaitAck(topic: String, payload: ByteArray) {
        behaviour(topic, payload)
        published.add(topic to String(payload, Charsets.UTF_8))
    }
    override fun close() { closed++ }
}

private fun reading(id: Long, uid: String = "04A1B2C3", type: String = "RFID") = PersistedReading(
    id = id, type = type, uidOrCode = uid, timestamp = 1_700_000_000_000L + id, source = "NFC",
    memoryBank = 3, address = 4, length = 4, payload = "DEADBEEF"
)

private fun entry(r: PersistedReading) = OutboxEntry(r.id, r, r.timestamp)

class MqttOutboxTransportTest {
    private val encoder = ReadingMqttEncoder("abcd1234")

    @Test fun ack_när_länken_återvänder_och_rätt_topic_och_payload() = runBlocking {
        val link = FakeLink()
        val t = MqttOutboxTransport(link, encoder)
        assertEquals(SendResult.Acked, t.send(entry(reading(42))))
        val (topic, body) = link.published.single()
        assertEquals("rfidmanager/04A1B2C3/telemetry", topic)
        val j = JSONObject(body)
        assertEquals(42L, j.getLong("id"))
        assertEquals("abcd1234", j.getString("deviceId"))
        assertEquals("abcd1234:42", j.getString("messageId"))
        assertEquals("ReadEscortMemory", j.getString("type"))
        assertEquals("04A1B2C3", j.getString("uid"))
        assertEquals(1_700_000_000_042L, j.getLong("timestamp"))
        assertEquals("NFC", j.getString("source"))
        assertTrue(j.getBoolean("sparkplug"))
        assertEquals("DEADBEEF", j.getJSONObject("data").getString("payload"))
        assertEquals(3, j.getJSONObject("data").getInt("memoryBank"))
    }

    @Test fun samma_post_ger_alltid_samma_messageId_idempotens() {
        val a = JSONObject(String(encoder.payload(entry(reading(7))), Charsets.UTF_8))
        val b = JSONObject(String(encoder.payload(entry(reading(7))), Charsets.UTF_8))
        assertEquals(a.getString("messageId"), b.getString("messageId"))
        assertFalse(a.getString("messageId") == JSONObject(String(encoder.payload(entry(reading(8))), Charsets.UTF_8)).getString("messageId"))
    }

    @Test fun streckkod_ger_ReadBarcode_och_farliga_tecken_i_topic_ersätts() {
        val e = entry(reading(1, uid = "a/b+c#d", type = "EAN"))
        assertEquals("rfidmanager/a_b_c_d/telemetry", encoder.topic(e))
        assertEquals("ReadBarcode", JSONObject(String(encoder.payload(e), Charsets.UTF_8)).getString("type"))
    }

    @Test fun fel_från_länken_blir_Failed_med_orsak_utan_att_kasta() = runBlocking {
        val link = FakeLink { _, _ -> throw IllegalStateException("Kan inte nå brokern (timeout)") }
        val r = MqttOutboxTransport(link, encoder).send(entry(reading(1)))
        assertEquals(SendResult.Failed("Kan inte nå brokern (timeout)"), r)
        assertTrue(link.published.isEmpty())
    }

    @Test fun undantag_utan_text_ger_klassnamn() = runBlocking {
        val link = FakeLink { _, _ -> throw java.io.IOException() }
        assertEquals(SendResult.Failed("IOException"), MqttOutboxTransport(link, encoder).send(entry(reading(1))))
    }

    @Test fun avbrott_kastas_vidare() {
        val link = FakeLink { _, _ -> throw CancellationException("stopp") }
        try {
            runBlocking { MqttOutboxTransport(link, encoder).send(entry(reading(1))) }
            fail()
        } catch (e: CancellationException) { /* ok */ }
    }

    @Test fun close_stänger_länken() {
        val link = FakeLink()
        MqttOutboxTransport(link, encoder).close()
        assertEquals(1, link.closed)
    }

    @Test fun hela_kedjan_dispatcher_transport_utan_dubbelskick() = runBlocking {
        val store = InMemoryOutboxStore<PersistedReading>()
        (1L..3L).forEach { store.add(it, reading(it), createdAt = it) }
        var down = true
        val link = FakeLink { _, _ -> if (down) throw IllegalStateException("offline") }
        val d = OutboxDispatcher(store, MqttOutboxTransport(link, encoder), NoBackoff, 10) { 99 }

        d.drain(); d.drain() // offline: inget når brokern
        assertTrue(link.published.isEmpty())
        assertEquals(3, store.countPending())
        assertEquals(2, store.attemptsOf(1))

        down = false
        d.drain(); d.drain()
        val ids = link.published.map { JSONObject(it.second).getString("messageId") }
        assertEquals(listOf("abcd1234:1", "abcd1234:2", "abcd1234:3"), ids) // alla 3, en gång var, i ordning
        assertEquals(OutboxStatus.SENT, store.statusOf(3))
    }
}
