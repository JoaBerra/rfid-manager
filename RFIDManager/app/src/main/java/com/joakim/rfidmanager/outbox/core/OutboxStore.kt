package com.joakim.rfidmanager.outbox.core

/**
 * Lagring av utkorgsposter. Implementeras mot t.ex. Room (se ReadingOutboxStore) eller minnet
 * ([InMemoryOutboxStore]). Alla metoder är suspend så att implementationen får blockera på I/O.
 *
 * Krav på implementationen:
 * - [nextPending] returnerar endast PENDING-poster, äldst först (createdAt, sedan id).
 * - Alla mark*-metoder är atomära per post och ökar `attempts` med 1 (utom [markSent]
 *   som också gör det, så att antalet försök visar hur många som krävdes).
 */
interface OutboxStore<T> {
    /** Upp till [limit] väntande poster, äldst först. */
    suspend fun nextPending(limit: Int): List<OutboxEntry<T>>

    /** Bekräftad leverans: status SENT, attempts+1, lastError rensas. */
    suspend fun markSent(id: Long, at: Long)

    /** Misslyckat försök men posten ska försökas igen: förblir PENDING, attempts+1, lastError/lastAttemptAt sätts. */
    suspend fun markAttemptFailed(id: Long, error: String, at: Long)

    /** Gav upp: status FAILED, attempts+1, lastError/lastAttemptAt sätts. */
    suspend fun markFailed(id: Long, error: String, at: Long)

    /** Sätter en FAILED-post tillbaka till PENDING med attempts=0 (ingen backoff). Ignorerar andra statusar. */
    suspend fun requeue(id: Long)

    /** Som [requeue] för alla FAILED-poster. Returnerar antal. */
    suspend fun requeueFailed(): Int

    /** Antal PENDING-poster. */
    suspend fun countPending(): Int
}
