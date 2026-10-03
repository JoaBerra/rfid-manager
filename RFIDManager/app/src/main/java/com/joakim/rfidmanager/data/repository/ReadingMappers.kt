package com.joakim.rfidmanager.data.repository

import com.joakim.rfidmanager.data.local.entities.PersistedReadingEntity
import com.joakim.rfidmanager.domain.model.PersistedReading
import com.joakim.rfidmanager.outbox.core.OutboxStatus

// Mappning mellan Room-entitet och domänmodell. outboxStatus är sanningen; kolumnerna
// transmitted/status speglas från den (se PersistedReadingEntity).

internal fun PersistedReadingEntity.toDomain() = PersistedReading(
    id = id,
    type = type,
    uidOrCode = uidOrCode,
    timestamp = timestamp,
    source = source,
    dataPreview = dataPreview,
    status = status,
    memoryBank = memoryBank,
    address = address,
    length = length,
    payload = payload,
    sparkplugJson = sparkplugJson,
    correlationId = correlationId,
    outboxStatus = parseOutboxStatus(outboxStatus),
    attempts = attempts,
    lastError = lastError,
    lastAttemptAt = lastAttemptAt,
    sentAt = sentAt
)

internal fun PersistedReading.toEntity() = PersistedReadingEntity(
    id = id,
    type = type,
    uidOrCode = uidOrCode,
    timestamp = timestamp,
    source = source,
    dataPreview = dataPreview,
    status = if (transmitted) "transmitted" else status,
    transmitted = transmitted,
    memoryBank = memoryBank,
    address = address,
    length = length,
    payload = payload,
    sparkplugJson = sparkplugJson,
    correlationId = correlationId,
    outboxStatus = outboxStatus.name,
    attempts = attempts,
    lastError = lastError,
    lastAttemptAt = lastAttemptAt,
    sentAt = sentAt
)

/** Okänt/trasigt värde i databasen behandlas som PENDING (hellre skicka igen än tappa en post). */
internal fun parseOutboxStatus(raw: String): OutboxStatus =
    OutboxStatus.entries.firstOrNull { it.name == raw } ?: OutboxStatus.PENDING
