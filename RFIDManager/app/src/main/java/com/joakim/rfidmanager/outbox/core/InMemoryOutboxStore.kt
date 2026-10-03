package com.joakim.rfidmanager.outbox.core

/**
 * Enkel [OutboxStore] i minnet. Avsedd för enhetstester och som referens när man skriver en
 * riktig lagring (t.ex. Room/SQLite). Inte trådsäker mellan parallella anropare.
 */
class InMemoryOutboxStore<T> : OutboxStore<T> {
    private data class Row<T>(
        val id: Long,
        val item: T,
        val createdAt: Long,
        var status: OutboxStatus = OutboxStatus.PENDING,
        var attempts: Int = 0,
        var lastAttemptAt: Long? = null,
        var lastError: String? = null,
        var sentAt: Long? = null
    )

    private val rows = LinkedHashMap<Long, Row<T>>()

    /** Lägger till en ny PENDING-post. Samma id två gånger ger IllegalArgumentException. */
    fun add(id: Long, item: T, createdAt: Long) {
        require(id !in rows) { "id $id finns redan" }
        rows[id] = Row(id, item, createdAt)
    }

    fun statusOf(id: Long): OutboxStatus = row(id).status
    fun attemptsOf(id: Long): Int = row(id).attempts
    fun lastErrorOf(id: Long): String? = row(id).lastError
    fun sentAtOf(id: Long): Long? = row(id).sentAt

    private fun row(id: Long) = rows[id] ?: error("okänt id $id")

    override suspend fun nextPending(limit: Int): List<OutboxEntry<T>> =
        rows.values
            .filter { it.status == OutboxStatus.PENDING }
            .sortedWith(compareBy({ it.createdAt }, { it.id }))
            .take(limit)
            .map { OutboxEntry(it.id, it.item, it.createdAt, it.attempts, it.lastAttemptAt, it.lastError) }

    override suspend fun markSent(id: Long, at: Long) {
        row(id).apply {
            status = OutboxStatus.SENT; attempts++; lastAttemptAt = at; sentAt = at; lastError = null
        }
    }

    override suspend fun markAttemptFailed(id: Long, error: String, at: Long) {
        row(id).apply { attempts++; lastAttemptAt = at; lastError = error }
    }

    override suspend fun markFailed(id: Long, error: String, at: Long) {
        row(id).apply { status = OutboxStatus.FAILED; attempts++; lastAttemptAt = at; lastError = error }
    }

    override suspend fun requeue(id: Long) {
        rows[id]?.takeIf { it.status == OutboxStatus.FAILED }?.apply {
            status = OutboxStatus.PENDING; attempts = 0; lastAttemptAt = null
        }
    }

    override suspend fun requeueFailed(): Int {
        val failed = rows.values.filter { it.status == OutboxStatus.FAILED }
        failed.forEach { requeue(it.id) }
        return failed.size
    }

    override suspend fun countPending(): Int = rows.values.count { it.status == OutboxStatus.PENDING }
}
