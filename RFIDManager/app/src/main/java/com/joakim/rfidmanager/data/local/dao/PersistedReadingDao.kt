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

    @Query("SELECT * FROM persisted_readings ORDER BY timestamp DESC")
    fun getAll(): Flow<List<PersistedReadingEntity>>

    @Query("SELECT * FROM persisted_readings WHERE type = :type ORDER BY timestamp DESC")
    fun getByType(type: String): Flow<List<PersistedReadingEntity>>

    @Query("SELECT * FROM persisted_readings WHERE transmitted = 0 ORDER BY timestamp DESC")
    fun getPendingTransmission(): Flow<List<PersistedReadingEntity>>

    @Query("UPDATE persisted_readings SET transmitted = 1, status = 'transmitted' WHERE id = :id")
    suspend fun markAsTransmitted(id: Long)

    @Query("DELETE FROM persisted_readings WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Query("DELETE FROM persisted_readings")
    suspend fun deleteAll()
}