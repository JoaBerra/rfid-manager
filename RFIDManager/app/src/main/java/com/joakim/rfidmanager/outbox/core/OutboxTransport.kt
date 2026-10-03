package com.joakim.rfidmanager.outbox.core

/**
 * Skickar en post till mottagaren och väntar på bekräftelse.
 *
 * - Ska returnera [SendResult.Acked] först när mottagaren faktiskt har bekräftat (ack).
 * - Får gärna returnera [SendResult.Failed]; kastade undantag (utom CancellationException)
 *   tolkas av [OutboxDispatcher] som [SendResult.Failed].
 * - Måste vara idempotent mot mottagaren: samma post (samma [OutboxEntry.id]) kan skickas
 *   flera gånger (at-least-once). Mottagaren avdupliceras via id:t.
 */
interface OutboxTransport<in T> {
    suspend fun send(entry: OutboxEntry<T>): SendResult
}
