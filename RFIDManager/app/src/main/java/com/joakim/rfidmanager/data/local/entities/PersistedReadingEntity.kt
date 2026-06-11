package com.joakim.rfidmanager.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for persisted readings (RFID escort memory or EAN barcode).
 *
 * Captures all metadata fields from the Fas 2 UI spec:
 * - uid/code
 * - timestamp
 * - source / location / context (e.g. "Gate 3 - Warehouse A", "Pallet 47-B")
 * - data preview + full Sparkplug data (memoryBank, address, length, payload)
 * - type (RFID / EAN)
 * - status (persisted, transmitted via Sparkplug)
 *
 * This maps directly to the fields visible in the reference images and the
 * Figma Design Specification (Figma-Design-Spec-Fas2).
 */
@Entity(tableName = "persisted_readings")
data class PersistedReadingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** "RFID" or "EAN" */
    val type: String,

    /** UID for RFID or EAN code (e.g. "300833B2E1A4F7C9" or "5907789123456") */
    val uidOrCode: String,

    /** Millis since epoch */
    val timestamp: Long,

    /** Human readable source/location (e.g. "Source: Gate 3 - Warehouse A", "Line 7 - Palletizer") */
    val source: String? = null,

    /** Short preview shown in list (e.g. "P12: 74 65 73 74" or first part of payload) */
    val dataPreview: String? = null,

    /** Current persistence status: "persisted", "transmitted", "sending" etc. */
    val status: String = "persisted",

    /** True if successfully sent via MQTT/Sparkplug */
    val transmitted: Boolean = false,

    // --- RFID / Sparkplug specific metadata (from JSON in mocks and spec) ---
    val memoryBank: Int? = null,
    val address: Int? = null,
    val length: Int? = null,

    /** Hex payload or full data string */
    val payload: String? = null,

    /** Full Sparkplug-style JSON for the message (for audit / retransmit) */
    val sparkplugJson: String? = null,

    /** Optional correlation / seq from MQTT */
    val correlationId: String? = null
)