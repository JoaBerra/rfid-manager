package com.joakim.rfidmanager.domain.model

import com.joakim.rfidmanager.outbox.core.OutboxStatus

/**
 * Domain model for a persisted reading.
 *
 * Mirrors the metadata visible in the Fas 2 UI mocks and the
 * Figma Design Specification. Used between data layer and UI.
 */
data class PersistedReading(
    val id: Long,
    val type: String,           // "RFID" or "EAN"
    val uidOrCode: String,
    val timestamp: Long,
    val source: String? = null,
    val dataPreview: String? = null,
    val status: String = "persisted",

    // RFID/Sparkplug details
    val memoryBank: Int? = null,
    val address: Int? = null,
    val length: Int? = null,
    val payload: String? = null,
    val sparkplugJson: String? = null,
    val correlationId: String? = null,

    // Utkorg: sändningsstatus (sanningen), försök, senaste fel och tidpunkter
    val outboxStatus: OutboxStatus = OutboxStatus.PENDING,
    val attempts: Int = 0,
    val lastError: String? = null,
    val lastAttemptAt: Long? = null,
    val sentAt: Long? = null
) {
    /** Härledd: brokern har bekräftat leveransen. Ersätter det tidigare fria fältet 'transmitted'. */
    val transmitted: Boolean get() = outboxStatus == OutboxStatus.SENT

    fun isRfid(): Boolean = type.equals("RFID", ignoreCase = true)
    fun isEan(): Boolean = type.equals("EAN", ignoreCase = true)
}