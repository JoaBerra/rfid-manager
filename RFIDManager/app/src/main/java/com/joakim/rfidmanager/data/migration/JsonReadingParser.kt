package com.joakim.rfidmanager.data.migration

import com.joakim.rfidmanager.data.local.entities.PersistedReadingEntity
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Ren tolkning av den gamla filen filesDir/readings.json (JSON-reservlagringen).
 *
 * - Hela filen trasig (ingen giltig JSON-array) => [JSONException]; anroparen ska då
 *   lämna filen orörd och försöka igen vid nästa start.
 * - Enstaka poster som saknar obligatoriska fält (type, uidOrCode, timestamp) eller inte
 *   är objekt hoppas över och räknas i [ParseResult.skipped]. Filen bevaras ändå
 *   (döps om till .migrated) så inget går förlorat.
 * - Valfria fält som saknas eller är null blir null / standardvärde.
 * - Äldre statusvärdet "transmitted via Sparkplug" normaliseras till "transmitted".
 */
object JsonReadingParser {

    const val STATUS_PERSISTED = "persisted"
    const val STATUS_TRANSMITTED = "transmitted"
    private const val LEGACY_STATUS_TRANSMITTED = "transmitted via Sparkplug"

    data class ParseResult(
        val readings: List<PersistedReadingEntity>,
        val skipped: Int,
        val skipReasons: List<String>
    )

    /** Tom/blank text tolkas som en tom lista (inget att migrera). */
    @Throws(JSONException::class)
    fun parse(text: String): ParseResult {
        val trimmed = text.trim().removePrefix("\uFEFF").trim()
        if (trimmed.isEmpty()) return ParseResult(emptyList(), 0, emptyList())

        val arr = JSONArray(trimmed) // kastar JSONException om det inte är en giltig array
        val out = ArrayList<PersistedReadingEntity>(arr.length())
        val reasons = ArrayList<String>()
        var skipped = 0
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i)
            if (obj == null) {
                skipped++
                if (reasons.size < MAX_REASONS) reasons.add("post $i är inget objekt")
                continue
            }
            try {
                out.add(toEntity(obj))
            } catch (e: JSONException) {
                skipped++
                if (reasons.size < MAX_REASONS) reasons.add("post $i: ${e.message}")
            }
        }
        return ParseResult(out, skipped, reasons)
    }

    private const val MAX_REASONS = 20

    private fun toEntity(obj: JSONObject): PersistedReadingEntity {
        val transmitted = obj.optBoolean("transmitted", false)
        val rawStatus = optStr(obj, "status") ?: STATUS_PERSISTED
        val status = if (rawStatus == LEGACY_STATUS_TRANSMITTED) STATUS_TRANSMITTED else rawStatus
        return PersistedReadingEntity(
            id = if (obj.has("id") && !obj.isNull("id")) obj.getLong("id") else 0L,
            type = obj.getString("type"),
            uidOrCode = obj.getString("uidOrCode"),
            timestamp = obj.getLong("timestamp"),
            source = optStr(obj, "source"),
            dataPreview = optStr(obj, "dataPreview"),
            status = status,
            transmitted = transmitted,
            memoryBank = optInt(obj, "memoryBank"),
            address = optInt(obj, "address"),
            length = optInt(obj, "length"),
            payload = optStr(obj, "payload"),
            sparkplugJson = optStr(obj, "sparkplugJson"),
            correlationId = optStr(obj, "correlationId")
        )
    }

    private fun optStr(obj: JSONObject, key: String): String? =
        if (obj.has(key) && !obj.isNull(key)) obj.getString(key) else null

    private fun optInt(obj: JSONObject, key: String): Int? =
        if (obj.has(key) && !obj.isNull(key)) obj.getInt(key) else null
}
