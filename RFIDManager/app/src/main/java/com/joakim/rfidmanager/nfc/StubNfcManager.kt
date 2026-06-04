package com.joakim.rfidmanager.nfc

import com.joakim.rfidmanager.domain.model.RfidTag
import com.joakim.rfidmanager.domain.model.TagType

/**
 * # NFC Layer — StubNfcManager (Architecture: NFC Layer, dev stub)
 *
 * Används under UI-utveckling (Figma→Compose fasen juni 2026) innan fysisk Galaxy Note 10 fanns.
 * Returnerar syntetisk data så att RFIDManagerScreen + RFIDTagList + WriteTagForm kan testas utan telefon/NFC.
 *
 * **ID-koppling:** Samma RfidTag + sectorsRead Map som den riktiga AndroidNfcManager.
 * I MainActivity byttes den ut mot AndroidNfcManager när "Riktig AndroidNfcManager inkopplad" (log 2026-06-02).
 *
 * Design: Låter radar, StatCards, SELECTED, "NO TAGS DETECTED" etc. itereras mot Figma-tokens (#00FF88 etc).
 */
class StubNfcManager : NfcManager {

    private var isScanning = false
    private var listener: ((RfidTag) -> Unit)? = null

    override fun startScanning(onTagDetected: (RfidTag) -> Unit) {
        isScanning = true
        listener = onTagDetected
        // TODO: When real device is available, replace with real NfcAdapter.enableReaderMode
    }

    override fun stopScanning() {
        isScanning = false
        listener = null
    }

    override fun isNfcEnabled(): Boolean = true // Pretend NFC is always on in stub

    override suspend fun readMifareClassicSector(uid: ByteArray, sector: Int, keyA: ByteArray?): ByteArray? {
        // Return fake 16-byte sector data
        return ByteArray(16) { (it + sector).toByte() }
    }

    override suspend fun writeMifareClassicBlock(uid: ByteArray, block: Int, data: ByteArray, keyA: ByteArray?): Boolean {
        // Pretend write always succeeds in stub mode
        return true
    }

    override suspend fun readMifareUltralightPages(uid: ByteArray, page: Int): ByteArray? {
        // Return fake 16-byte (4 pages) data for stub
        return ByteArray(16) { (it + page).toByte() }
    }

    override suspend fun writeMifareUltralightPage(uid: ByteArray, page: Int, data: ByteArray): Boolean {
        // Pretend success
        return true
    }
}
