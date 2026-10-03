package com.joakim.rfidmanager.outbox.mqtt

import com.joakim.rfidmanager.outbox.core.OutboxEntry
import com.joakim.rfidmanager.outbox.core.OutboxTransport
import com.joakim.rfidmanager.outbox.core.SendResult
import kotlin.coroutines.cancellation.CancellationException

/** Gör om en utkorgspost till MQTT-topic och nyttolast. Generisk: ett per domän (avläsning, FASAD-händelse …). */
interface MqttMessageEncoder<in T> {
    fun topic(entry: OutboxEntry<T>): String
    fun payload(entry: OutboxEntry<T>): ByteArray
}

/**
 * Smal MQTT-länk som transporten använder (gör att transporten kan testas utan Paho/broker).
 * [publishAndAwaitAck] ska bara returnera när brokern bekräftat leveransen (QoS 1: PUBACK),
 * annars kasta undantag med begriplig text.
 */
interface MqttLink {
    suspend fun publishAndAwaitAck(topic: String, payload: ByteArray)

    /** Kopplar ner och frigör resurser. Får anropas flera gånger. */
    fun close()
}

/**
 * [OutboxTransport] över MQTT. Ack = att [MqttLink.publishAndAwaitAck] återvänder (brokerns PUBACK).
 * Alla fel (ingen anslutning, fel inloggning, timeout …) blir [SendResult.Failed] med felorsaken.
 */
class MqttOutboxTransport<T>(
    private val link: MqttLink,
    private val encoder: MqttMessageEncoder<T>
) : OutboxTransport<T> {

    override suspend fun send(entry: OutboxEntry<T>): SendResult = try {
        link.publishAndAwaitAck(encoder.topic(entry), encoder.payload(entry))
        SendResult.Acked
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        SendResult.Failed(e.message?.takeIf { it.isNotBlank() } ?: e.javaClass.simpleName)
    }

    fun close() = link.close()
}
