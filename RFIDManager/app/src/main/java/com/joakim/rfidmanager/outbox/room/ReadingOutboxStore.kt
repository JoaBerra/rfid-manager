package com.joakim.rfidmanager.outbox.room

import com.joakim.rfidmanager.data.local.dao.PersistedReadingDao
import com.joakim.rfidmanager.data.local.entities.PersistedReadingEntity
import com.joakim.rfidmanager.data.repository.toDomain
import com.joakim.rfidmanager.domain.model.PersistedReading
import com.joakim.rfidmanager.outbox.core.OutboxEntry
import com.joakim.rfidmanager.outbox.core.OutboxStore

/**
 * Adapter: gör Room-tabellen persisted_readings till en [OutboxStore] för [PersistedReading].
 * All sändstatus ligger i tabellens utkorgskolumner (schema v2); DAO:ns frågor ser till att den
 * äldre kolumnen `transmitted` hålls i synk.
 */
class ReadingOutboxStore(private val dao: PersistedReadingDao) : OutboxStore<PersistedReading> {

    override suspend fun nextPending(limit: Int): List<OutboxEntry<PersistedReading>> =
        dao.nextPending(limit).map { it.toOutboxEntry() }

    override suspend fun markSent(id: Long, at: Long) = dao.markSent(id, at)

    override suspend fun markAttemptFailed(id: Long, error: String, at: Long) =
        dao.markAttemptFailed(id, error, at)

    override suspend fun markFailed(id: Long, error: String, at: Long) = dao.markFailed(id, error, at)

    override suspend fun requeue(id: Long) {
        dao.requeue(id)
    }

    override suspend fun requeueFailed(): Int = dao.requeueFailed()

    override suspend fun countPending(): Int = dao.countPending()

    private fun PersistedReadingEntity.toOutboxEntry() = OutboxEntry(
        id = id,
        item = toDomain(),
        createdAt = timestamp,
        attempts = attempts,
        lastAttemptAt = lastAttemptAt,
        lastError = lastError
    )
}
