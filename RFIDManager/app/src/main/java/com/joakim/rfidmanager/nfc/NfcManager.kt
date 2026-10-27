package com.joakim.rfidmanager.nfc

import com.joakim.rfidmanager.domain.model.RfidTag

/**
 * # NFC Layer — NfcManager (Architecture: NFC Layer)
 *
 * Detta är **kontraktet** (interface) mellan UI-lagret och NFC-hårdvaran.
 * Se [[App-Architecture]] (Mermaid: UI → NFC Layer → NfcManager Interface).
 *
 * **ID-begrepp som förankras här (används genomgående i Architecture-Design-Källkod):**
 * - `startScanning(onTagDetected: (RfidTag)->Unit)` → motsvarar `onStartScan` i RFIDManagerScreen + MainActivity.
 * - `RfidTag` (domain.model) → bär `uid`, `type` (TagType), `sectorsRead: Map<Int, ByteArray>` (nyckel = sector/page).
 * - `readMifareUltralightPages` / `writeMifareUltralightPage` → för eskortminnen (användarens MIFARE_ULTRALIGHT/NTAG).
 * - `lastTag` (i impl) + "armed write" (pending i UI) → löser TagLostException vid skrivning (se AndroidNfcManager + MainActivity:pendingWrite).
 */
interface NfcManager {

    fun startScanning(onTagDetected: (RfidTag) -> Unit)

    fun stopScanning()

    fun restartReaderMode()

    fun isNfcEnabled(): Boolean

    suspend fun readMifareClassicSector(uid: ByteArray, sector: Int, keyA: ByteArray?): ByteArray?

    suspend fun writeMifareClassicBlock(uid: ByteArray, block: Int, data: ByteArray, keyA: ByteArray?): Boolean

    suspend fun readMifareUltralightPages(uid: ByteArray, page: Int): ByteArray?

    suspend fun writeMifareUltralightPage(uid: ByteArray, page: Int, data: ByteArray): Boolean

    /**
     * Skriv flera 4-byte-sidor i följd på MIFARE Ultralight / NTAG (t.ex. NDEF Type 2 TLV för `fasad://besiktning?...`).
     */
    suspend fun writeMifareUltralightPages(uid: ByteArray, pages: Map<Int, ByteArray>): Boolean {
        if (pages.isEmpty()) return false
        for ((page, data) in pages.entries.sortedBy { it.key }) {
            if (!writeMifareUltralightPage(uid, page, data)) return false
        }
        return true
    }
}
