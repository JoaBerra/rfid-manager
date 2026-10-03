package com.joakim.rfidmanager.data.repository

import android.util.Log
import com.joakim.rfidmanager.data.local.dao.PersistedReadingDao
import com.joakim.rfidmanager.data.local.entities.PersistedReadingEntity
import com.joakim.rfidmanager.data.migration.JsonToRoomMigrator
import com.joakim.rfidmanager.data.migration.MigrationResult
import com.joakim.rfidmanager.domain.model.PersistedReading
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException

/**
 * Repository för sparade avläsningar. Room/SQLite är enda lagringen.
 *
 * Den gamla filen readings.json läses in av [JsonToRoomMigrator] (en gång per appstart,
 * tills den lyckats) innan någon läs-/skrivoperation körs. Om migreringen misslyckas ligger
 * JSON-filen kvar orörd och [migrationResult] visar felet; appen fortsätter spara nya
 * avläsningar i Room men visar INTE JSON-innehållet (annars skulle två datakällor blandas
 * och id:n kunna krocka). Det är säkrast: inget skrivs över och inget raderas, och den gamla
 * datan kommer med vid nästa lyckade migrering.
 */
class PersistedReadingRepository(
    private val dao: PersistedReadingDao,
    private val migrator: JsonToRoomMigrator? = null
) {
    companion object {
        private const val TAG = "PersistedReadingRepo"
        const val STATUS_PERSISTED = "persisted"
        const val STATUS_TRANSMITTED = "transmitted"
    }

    val isUsingRealDatabase: Boolean get() = true

    private val migrationMutex = Mutex()
    private var migrationAttempted = false

    private val _migrationResult = MutableStateFlow<MigrationResult?>(null)
    /** null = migreringen har inte körts än i denna process. */
    val migrationResult: StateFlow<MigrationResult?> = _migrationResult.asStateFlow()

    private val _storageError = MutableStateFlow<String?>(null)
    /** Senaste fel mot databasen (läsning/skrivning), eller null. */
    val storageError: StateFlow<String?> = _storageError.asStateFlow()

    /** Kör JSON->Room-migreringen (högst en gång per process). Alla operationer anropar den först. */
    private suspend fun ensureMigrated() {
        if (migrationAttempted) return
        migrationMutex.withLock {
            if (migrationAttempted) return
            val m = migrator
            if (m != null) {
                _migrationResult.value = m.migrate() // kastar bara vid avbrott
            }
            migrationAttempted = true
        }
    }

    suspend fun load() {
        ensureMigrated()
    }

    private fun <T> Flow<T>.logErrors(fallback: T): Flow<T> = catch { e ->
        Log.e(TAG, "Kunde inte läsa från Room", e)
        _storageError.value = e.message ?: e.javaClass.simpleName
        emit(fallback)
    }

    fun getAllReadings(): Flow<List<PersistedReading>> =
        dao.getAll().map { entities -> entities.map { it.toDomain() } }.logErrors(emptyList())

    fun getReadingsByType(type: String): Flow<List<PersistedReading>> =
        dao.getByType(type).map { entities -> entities.map { it.toDomain() } }.logErrors(emptyList())

    fun getPendingForTransmission(): Flow<List<PersistedReading>> =
        dao.getPendingTransmission().map { entities -> entities.map { it.toDomain() } }.logErrors(emptyList())

    /** Returnerar true om avläsningen sparades. Fel loggas och syns i [storageError]. */
    suspend fun saveReading(reading: PersistedReading): Boolean =
        guarded("spara avläsning") { dao.insert(reading.toEntity()) }

    /**
     * Markerar som skickad. OBS: anropas idag oavsett om MQTT-publiceringen lyckades
     * (känt fel, se backlog) – beteendet är medvetet oförändrat här.
     */
    suspend fun markAsTransmitted(id: Long) {
        guarded("markera som skickad") { dao.markAsTransmitted(id) }
    }

    suspend fun housekeeping(cutoffTimestamp: Long) {
        guarded("rensa gamla avläsningar") { dao.deleteOlderThan(cutoffTimestamp) }
    }

    suspend fun clearAll() {
        guarded("rensa alla avläsningar") { dao.deleteAll() }
    }

    private suspend fun guarded(what: String, block: suspend () -> Unit): Boolean {
        return try {
            ensureMigrated()
            block()
            _storageError.value = null
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Room-fel vid $what", e)
            _storageError.value = e.message ?: e.javaClass.simpleName
            false
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