package com.joakim.rfidmanager.data.repository

import android.content.Context
import com.joakim.rfidmanager.data.local.dao.PersistedReadingDao
import com.joakim.rfidmanager.data.local.entities.PersistedReadingEntity
import com.joakim.rfidmanager.domain.model.PersistedReading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Repository for persisted readings.
 * Provides domain models to the UI layer.
 *
 * Triple mode:
 * - If dao != null: real Room + SQLite (requires room-compiler processor at build time).
 * - If dao == null && context != null: JSON file-backed storage (survives app restart). No annotation processor needed.
 * - If dao == null && context == null: pure in-memory (MutableStateFlow). Data lost on process death.
 *
 * The JSON file fallback makes Fas 3.4 (persistence survives restart) work without KSP.
 */
class PersistedReadingRepository(
    private val dao: PersistedReadingDao? = null,
    private val appContext: Context? = null
) {
    val isUsingRealDatabase: Boolean get() = dao != null

    /** True when JSON file storage is active (data survives restart without Room). */
    val isUsingJsonFallback: Boolean get() = dao == null && appContext != null

    private val jsonFile: File? by lazy {
        appContext?.let { File(it.filesDir, "readings.json") }
    }

    // --- In-memory fallback state (only used when dao == null) ---
    private val _allReadings = MutableStateFlow<List<PersistedReading>>(emptyList())
    private var nextId: Long = 1L
    private var loaded = false

    private suspend fun ensureLoaded() {
        if (dao != null || loaded) return
        loaded = true
        withContext(Dispatchers.IO) {
            try {
                val file = jsonFile ?: return@withContext
                if (!file.exists()) return@withContext
                val text = file.readText()
                val arr = JSONArray(text)
                val list = mutableListOf<PersistedReading>()
                var maxId = 0L
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val r = jsonToReading(obj)
                    list.add(r)
                    if (r.id > maxId) maxId = r.id
                }
                _allReadings.value = list
                nextId = maxId + 1
                android.util.Log.i("PersistedReadingRepo", "Loaded ${list.size} readings from JSON file")
            } catch (e: Exception) {
                android.util.Log.e("PersistedReadingRepo", "Failed to load JSON file", e)
            }
        }
    }

    private suspend fun persistToFile() {
        if (dao != null) return
        withContext(Dispatchers.IO) {
            try {
                val file = jsonFile ?: return@withContext
                val arr = JSONArray()
                for (r in _allReadings.value) {
                    arr.put(readingToJson(r))
                }
                file.writeText(arr.toString(2))
            } catch (e: Exception) {
                android.util.Log.e("PersistedReadingRepo", "Failed to persist JSON file", e)
            }
        }
    }

    private fun readingToJson(r: PersistedReading): JSONObject = JSONObject().apply {
        put("id", r.id)
        put("type", r.type)
        put("uidOrCode", r.uidOrCode)
        put("timestamp", r.timestamp)
        put("status", r.status)
        put("transmitted", r.transmitted)
        r.source?.let { put("source", it) }
        r.dataPreview?.let { put("dataPreview", it) }
        r.memoryBank?.let { put("memoryBank", it) }
        r.address?.let { put("address", it) }
        r.length?.let { put("length", it) }
        r.payload?.let { put("payload", it) }
        r.sparkplugJson?.let { put("sparkplugJson", it) }
        r.correlationId?.let { put("correlationId", it) }
    }

    private fun jsonToReading(obj: JSONObject): PersistedReading = PersistedReading(
        id = obj.getLong("id"),
        type = obj.getString("type"),
        uidOrCode = obj.getString("uidOrCode"),
        timestamp = obj.getLong("timestamp"),
        source = if (obj.has("source") && !obj.isNull("source")) obj.getString("source") else null,
        dataPreview = if (obj.has("dataPreview") && !obj.isNull("dataPreview")) obj.getString("dataPreview") else null,
        status = obj.optString("status", "persisted"),
        transmitted = obj.optBoolean("transmitted", false),
        memoryBank = if (obj.has("memoryBank") && !obj.isNull("memoryBank")) obj.getInt("memoryBank") else null,
        address = if (obj.has("address") && !obj.isNull("address")) obj.getInt("address") else null,
        length = if (obj.has("length") && !obj.isNull("length")) obj.getInt("length") else null,
        payload = if (obj.has("payload") && !obj.isNull("payload")) obj.getString("payload") else null,
        sparkplugJson = if (obj.has("sparkplugJson") && !obj.isNull("sparkplugJson")) obj.getString("sparkplugJson") else null,
        correlationId = if (obj.has("correlationId") && !obj.isNull("correlationId")) obj.getString("correlationId") else null
    )

    suspend fun load() {
        ensureLoaded()
    }

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
        ensureLoaded()
        if (dao != null) {
            dao.insert(reading.toEntity())
        } else {
            val withId = if (reading.id == 0L) reading.copy(id = nextId++) else reading
            _allReadings.value = _allReadings.value + withId
        }
        persistToFile()
    }

    suspend fun markAsTransmitted(id: Long) {
        ensureLoaded()
        if (dao != null) {
            dao.markAsTransmitted(id)
        } else {
            _allReadings.value = _allReadings.value.map { r ->
                if (r.id == id) r.copy(transmitted = true, status = "transmitted via Sparkplug") else r
            }
        }
        persistToFile()
    }

    suspend fun housekeeping(cutoffTimestamp: Long) {
        ensureLoaded()
        if (dao != null) {
            dao.deleteOlderThan(cutoffTimestamp)
        } else {
            _allReadings.value = _allReadings.value.filter { it.timestamp >= cutoffTimestamp }
        }
        persistToFile()
    }

    suspend fun clearAll() {
        if (dao != null) {
            dao.deleteAll()
        } else {
            _allReadings.value = emptyList()
        }
        persistToFile()
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