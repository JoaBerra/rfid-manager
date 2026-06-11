package com.joakim.rfidmanager.domain.model

/**
 * # Domain Model — RfidTag (Architecture: Domain Layer)
 *
 * **Ren domänmodell** (inga Android-beroenden, ingen UI).
 * Se [[App-Architecture]]: Domain / Model → RfidTag.
 *
 * **ID-begrepp som används överallt:**
 * - `uid: ByteArray` + `uidHex` (computed) → primary key för taggar (t.ex. "0479981A8A6A80").
 * - `type: TagType` → MIFARE_ULTRALIGHT / NTAG för eskortminne (användarens taggar), Classic för andra.
 * - `sectorsRead: Map<Int, ByteArray>` → **nyckeln är page (Ultralight) eller sector (Classic)**.
 *   Ex: page 2 = lock/OTP, page 12 = user data ("74 65 73 74").
 *   Mappas i MainActivity.toUiTag() till ui.model.RFIDTag.fullSectors (hex-strängar för Compose).
 *
 * **Kotlin-novis:** `data class` ger equals/hashCode gratis, men vi override:ar för att jämföra på UID-innehåll
 * (ByteArray jämförs inte värdemässigt av default data class). `val uidHex` är en "computed property" (custom getter).
 *
 * Design-förankring: Flödar till Figma "READ LOG" rader (UID monospace, type badge, dataPreview) och "SELECTED TAG MEMORY".
 */
data class RfidTag(
    val uid: ByteArray,
    val type: TagType,
    val rssi: Int? = null,
    val timestamp: Long = System.currentTimeMillis(),
    /** Sectors successfully read during detection (sector -> 16-byte block data). Useful for escort memory inspection. */
    val sectorsRead: Map<Int, ByteArray> = emptyMap()
) {
    val uidHex: String
        get() = uid.joinToString("") { "%02X".format(it) }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RfidTag
        return uid.contentEquals(other.uid)
    }

    override fun hashCode(): Int = uid.contentHashCode()
}

/**
 * **TagType (Architecture Domain)**
 *
 * Mappas i MainActivity.toUiTag() till UI-strängar som "MIFARE Ultralight".
 * Används för att välja rätt write-metod (4-byte page vs 16-byte block) och labelPrefix "P" vs "S" i SELECTED.
 */
enum class TagType {
    MIFARE_CLASSIC_1K,
    MIFARE_CLASSIC_4K,
    MIFARE_ULTRALIGHT,
    NTAG,
    UNKNOWN
}
