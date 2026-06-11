package com.joakim.rfidmanager.ui.model

/**
 * # UI Model — RFIDTag (Architecture: UI Layer, presentation model)
 *
 * Lättviktig modell **endast för Compose**.
 * Skiljs från domain.model.RfidTag för att UI inte ska bero på ByteArray etc.
 *
 * **Mapping:** MainActivity.toUiTag() (extension på DomainRfidTag) → konverterar timestamp→readAt sträng,
 * sectorsRead ByteArray→hex-strängar i fullSectors, sätter "P" vs "S" prefix i dataPreview.
 *
 * **ID-begrepp:**
 * - `id` / `uid` (samma hex) → används för `selectedId: String?` i RFIDManagerScreen + RFIDTagList.
 * - `fullSectors` → drivande för "SELECTED TAG MEMORY" (större fonter, lock-bit parser för page 2, scrollable).
 * - `dataPreview` → visas i list-rader och top SELECTED summary.
 *
 * Design (Figma-to-Compose): Monospace för UID/hex (JetBrains Mono), accentfärg för previews, Primary #00FF88 för highlights.
 * Se RFIDTagList (signal bars, type badge) och RFIDManagerScreen (detaljerad minnesruta tidigt i vänster panel).
 */
data class RFIDTag(
    val id: String,
    val uid: String,
    val type: String,
    val rssi: Int,
    val readAt: String,
    /** Optional hex preview of first successfully read sector data (for escort memory inspection) */
    val dataPreview: String = "",
    /** Full hex dump of all sectors read during this detection (sector -> hex string). */
    val fullSectors: Map<Int, String> = emptyMap()
)
