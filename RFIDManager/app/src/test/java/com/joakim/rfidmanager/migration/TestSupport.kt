package com.joakim.rfidmanager.migration

import com.joakim.rfidmanager.data.local.entities.PersistedReadingEntity
import com.joakim.rfidmanager.data.migration.MigrationLog
import com.joakim.rfidmanager.data.migration.ReadingMigrationStore

/** In-memory-ersättare för Room med transaktionssemantik (rollback vid undantag). */
class FakeStore(initial: List<PersistedReadingEntity> = emptyList()) : ReadingMigrationStore {
    var rows: MutableMap<Long, PersistedReadingEntity> = initial.associateBy { it.id }.toMutableMap()
    var transactions = 0
    var committed = 0
    var failOnInsert: Exception? = null
    /** Simulerar att inserten "lyckas" men tappar poster, för att testa verifieringen. */
    var dropOnInsert = false

    override suspend fun <T> inTransaction(block: suspend () -> T): T {
        transactions++
        val snapshot = rows.toMutableMap()
        try {
            val r = block()
            committed++
            return r
        } catch (e: Throwable) {
            rows = snapshot
            throw e
        }
    }

    override suspend fun existing(): List<PersistedReadingEntity> = rows.values.toList()

    override suspend fun insertAllStrict(readings: List<PersistedReadingEntity>) {
        failOnInsert?.let { throw it }
        val list = if (dropOnInsert) readings.drop(1) else readings
        for (r in list) {
            check(r.id !in rows) { "UNIQUE constraint failed: id=${r.id}" }
            rows[r.id] = r
        }
    }
}

class RecordingLog : MigrationLog {
    val infos = mutableListOf<String>()
    val warns = mutableListOf<String>()
    val errors = mutableListOf<String>()
    override fun info(msg: String) { infos.add(msg) }
    override fun warn(msg: String, t: Throwable?) { warns.add(msg) }
    override fun error(msg: String, t: Throwable?) { errors.add(msg) }
}

fun entity(
    id: Long,
    uid: String = "UID$id",
    ts: Long = 1_700_000_000_000L + id,
    transmitted: Boolean = false,
    payload: String? = null
) = PersistedReadingEntity(
    id = id, type = "RFID", uidOrCode = uid, timestamp = ts,
    status = if (transmitted) "transmitted" else "persisted",
    transmitted = transmitted, payload = payload
)
