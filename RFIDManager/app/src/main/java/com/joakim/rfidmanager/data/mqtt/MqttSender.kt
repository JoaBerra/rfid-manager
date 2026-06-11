package com.joakim.rfidmanager.data.mqtt

import android.util.Log
import com.joakim.rfidmanager.domain.model.PersistedReading
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.json.JSONObject

/**
 * Enkel MQTT-sändare för Sparkplug B-liknande payload.
 * Använder en delad MqttConnectionManager för att återanvända den beständiga anslutningen.
 */
object MqttSender {

    private const val TAG = "MqttSender"

    private var connectionManager: MqttConnectionManager? = null

    fun init(manager: MqttConnectionManager) {
        connectionManager = manager
        Log.i(TAG, "MqttSender initialized with shared MqttConnectionManager")
    }

    suspend fun sendReading(reading: PersistedReading) {
        val manager = connectionManager ?: run {
            Log.w(TAG, "Cannot send — MqttConnectionManager not initialized")
            return
        }
        val client = manager.client
        if (client == null || !client.isConnected) {
            Log.w(TAG, "Cannot send — MQTT not connected")
            return
        }
        Log.i(TAG, "Publishing via shared connection for uid=${reading.uidOrCode}")

        val payload = JSONObject().apply {
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

        val message = MqttMessage(payload.toString().toByteArray())
        message.qos = 1

        val topic = "rfidmanager/${reading.uidOrCode}/telemetry"

        try {
            client.publish(topic, message)
            manager.onPublished()
            Log.i(TAG, "Published to $topic : ${payload.toString(2)}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to publish reading", e)
        }

        Log.i(TAG, "=== SEND COMPLETE for uid=${reading.uidOrCode} type=${if (reading.isRfid()) "ReadEscortMemory" else "ReadBarcode"} ===")
    }
}