package com.joakim.rfidmanager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.joakim.rfidmanager.data.local.entities.PersistedReadingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersistedReadingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reading: PersistedReadingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(readings: List<PersistedReadingEntity>)

    /**
     * Strikt insert för JSON-migreringen: kastar vid id-krock istället för att tyst
     * skriva över (migreringen har redan löst krockar innan den anropar denna).
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAllStrict(readings: List<PersistedReadingEntity>)

    /** Engångsläsning (används av migreringen för att upptäcka id-krockar). */
    @Query("SELECT * FROM persisted_readings")
    suspend fun getAllOnce(): List<PersistedReadingEntity>

    @Query("SELECT * FROM persisted_readings ORDER BY timestamp DESC")
    fun getAll(): Flow<List<PersistedReadingEntity>>

    @Query("SELECT * FROM persisted_readings WHERE type = :type ORDER BY timestamp DESC")
    fun getByType(type: String): Flow<List<PersistedReadingEntity>>

    // --- Utkorg (schema v2) ---
    // outboxStatus är sanningen om sändningsstatus. Kolumnerna transmitted/status är äldre
    // speglingar och skrivs bara här, så att de aldrig kan hamna i otakt.

    /** Väntande poster, äldst först (utkorgens sändordning). */
    @Query("SELECT * FROM persisted_readings WHERE outboxStatus = 'PENDING' ORDER BY timestamp ASC, id ASC LIMIT :limit")
    suspend fun nextPending(limit: Int): List<PersistedReadingEntity>

    @Query("SELECT COUNT(*) FROM persisted_readings WHERE outboxStatus = 'PENDING'")
    suspend fun countPending(): Int

    @Query("SELECT COUNT(*) FROM persisted_readings WHERE outboxStatus = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM persisted_readings WHERE outboxStatus = 'FAILED'")
    fun observeFailedCount(): Flow<Int>

    @Query("""UPDATE persisted_readings SET outboxStatus = 'SENT', transmitted = 1, status = 'transmitted',
        attempts = attempts + 1, lastError = NULL, lastAttemptAt = :at, sentAt = :at WHERE id = :id""")
    suspend fun markSent(id: Long, at: Long)

    /** Misslyckat försök, posten förblir PENDING. */
    @Query("""UPDATE persisted_readings SET attempts = attempts + 1, lastError = :error, lastAttemptAt = :at
        WHERE id = :id AND outboxStatus = 'PENDING'""")
    suspend fun markAttemptFailed(id: Long, error: String, at: Long)

    @Query("""UPDATE persisted_readings SET outboxStatus = 'FAILED', transmitted = 0, status = 'persisted',
        attempts = attempts + 1, lastError = :error, lastAttemptAt = :at WHERE id = :id""")
    suspend fun markFailed(id: Long, error: String, at: Long)

    /** FAILED -> PENDING, försök nollställs och backoff-tidpunkten rensas. */
    @Query("""UPDATE persisted_readings SET outboxStatus = 'PENDING', attempts = 0, lastAttemptAt = NULL
        WHERE id = :id AND outboxStatus = 'FAILED'""")
    suspend fun requeue(id: Long): Int

    @Query("""UPDATE persisted_readings SET outboxStatus = 'PENDING', attempts = 0, lastAttemptAt = NULL
        WHERE outboxStatus = 'FAILED'""")
    suspend fun requeueFailed(): Int

    /** Gamla poster rensas, men aldrig väntande (PENDING) – de är ännu inte levererade. */
    @Query("DELETE FROM persisted_readings WHERE timestamp < :cutoff AND outboxStatus != 'PENDING'")
    suspend fun deleteOlderThan(cutoff: Long)

    @Query("DELETE FROM persisted_readings")
    suspend fun deleteAll()
}