package com.joakim.rfidmanager.data.repository

import com.joakim.rfidmanager.data.local.dao.PersistedReadingDao
import com.joakim.rfidmanager.data.local.entities.PersistedReadingEntity
import com.joakim.rfidmanager.domain.model.PersistedReading
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Repository for persisted readings.
 * Provides domain models to the UI layer.
 *
 * Dual mode (Fas 2 demo):
 * - If dao != null: real Room + SQLite (requires room-compiler processor at build time).
 * - If dao == null: pure in-memory (MutableStateFlow backed). No annotation processor needed.
 *   This lets the app launch and the full PERSISTED tab + Transmit + Sparkplug flow be tested
 *   while the KSP/Gradle plugin resolution for room-compiler is being fixed.
 *   Data lives only for the current process (lost on force-stop/restart) — perfect for quick UI/MQTT validation.
 *   When processor is re-enabled, pass a real dao and you get persistent storage + cross-restart.
 */
class PersistedReadingRepository(
    private val dao: PersistedReadingDao? = null
) {
    // --- In-memory fallback state (only used when dao == null) ---
    private val _allReadings = MutableStateFlow<List<PersistedReading>>(emptyList())
    private var nextId: Long = 1L

    fun getAllReadings(): Flow<List<PersistedReading>> =
        if (dao != null) {
            dao.getAll().map { entities -> entities.map { it.toDomain() } }
        } else {
            _allReadings
        }

    fun getReadingsByType(type: String): Flow<List<PersistedReading>> =
        if (dao != null) {
            dao.getByType(type).map { entities -> entities.map { it.toDomain() } }
        } else {
            _allReadings.map { list -> list.filter { it.type.equals(type, ignoreCase = true) } }
        }

    fun getPendingForTransmission(): Flow<List<PersistedReading>> =
        if (dao != null) {
            dao.getPendingTransmission().map { entities -> entities.map { it.toDomain() } }
        } else {
            _allReadings.map { list -> list.filter { !it.transmitted } }
        }

    suspend fun saveReading(reading: PersistedReading) {
        if (dao != null) {
            dao.insert(reading.toEntity())
        } else {
            val withId = if (reading.id == 0L) reading.copy(id = nextId++) else reading
            _allReadings.value = _allReadings.value + withId
        }
    }

    suspend fun markAsTransmitted(id: Long) {
        if (dao != null) {
            dao.markAsTransmitted(id)
        } else {
            _allReadings.value = _allReadings.value.map { r ->
                if (r.id == id) r.copy(transmitted = true, status = "transmitted via Sparkplug") else r
            }
        }
    }

    suspend fun housekeeping(cutoffTimestamp: Long) {
        if (dao != null) {
            dao.deleteOlderThan(cutoffTimestamp)
        } else {
            _allReadings.value = _allReadings.value.filter { it.timestamp >= cutoffTimestamp }
        }
    }

    suspend fun clearAll() {
        if (dao != null) {
            dao.deleteAll()
        } else {
            _allReadings.value = emptyList()
        }
    }

    // --- Mappers ---
    private fun PersistedReadingEntity.toDomain() = PersistedReading(
        id = id,
        type = type,
        uidOrCode = uidOrCode,
        timestamp = timestamp,
        source = source,
        dataPreview = dataPreview,
        status = status,
        transmitted = transmitted,
        memoryBank = memoryBank,
        address = address,
        length = length,
        payload = payload,
        sparkplugJson = sparkplugJson,
        correlationId = correlationId
    )

    private fun PersistedReading.toEntity() = PersistedReadingEntity(
        id = id,
        type = type,
        uidOrCode = uidOrCode,
        timestamp = timestamp,
        source = source,
        dataPreview = dataPreview,
        status = status,
        transmitted = transmitted,
        memoryBank = memoryBank,
        address = address,
        length = length,
        payload = payload,
        sparkplugJson = sparkplugJson,
        correlationId = correlationId
    )
}