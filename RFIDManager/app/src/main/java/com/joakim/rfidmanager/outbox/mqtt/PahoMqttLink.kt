package com.joakim.rfidmanager.outbox.mqtt

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

/**
 * [MqttLink] över Paho (synkron MqttClient). Egen anslutning per utskicksomgång – beror inte på UI:ts
 * MqttConnectionManager eller att appen är öppen. Ansluter lazy vid första publiceringen.
 *
 * Ack: publiceringen sker med QoS 1 och vi väntar på leveranstoken (`waitForCompletion`, med timeout),
 * som slutförs först när brokern skickat PUBACK. Uteblir den kastas ett fel (posten förblir väntande).
 *
 * Lösenordet hålls bara i minnet för anslutningen och loggas/inkluderas aldrig i felmeddelanden.
 *
 * @param clientId måste vara unikt per samtidig anslutning (brokern kopplar annars ner den äldre).
 */
class PahoMqttLink(
    private val brokerUrl: String,
    private val clientId: String,
    private val username: String,
    private val password: String,
    private val connectTimeoutSeconds: Int = 10,
    private val ackTimeoutMillis: Long = 15_000L
) : MqttLink {

    private var client: MqttClient? = null

    override suspend fun publishAndAwaitAck(topic: String, payload: ByteArray) {
        withContext(Dispatchers.IO) {
            try {
                val c = ensureConnected()
                val message = MqttMessage(payload).apply {
                    qos = 1
                    isRetained = false
                }
                val token = c.getTopic(topic).publish(message)
                token.waitForCompletion(ackTimeoutMillis) // kastar MqttException(32000) vid timeout
                if (!token.isComplete) throw MqttException(MqttException.REASON_CODE_CLIENT_TIMEOUT.toInt())
            } catch (e: MqttException) {
                resetConnection() // okänt tillstånd: nästa försök ansluter på nytt
                throw IllegalStateException(describe(e), e)
            } catch (e: Exception) {
                resetConnection()
                throw e
            }
        }
    }

    private fun ensureConnected(): MqttClient {
        client?.takeIf { it.isConnected }?.let { return it }
        resetConnection()
        val c = MqttClient(brokerUrl, clientId, MemoryPersistence())
        val options = MqttConnectOptions().apply {
            keepAliveInterval = 30
            connectionTimeout = connectTimeoutSeconds
            isCleanSession = true
            isAutomaticReconnect = false
            if (username.isNotEmpty()) {
                userName = username
                password = this@PahoMqttLink.password.toCharArray()
            }
        }
        c.timeToWait = ackTimeoutMillis
        c.connect(options)
        client = c
        return c
    }

    private fun resetConnection() {
        val c = client ?: return
        client = null
        try { if (c.isConnected) c.disconnect(2_000) } catch (_: Exception) {}
        try { c.close() } catch (_: Exception) {}
    }

    override fun close() = resetConnection()

    private fun describe(e: MqttException): String {
        val reason = when (e.reasonCode) {
            4 -> "Fel användarnamn eller lösenord"
            5 -> "Ej behörig"
            32000 -> "Brokern bekräftade inte i tid"
            32103 -> "Kan inte nå brokern"
            32109 -> "Anslutningen bröts"
            32110 -> "Anslutning pågår redan"
            else -> null
        }
        val detail = e.cause?.message ?: e.message ?: e.javaClass.simpleName
        return if (reason != null) "$reason ($detail)" else detail
    }
}
