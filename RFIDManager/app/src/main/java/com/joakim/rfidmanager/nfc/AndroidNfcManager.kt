package com.joakim.rfidmanager.nfc

import android.app.Activity
import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.NfcA
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.content.getSystemService
import com.joakim.rfidmanager.domain.model.RfidTag
import com.joakim.rfidmanager.domain.model.TagType
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * # NFC Layer — AndroidNfcManager (Architecture: NFC Layer, real impl of NfcManager)
 *
 * Detta är **den riktiga implementationen** som pratar med Androids NFC-stack.
 * Se [[App-Architecture]]: NFC Layer → NfcManager Interface → AndroidNfcManager (när hårdvara finns).
 *
 * **Centrala ID-begrepp (Architecture-Design-Källkod):**
 * - `handleTag(tag: Tag)` — kärnan: läser data (sectorsRead/pages), sätter `lastTag`, vibrerar, levererar RfidTag till UI via tagListener.
 * - `lastTag: Tag?` — **kortlivad cache** av den Tag som precis kom från reader callback. Endast giltig under/ strax efter callback.
 *   Används för "armed write" och "on-demand" läs/skriv utan att Tag blir stale (vanlig orsak till "Transceive failed" / TagLostException).
 * - `page 2 Lock/OTP` (Ultralight): läses alltid (startPage=2). LB0-parser i UI (RFIDManagerScreen) visar låsta pages.
 *   Design-förankring: Figma "use this to pick safe Target page" + varningar i SELECTED TAG MEMORY.
 * - `enableReaderMode` + `FLAG_READER_NO_PLATFORM_SOUNDS` — förhindrar systemets NFC-chooser (se log.md 2026-06-02 "Fix: System chooser").
 * - `onTagDiscovered` — bro för onNewIntent fallback (när användaren väljer appen i chooser ändå).
 *
 * **Kotlin-novis:** `lastTag` är privat mutable state som hanteras trådsäkert via att writes bara sker från reader-callback eller runBlocking
 * i MainActivity LaunchedEffect. `suspend fun` + `runOnUiThread` för att leverera till Compose state.
 *
 * **Eskortminne-fokus:** Stödjer både MIFARE Classic (sector auth) och Ultralight (page reads 4-byte). Användarens taggar var Ultralight
 * (UID t.ex. 0479981A8A6A80) med NDEF + skrivbar user memory (page 12 "74 65 73 74" = "test" lyckades 2026-06-03).
 */
class AndroidNfcManager(
    private val activity: Activity,
    private val settings: com.joakim.rfidmanager.data.settings.AppSettings? = null
) : NfcManager {

    private val TAG = "AndroidNfcManager"
    private var nfcAdapter: NfcAdapter? = null
    private var isScanning = false
    private var tagListener: ((RfidTag) -> Unit)? = null
    /**
     * **lastTag (NFC-READ-WRITE-FRESH / Architecture NFC layer)**
     *
     * Endast giltig *kort* tid efter att reader mode callbacken (onTagDiscovered) eller onNewIntent har levererat en Tag.
     * Sätts i handleTag.
     *
     * Varför? Androids Tag-objekt blir ogiltiga snabbt (TagLostException vid transceive om du väntar).
     * Lösning: "armed write" i UI (MainActivity.pendingWrite) väntar på nästa fresh detection, som garanterar att lastTag är levande.
     * Se writeMifareUltralightPage + MainActivity: onWrite + LaunchedEffect lambda som kör write när UID matchar.
     *
     * Design: Motsvarar Figma "Hold the selected tag to the phone NOW to write to page X" + status i accent-färg.
     */
    private var lastTag: Tag? = null  // Only valid briefly inside/ right after the reader callback. Used for post-detection reads/writes.

    private val vibrator: Vibrator? by lazy {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vm = activity.getSystemService<VibratorManager>()
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            activity.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private val soundPool: SoundPool? by lazy {
        try {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(attrs)
                .build()
        } catch (_: Exception) { null }
    }

    private var beepSoundId: Int = 0

    init {
        nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
        beepSoundId = soundPool?.load(activity, com.joakim.rfidmanager.R.raw.beep, 1) ?: 0
    }

    override fun isNfcEnabled(): Boolean {
        return nfcAdapter?.isEnabled == true
    }

    override fun startScanning(onTagDetected: (RfidTag) -> Unit) {
        val adapter = nfcAdapter
        if (adapter == null) {
            Log.w(TAG, "NFC adapter is null - NFC not supported or not available")
            return
        }
        if (!adapter.isEnabled) {
            Log.w(TAG, "NFC is not enabled on the device. Please enable NFC in settings.")
            return
        }

        // Always disable previous reader mode first to ensure clean state
        adapter.disableReaderMode(activity)

        tagListener = onTagDetected
        isScanning = true

        /**
         * **enableReaderMode flags (NFC-READER-MODE / Architecture + Android NFC stack)**
         *
         * FLAG_READER_* begränsar till relevanta teknologier.
         * **FLAG_READER_NO_PLATFORM_SOUNDS** är kritisk: den hindrar Android från att visa systemets NFC-chooser + ljud.
         * Utan den hamnar användaren i Chrome (NDEF) istället för att appens READ LOG fylls (se log.md 2026-06-02 "Fix: System chooser").
         *
         * Fallback: AndroidManifest + onNewIntent (TECH_DISCOVERED + NDEF) så appen kan väljas manuellt och ändå fylla listan.
         */
        val flags = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS

        val options = Bundle().apply {
            putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 5000)
        }

        adapter.enableReaderMode(activity, { tag ->
            Log.d(TAG, "Reader mode callback received tag")
            onTagDiscovered(tag)
        }, flags, options)

        Log.i(TAG, "Reader mode ENABLED successfully - tags should now be delivered to app only (no system chooser)")
    }

    override fun stopScanning() {
        nfcAdapter?.disableReaderMode(activity)
        isScanning = false
        tagListener = null
        Log.d(TAG, "Reader mode disabled")
    }

    override fun restartReaderMode() {
        val adapter = nfcAdapter
        val listener = tagListener
        if (adapter == null || listener == null || !adapter.isEnabled) return
        adapter.disableReaderMode(activity)
        val flags = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS
        val options = Bundle().apply {
            putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 5000)
        }
        adapter.enableReaderMode(activity, { tag ->
            onTagDiscovered(tag)
        }, flags, options)
        Log.d(TAG, "Reader mode restarted on resume (callback preserved)")
    }

    private fun handleTag(tag: Tag) {
        /**
         * **handleTag (NFC-HANDLE-TAG / Architecture NFC Layer core)**
         *
         * Detta är **hjärtat** i NFC-lagret.
         * 1. Sparar `lastTag = tag` (för armed write + färska transceive).
         * 2. Detekterar typ (Classic vs Ultralight/NTAG) från techList.
         * 3. **Läser data medan Tag är "live"**:
         *    - Classic: sector 0+ med default keys (MIFARE_CLASSIC_1K/4K).
         *    - Ultralight (eskortminne): alltid page 2 (Lock/OTP + CC), sedan 4,8,12,16... (4 bytes per page).
         *      `sectorsRead: Map<Int, ByteArray>` — nyckeln är page-nummer för Ultra, sector för Classic.
         * 4. Bygger RfidTag (domain) och levererar via tagListener till MainActivity (som mappar till ui.model.RFIDTag + detectedTags).
         * 5. Vibrerar (UX).
         *
         * **Varför läsa här och inte senare?** Tag-referensen är endast giltig under callbacken (se lastTag-kommentar).
         * Detta är anledningen till "armed write"-mönstret: skrivningen armeras i UI, exekveras på nästa detection.
         *
         * **Page 2 Lock/OTP (NFC-READ-LOCK / Ultralight specifikt):**
         * Många "giveaway" eskortminnen har låsta pages (LB0 i page 2). Vi läser alltid page 2 så UI kan varna
         * och föreslå safe target page (se RFIDManagerScreen lock parser + WriteTagForm defaultAddr).
         * Exempel från log: "20 48 F8 FF..." → LB0=0x20 betyder vissa pages låsta.
         * Verifierat: page 12 var skrivbar på användarens lanabgroup.se-tagg.
         *
         * Design-förankring (Figma-to-Compose): SELECTED TAG MEMORY + "Lock bits: pages X locked" i accent #F59E0B.
         * Se också Hardware-Testenheter.md för exakta Samsung-menytexter och udev-regler.
         */
        lastTag = tag  // keep briefly for possible immediate follow-up reads/writes from UI

        val uid = tag.id ?: byteArrayOf()
        val techList = tag.techList?.toList() ?: emptyList()
        val type = when {
            techList.any { it.contains("MifareClassic") } -> {
                if (MifareClassic.get(tag)?.size == MifareClassic.SIZE_1K) TagType.MIFARE_CLASSIC_1K
                else TagType.MIFARE_CLASSIC_4K
            }
            techList.any { it.contains("MifareUltralight") } -> TagType.MIFARE_ULTRALIGHT
            techList.any { it.contains("Ndef") || it.contains("NdefFormatable") } -> TagType.NTAG
            else -> TagType.UNKNOWN
        }

        // Simple RSSI proxy (real signal strength not directly exposed by Android NFC stack for most tags)
        val rssi = -50  // placeholder; can improve with presence checks later

        var sectorsRead = emptyMap<Int, ByteArray>()

        // If Mifare Classic, attempt to read some sectors *right here* while the Tag is still valid in this callback.
        // This is the only reliable place to do auth/read for escort memory without complex tag caching.
        if (type == TagType.MIFARE_CLASSIC_1K || type == TagType.MIFARE_CLASSIC_4K) {
            // Read as many sectors as possible with default keys (common for escort memory cards)
            // Start with 0 (manufacturer), then data sectors. Stop on first auth failure for speed.
            val maxSectors = if (type == TagType.MIFARE_CLASSIC_1K) 16 else 40
            for (s in 0 until maxSectors) {
                val data = readMifareClassicSectorInternal(tag, s, byteArrayOf())
                if (data != null) {
                    sectorsRead = sectorsRead + (s to data)
                    Log.i(TAG, "Sector $s read for ${uid.joinToString("") { "%02X".format(it) }}: ${data.joinToString(" ") { "%02X".format(it) }}")
                } else {
                    // If sector 0 fails we still want to continue? Usually 0 works. Stop early on consecutive fails.
                    if (s > 2) break
                }
            }
        } else if (type == TagType.MIFARE_ULTRALIGHT || techList.any { it.contains("MifareUltralight") || it.contains("Ndef") }) {
            // Read user data pages for Ultralight / NTAG (pages are 4 bytes, readPages returns 4 pages = 16 bytes)
            val mifareU = MifareUltralight.get(tag)
            if (mifareU != null) {
                try {
                    mifareU.connect()
                    // Read lock bytes and OTP/CC first (pages 2-3), then user memory.
                    for (startPage in listOf(2, 4, 8, 12, 16)) {
                        try {
                            val data = mifareU.readPages(startPage)  // 16 bytes
                            sectorsRead = sectorsRead + (startPage to data)
                            val label = if (startPage == 2) "Lock/OTP" else "pages"
                            Log.i(TAG, "Ultralight $label $startPage-${startPage + 3} read for ${uid.joinToString("") { "%02X".format(it) }}: ${data.joinToString(" ") { "%02X".format(it) }}")
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed reading Ultralight pages starting at $startPage", e)
                            if (startPage >= 4) break
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "MifareUltralight connect/read error", e)
                } finally {
                    try { mifareU.close() } catch (_: Exception) {}
                }
            }
        }

        val detected = RfidTag(
            uid = uid,
            type = type,
            rssi = rssi,
            timestamp = System.currentTimeMillis(),
            sectorsRead = sectorsRead
        )

        // Haptic + sound feedback on detection
        if (settings?.hapticEnabled?.value != false) vibrate()
        if (settings?.soundEnabled?.value != false) playBeep()

        Log.d(TAG, "Tag detected: ${detected.uidHex} type=$type techs=$techList sectorsRead=${sectorsRead.keys}")

        // Deliver on main thread
        activity.runOnUiThread {
            tagListener?.invoke(detected)
        }
    }

    private fun vibrate() {
        if (settings?.hapticEnabled?.value == false) return
        vibrator?.let {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(80)
            }
        }
    }

    private fun playBeep() {
        if (settings?.soundEnabled?.value == false) return
        beepSoundId.takeIf { it > 0 }?.let { id ->
            soundPool?.play(id, 1.0f, 1.0f, 1, 0, 1.0f)
        }
    }

    // --- Mifare Classic sector operations (to be called from background after detection) ---

    override suspend fun readMifareClassicSector(uid: ByteArray, sector: Int, keyA: ByteArray?): ByteArray? {
        val currentTag = lastTag
        if (currentTag != null && currentTag.id.contentEquals(uid)) {
            // We have a (very) recent live tag for this UID — use it
            return readMifareClassicSectorInternal(currentTag, sector, keyA ?: byteArrayOf())
        }
        Log.w(TAG, "readMifareClassicSector: no live Tag available for UID (must call immediately after detection or during callback)")
        return null
    }

    override suspend fun writeMifareClassicBlock(uid: ByteArray, block: Int, data: ByteArray, keyA: ByteArray?): Boolean {
        val currentTag = lastTag
        if (currentTag != null && currentTag.id.contentEquals(uid)) {
            return writeMifareClassicBlockInternal(currentTag, block, data, keyA ?: byteArrayOf())
        }
        Log.w(TAG, "writeMifareClassicBlock: no live Tag available for UID")
        return false
    }

    override suspend fun readMifareUltralightPages(uid: ByteArray, page: Int): ByteArray? {
        val currentTag = lastTag
        if (currentTag != null && currentTag.id.contentEquals(uid)) {
            return readMifareUltralightPagesInternal(currentTag, page)
        }
        Log.w(TAG, "readMifareUltralightPages: no live Tag available for UID")
        return null
    }

    override suspend fun writeMifareUltralightPage(uid: ByteArray, page: Int, data: ByteArray): Boolean {
        /**
         * **Armed write execution path (NFC-WRITE-ARMED)**
         *
         * UI (MainActivity.onWrite) armar `pendingWrite` + visar "Hold ... NOW".
         * När nästa fresh tag kommer in i handleTag → lastTag uppdateras → listenern i LaunchedEffect matchar UID
         * och anropar writeMifareUltralightPage (här) → använder live lastTag → internal writePage.
         *
         * Detta är lösningen på "write transceive fail on page 4" (lock bits + stale tag).
         * Verifierat 2026-06-03: page 12 "74 65 73 74" (test ASCII) på lanabgroup-tagg.
         */
        val currentTag = lastTag
        if (currentTag != null && currentTag.id.contentEquals(uid)) {
            return writeMifareUltralightPageInternal(currentTag, page, data)
        }
        Log.w(TAG, "writeMifareUltralightPage: no live Tag available for UID")
        return false
    }

    /**
     * Example of how a real Mifare Classic read would look when we have a live Tag.
     * Call this from within the reader callback or with a recently obtained Tag.
     */
    @Suppress("unused")
    private fun readMifareClassicSectorInternal(tag: Tag, sector: Int, keyA: ByteArray): ByteArray? {
        val mifare = MifareClassic.get(tag) ?: return null
        return try {
            mifare.connect()
            val sectorCount = mifare.sectorCount
            if (sector < 0 || sector >= sectorCount) return null

            val auth = if (keyA.isNotEmpty()) {
                mifare.authenticateSectorWithKeyA(sector, keyA)
            } else {
                // Common default keys for many escort memories / access cards
                val defaultKeys = listOf(
                    MifareClassic.KEY_DEFAULT,
                    MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY,
                    byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
                )
                defaultKeys.any { mifare.authenticateSectorWithKeyA(sector, it) }
            }

            if (!auth) {
                Log.w(TAG, "Auth failed for sector $sector")
                return null
            }

            val blockIndex = mifare.sectorToBlock(sector)
            val blockData = mifare.readBlock(blockIndex)  // 16 bytes typical
            blockData
        } catch (e: Exception) {
            Log.e(TAG, "Mifare read error sector $sector", e)
            null
        } finally {
            try { mifare.close() } catch (_: Exception) {}
        }
    }

    @Suppress("unused")
    private fun writeMifareClassicBlockInternal(tag: Tag, block: Int, data: ByteArray, keyA: ByteArray): Boolean {
        val mifare = MifareClassic.get(tag) ?: return false
        return try {
            mifare.connect()
            // Determine which sector the block belongs to
            var targetSector = -1
            for (s in 0 until mifare.sectorCount) {
                val firstBlock = mifare.sectorToBlock(s)
                val blocksInSector = if (mifare.size == MifareClassic.SIZE_1K && s < 32 || mifare.size == MifareClassic.SIZE_4K && s < 32) 4 else 16
                if (block >= firstBlock && block < firstBlock + blocksInSector) {
                    targetSector = s
                    break
                }
            }
            if (targetSector < 0) {
                Log.w(TAG, "Block $block does not belong to any sector")
                return false
            }

            val auth = if (keyA.isNotEmpty()) {
                mifare.authenticateSectorWithKeyA(targetSector, keyA)
            } else {
                val defaultKeys = listOf(
                    MifareClassic.KEY_DEFAULT,
                    MifareClassic.KEY_MIFARE_APPLICATION_DIRECTORY,
                    byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
                )
                defaultKeys.any { mifare.authenticateSectorWithKeyA(targetSector, it) }
            }

            if (!auth) {
                Log.w(TAG, "Auth failed for sector $targetSector (block $block)")
                return false
            }

            // Never write to the sector trailer (last block in sector) unless you really know what you're doing
            val firstBlock = mifare.sectorToBlock(targetSector)
            val blocksInSector = if (targetSector < 32) 4 else 16
            val trailerBlock = firstBlock + blocksInSector - 1
            if (block == trailerBlock) {
                Log.w(TAG, "Refusing to write to sector trailer block $block")
                return false
            }

            mifare.writeBlock(block, data)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Mifare write error block $block", e)
            false
        } finally {
            try { mifare.close() } catch (_: Exception) {}
        }
    }

    @Suppress("unused")
    private fun readMifareUltralightPagesInternal(tag: Tag, page: Int): ByteArray? {
        val mifareU = MifareUltralight.get(tag) ?: return null
        return try {
            mifareU.connect()
            mifareU.readPages(page)  // returns 16 bytes (4 pages)
        } catch (e: Exception) {
            Log.e(TAG, "Ultralight readPages error at page $page", e)
            null
        } finally {
            try { mifareU.close() } catch (_: Exception) {}
        }
    }

    @Suppress("unused")
    private fun writeMifareUltralightPageInternal(tag: Tag, page: Int, data: ByteArray): Boolean {
        /**
         * Utför den faktiska `mifareU.writePage(page, 4bytes)`.
         * Anropas **endast** med en Tag som just kom från handleTag (via lastTag).
         * Se writeMifareUltralightPage för det "armed" flödet.
         */
        val mifareU = MifareUltralight.get(tag) ?: return false
        return try {
            mifareU.connect()
            val writeData = data.copyOf(4)  // Ultralight pages are 4 bytes
            mifareU.writePage(page, writeData)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Ultralight writePage error at page $page", e)
            false
        } finally {
            try { mifareU.close() } catch (_: Exception) {}
        }
    }

    /**
     * **onTagDiscovered (NFC-FALLBACK / chooser bridge)**
     *
     * Anropas både från:
     * - Reader mode callback (det vanliga fallet efter enableReaderMode).
     * - MainActivity.onNewIntent (när användaren manuellt väljer "RFID Manager" i NFC-chooser).
     *
     * Detta är **fallback-mekanismen** som garanterar att READ LOG fylls även om systemdispatch tar över
     * (se AndroidManifest NDEF/TECH filters + processDiscoveredTag i MainActivity).
     *
     * ID: kopplas till "onTagDetectedFromIntent" koncept i äldre anteckningar.
     */
    fun onTagDiscovered(tag: Tag) {
        handleTag(tag)
    }
}
