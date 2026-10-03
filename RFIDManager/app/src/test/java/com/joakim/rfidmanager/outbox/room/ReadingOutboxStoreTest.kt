package com.joakim.rfidmanager.outbox.room

import com.joakim.rfidmanager.data.local.dao.PersistedReadingDao
import com.joakim.rfidmanager.data.local.entities.PersistedReadingEntity
import com.joakim.rfidmanager.data.repository.parseOutboxStatus
import com.joakim.rfidmanager.data.repository.toDomain
import com.joakim.rfidmanager.domain.model.PersistedReading
import com.joakim.rfidmanager.outbox.core.DrainResult
import com.joakim.rfidmanager.outbox.core.NoBackoff
import com.joakim.rfidmanager.outbox.core.OutboxDispatcher
import com.joakim.rfidmanager.outbox.core.OutboxEntry
import com.joakim.rfidmanager.outbox.core.OutboxStatus
import com.joakim.rfidmanager.outbox.core.OutboxTransport
import com.joakim.rfidmanager.outbox.core.SendResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Minnesbaserad DAO som speglar semantiken i PersistedReadingDao-frågorna (för adaptertest på JVM). */
private class FakeDao : PersistedReadingDao {
    val rows = LinkedHashMap<Long, PersistedReadingEntity>()
    override suspend fun insert(reading: PersistedReadingEntity): Long { rows[reading.id] = reading; return reading.id }
    override suspend fun insertAll(readings: List<PersistedReadingEntity>) { readings.forEach { insert(it) } }
    override suspend fun insertAllStrict(readings: List<PersistedReadingEntity>) { readings.forEach { insert(it) } }
    override suspend fun getAllOnce() = rows.values.toList()
    override fun getAll(): Flow<List<PersistedReadingEntity>> = flowOf(rows.values.toList())
    override fun getByType(type: String): Flow<List<PersistedReadingEntity>> = flowOf(rows.values.filter { it.type == type })
    override suspend fun nextPending(limit: Int) =
        rows.values.filter { it.outboxStatus == "PENDING" }.sortedWith(compareBy({ it.timestamp }, { it.id })).take(limit)
    override suspend fun countPending() = rows.values.count { it.outboxStatus == "PENDING" }
    override fun observePendingCount(): Flow<Int> = flowOf(rows.values.count { it.outboxStatus == "PENDING" })
    override fun observeFailedCount(): Flow<Int> = flowOf(rows.values.count { it.outboxStatus == "FAILED" })
    override suspend fun markSent(id: Long, at: Long) {
        rows[id] = rows.getValue(id).copy(outboxStatus = "SENT", transmitted = true, status = "transmitted",
            attempts = rows.getValue(id).attempts + 1, lastError = null, lastAttemptAt = at, sentAt = at)
    }
    override suspend fun markAttemptFailed(id: Long, error: String, at: Long) {
        val r = rows.getValue(id); if (r.outboxStatus != "PENDING") return
        rows[id] = r.copy(attempts = r.attempts + 1, lastError = error, lastAttemptAt = at)
    }
    override suspend fun markFailed(id: Long, error: String, at: Long) {
        val r = rows.getValue(id)
        rows[id] = r.copy(outboxStatus = "FAILED", transmitted = false, status = "persisted",
            attempts = r.attempts + 1, lastError = error, lastAttemptAt = at)
    }
    override suspend fun requeue(id: Long): Int {
        val r = rows[id] ?: return 0; if (r.outboxStatus != "FAILED") return 0
        rows[id] = r.copy(outboxStatus = "PENDING", attempts = 0, lastAttemptAt = null); return 1
    }
    override suspend fun requeueFailed(): Int = rows.keys.toList().sumOf { requeue(it) }
    override suspend fun deleteOlderThan(cutoff: Long) { rows.values.removeAll { it.timestamp < cutoff && it.outboxStatus != "PENDING" } }
    override suspend fun deleteAll() { rows.clear() }
}

private fun entity(id: Long, ts: Long = 1000L + id, transmitted: Boolean = false) = PersistedReadingEntity(
    id = id, type = "RFID", uidOrCode = "UID$id", timestamp = ts, transmitted = transmitted
)

class ReadingOutboxStoreTest {

    @Test fun entitetens_utkorgsstatus_härleds_från_transmitted_för_äldre_kod() {
        assertEquals("PENDING", entity(1).outboxStatus)
        assertEquals("SENT", entity(1, transmitted = true).outboxStatus)
    }

    @Test fun nextPending_ger_endast_PENDING_äldst_först_med_mappade_fält() = runBlocking {
        val dao = FakeDao()
        dao.insert(entity(3, ts = 300)); dao.insert(entity(1, ts = 100)); dao.insert(entity(2, ts = 200, transmitted = true))
        val store = ReadingOutboxStore(dao)
        val list = store.nextPending(10)
        assertEquals(listOf(1L, 3L), list.map { it.id })
        assertEquals("UID1", list[0].item.uidOrCode)
        assertEquals(100L, list[0].createdAt)
        assertEquals(2, store.countPending())
    }

    @Test fun tidsstämpel_lika_ger_id_ordning() = runBlocking {
        val dao = FakeDao()
        dao.insert(entity(9, ts = 5)); dao.insert(entity(4, ts = 5))
        assertEquals(listOf(4L, 9L), ReadingOutboxStore(dao).nextPending(5).map { it.id })
    }

    @Test fun markSent_sätter_SENT_och_speglar_äldre_kolumner() = runBlocking {
        val dao = FakeDao(); dao.insert(entity(1))
        ReadingOutboxStore(dao).markSent(1, 777)
        val r = dao.rows.getValue(1)
        assertEquals("SENT", r.outboxStatus); assertTrue(r.transmitted); assertEquals("transmitted", r.status)
        assertEquals(777L, r.sentAt); assertEquals(1, r.attempts); assertNull(r.lastError)
        assertTrue(r.toDomain().transmitted)
    }

    @Test fun fel_ger_försök_och_felorsak_och_FAILED_kan_köas_om() = runBlocking {
        val dao = FakeDao(); dao.insert(entity(1))
        val store = ReadingOutboxStore(dao)
        store.markAttemptFailed(1, "timeout", 10)
        assertEquals("PENDING", dao.rows.getValue(1).outboxStatus)
        assertEquals(1, dao.rows.getValue(1).attempts)
        store.markFailed(1, "avvisad", 20)
        val failed = dao.rows.getValue(1)
        assertEquals("FAILED", failed.outboxStatus); assertEquals(2, failed.attempts); assertEquals("avvisad", failed.lastError)
        assertFalse(failed.toDomain().transmitted)
        assertEquals(0, store.countPending())
        store.requeue(1)
        val again = dao.rows.getValue(1)
        assertEquals("PENDING", again.outboxStatus); assertEquals(0, again.attempts); assertNull(again.lastAttemptAt)
    }

    @Test fun housekeeping_raderar_aldrig_väntande_poster() = runBlocking {
        val dao = FakeDao()
        dao.insert(entity(1, ts = 10)); dao.insert(entity(2, ts = 10, transmitted = true))
        dao.deleteOlderThan(100)
        assertEquals(listOf(1L), dao.rows.keys.toList())
    }

    @Test fun okänt_statusvärde_behandlas_som_PENDING() {
        assertEquals(OutboxStatus.PENDING, parseOutboxStatus("skräp"))
        assertEquals(OutboxStatus.FAILED, parseOutboxStatus("FAILED"))
    }

    @Test fun dispatcher_mot_room_adaptern_skickar_i_ordning_och_markerar_SENT_efter_ack() = runBlocking {
        val dao = FakeDao()
        dao.insert(entity(2, ts = 200)); dao.insert(entity(1, ts = 100)); dao.insert(entity(3, ts = 300))
        val seen = mutableListOf<Long>()
        val transport = object : OutboxTransport<PersistedReading> {
            override suspend fun send(entry: OutboxEntry<PersistedReading>): SendResult {
                seen.add(entry.id)
                // Posten får inte vara SENT innan ack
                assertEquals("PENDING", dao.rows.getValue(entry.id).outboxStatus)
                return if (entry.id == 3L) SendResult.Failed("nere") else SendResult.Acked
            }
        }
        val d = OutboxDispatcher(ReadingOutboxStore(dao), transport, NoBackoff, 5) { 5000 }
        val r = d.drain() as DrainResult.Blocked
        assertEquals(2, r.sent)
        assertEquals(listOf(1L, 2L, 3L), seen)
        assertEquals("SENT", dao.rows.getValue(1).outboxStatus)
        assertEquals("SENT", dao.rows.getValue(2).outboxStatus)
        assertEquals("PENDING", dao.rows.getValue(3).outboxStatus)
        assertEquals("nere", dao.rows.getValue(3).lastError)
        assertEquals(1, dao.rows.getValue(3).attempts)
    }
}
