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
 *
 * **Varför interface? (Kotlin-novis-tips)**
 * - Gör det lätt att byta StubNfcManager (dev utan telefon) mot AndroidNfcManager (riktig hårdvara).
 * - UI (MainActivity/RFIDManagerScreen) beror bara på detta kontrakt → ren separation av lager.
 *
 * Design-förankring: Figma "READ LOG / WRITE TAG" flikar + "hold tag NOW" instruktioner mappar direkt till
 * armed write-mönstret som använder detta interface.
 */
interface NfcManager {

    /**
     * Starta NFC-lyssning (foreground reader mode).
     *
     * **Architecture-ID:** `onStartScan` (anropas från RFIDManagerScreen via MainActivity.scanningEnabled + LaunchedEffect).
     * **Källkod:** AndroidNfcManager använder `NfcAdapter.enableReaderMode` med `FLAG_READER_NO_PLATFORM_SOUNDS`
     * för att undvika systemets chooser-dialog (se historik i log.md 2026-06-02).
     *
     * @param onTagDetected callback som levererar en **RfidTag** (med sectorsRead ifylld från handleTag).
     *   Detta är "ID-konceptet" som flödar från NFC → Domain → UI (detectedTags i MainActivity).
     */
    fun startScanning(onTagDetected: (RfidTag) -> Unit)

    /**
     * Stoppa NFC-lyssning.
     *
     * Anropas från onPause, "STOP SCAN", och när pendingWrite rensas.
     * Motsvarar isScanning=false i UI-lagret.
     */
    fun stopScanning()

    /**
     * Re-enable reader mode on activity resume WITHOUT replacing the callback.
     * Preserves the existing tagListener (set by LaunchedEffect's startScanning).
     */
    fun restartReaderMode()

    /**
     * Returnerar true om NFC är påslaget i systemet.
     * Används för UI-status (t.ex. i framtida statuskort).
     */
    fun isNfcEnabled(): Boolean

    /**
     * Läs en sektor från MIFARE Classic (16 byte block).
     *
     * **Används sällan direkt** — den riktiga läsningen sker i handleTag medan Tag är "live".
     * Behåller API för symmetri + framtida "on-demand" läsningar.
     *
     * @param uid för att matcha mot lastTag (se "armed/lastTag pattern").
     */
    suspend fun readMifareClassicSector(uid: ByteArray, sector: Int, keyA: ByteArray?): ByteArray?

    /**
     * Skriv ett block (16 byte) till MIFARE Classic.
     * Skyddar trailer-block internt (se writeMifareClassicBlockInternal).
     */
    suspend fun writeMifareClassicBlock(uid: ByteArray, block: Int, data: ByteArray, keyA: ByteArray?): Boolean

    /**
     * Läs 4 sidor (16 byte) från MIFARE Ultralight / NTAG.
     *
     * **Viktigt ID:** `sectorsRead` nycklar är här *page numbers* (2,4,8,12,16...) inte sector.
     * Används för eskortminne (användarens taggar är Ultralight).
     *
     * I UI visas som "P12:" etc (se RFIDManagerScreen "SELECTED TAG MEMORY").
     */
    suspend fun readMifareUltralightPages(uid: ByteArray, page: Int): ByteArray?

    /**
     * Skriv exakt 4 byte till en specifik page på Ultralight/NTAG.
     *
     * **Kritiskt mönster (Architecture NFC + "armed write"):**
     * - UI (WriteTagForm) armar via `onWrite` → pendingWrite i MainActivity.
     * - Nästa onTagDetected (fresh Tag från reader callback) matchar UID och exekverar write via detta.
     * - Detta undviker att använda en "död" Tag-referens (som ger TransceiveException/TagLost).
     *
     * Se MainActivity:pendingWrite + onTagDetected lambda, och AndroidNfcManager.lastTag.
     */
    suspend fun writeMifareUltralightPage(uid: ByteArray, page: Int, data: ByteArray): Boolean
}
