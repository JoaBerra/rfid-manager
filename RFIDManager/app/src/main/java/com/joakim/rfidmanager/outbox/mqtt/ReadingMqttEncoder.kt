package com.joakim.rfidmanager.outbox.mqtt

import com.joakim.rfidmanager.domain.model.PersistedReading
import com.joakim.rfidmanager.outbox.core.OutboxEntry
import org.json.JSONObject

/**
 * Kodar en avläsning som MQTT-meddelande (Sparkplug B-liknande JSON, som tidigare) och tillför fält
 * för dubblettskydd i dashboarden:
 *
 * - `id`        postens lokala id (Long)
 * - `deviceId`  installationens id (se AppSettings.deviceId)
 * - `messageId` "<deviceId>:<id>" – stabil, globalt unik nyckel; samma post får alltid samma messageId
 *
 * Äldre mottagare ignorerar okända fält, och äldre meddelanden utan dessa fält hanteras av dashboarden.
 */
class ReadingMqttEncoder(private val deviceId: String) : MqttMessageEncoder<PersistedReading> {

    override fun topic(entry: OutboxEntry<PersistedReading>): String =
        "rfidmanager/${safeTopicLevel(entry.item.uidOrCode)}/telemetry"

    override fun payload(entry: OutboxEntry<PersistedReading>): ByteArray =
        payloadJson(entry.item).toString().toByteArray(Charsets.UTF_8)

    fun payloadJson(reading: PersistedReading): JSONObject = JSONObject().apply {
        put("id", reading.id)
        put("deviceId", deviceId)
        put("messageId", messageId(reading.id))
        put("type", if (reading.isRfid()) "ReadEscortMemory" else "ReadBarcode")
        put("uid", reading.uidOrCode)
        put("timestamp", reading.timestamp)
        put("source", reading.source)
        put("sparkplug", true)

        val data = JSONObject()
        reading.memoryBank?.let { data.put("memoryBank", it) }
        reading.address?.let { data.put("address", it) }
        reading.length?.let { data.put("length", it) }
        reading.payload?.let { data.put("payload", it) }
        put("data", data)
    }

    fun messageId(id: Long): String = "$deviceId:$id"

    /** '/', '+' och '#' har betydelse i MQTT-topics och byts mot '_' i uid-nivån. */
    private fun safeTopicLevel(uid: String): String =
        uid.map { if (it == '/' || it == '+' || it == '#') '_' else it }.joinToString("").ifEmpty { "unknown" }
}
