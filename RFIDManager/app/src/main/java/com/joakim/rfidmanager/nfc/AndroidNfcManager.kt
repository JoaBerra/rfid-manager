package com.joakim.rfidmanager.nfc

import android.app.Activity
import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.content.getSystemService
import com.joakim.rfidmanager.domain.model.RfidTag
import com.joakim.rfidmanager.domain.model.TagType

/**
 * # NFC Layer — AndroidNfcManager (Architecture: NFC Layer, real impl of NfcManager)
 *
 * Detta är **den riktiga implementationen** som pratar med Androids NFC-stack.
 * Se [[App-Architecture]]: NFC Layer → NfcManager Interface → AndroidNfcManager (när hårdvara finns).
 */
class AndroidNfcManager(
    private val activity: Activity,
    private val settings: com.joakim.rfidmanager.data.settings.AppSettings? = null
) : NfcManager {

    private val TAG = "AndroidNfcManager"
    private var nfcAdapter: NfcAdapter? = null
    private var isScanning = false
    private var tagListener: ((RfidTag) -> Unit)? = null
    private var lastTag: Tag? = null  // Only valid briefly inside/ right after the reader callback. Used for post-detection reads/writes.

    private val vibrator: Vibrator? by lazy {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vm = activity.getSystemService<VibratorManager>()
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            activity.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
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
        enableReaderMode()
    }

    private fun enableReaderMode() {
        val adapter = nfcAdapter ?: return
        if (!adapter.isEnabled) return
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
            Log.d(TAG, "Reader mode callback received tag")
            onTagDiscovered(tag)
        }, flags, options)
        Log.i(TAG, "Reader mode ENABLED (background NDEF ready)")
    }

    override fun isNfcEnabled(): Boolean {
        return nfcAdapter?.isEnabled == true
    }

    override fun startScanning(onTagDetected: (RfidTag) -> Unit) {
        if (nfcAdapter == null) {
            Log.w(TAG, "NFC adapter is null - NFC not supported or not available")
            return
        }
        if (!nfcAdapter!!.isEnabled) {
            Log.w(TAG, "NFC is not enabled on the device. Please enable NFC in settings.")
            return
        }

        tagListener = onTagDetected
        isScanning = true
        enableReaderMode()
        Log.i(TAG, "Scanning STARTED — tags will appear in UI list")
    }

    override fun stopScanning() {
        isScanning = false
        Log.d(TAG, "Scanning STOPPED — reader mode stays active for background NDEF")
    }

    override fun restartReaderMode() {
        val adapter = nfcAdapter
        val listener = tagListener
        if (adapter == null || listener == null || !adapter.isEnabled) return
        isScanning = true
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

        val rssi = -50
        var sectorsRead = emptyMap<Int, ByteArray>()

        if (type == TagType.MIFARE_CLASSIC_1K || type == TagType.MIFARE_CLASSIC_4K) {
            val maxSectors = if (type == TagType.MIFARE_CLASSIC_1K) 16 else 40
            for (s in 0 until maxSectors) {
                val data = readMifareClassicSectorInternal(tag, s, byteArrayOf())
                if (data != null) {
                    sectorsRead = sectorsRead + (s to data)
                    Log.i(TAG, "Sector $s read for ${uid.joinToString("") { "%02X".format(it) }}: ${data.joinToString(" ") { "%02X".format(it) }}")
                } else {
                    if (s > 2) break
                }
            }
        } else if (type == TagType.MIFARE_ULTRALIGHT || techList.any { it.contains("MifareUltralight") || it.contains("Ndef") }) {
            val mifareU = MifareUltralight.get(tag)
            if (mifareU != null) {
                try {
                    mifareU.connect()
                    // Läs alltid sida 2 (Lock/OTP) samt sidorna 4..19, och fortsätt upp till sida 36 på NTAG
                    // om NDEF-meddelandet sträcker sig längre än 64 byte (t.ex. fasad://besiktning?...).
                    for (startPage in listOf(2, 4, 8, 12, 16, 20, 24, 28, 32, 36)) {
                        try {
                            val data = mifareU.readPages(startPage)  // 16 bytes (4 sidor)
                            sectorsRead = sectorsRead + (startPage to data)
                            val label = if (startPage == 2) "Lock/OTP" else "pages"
                            Log.i(TAG, "Ultralight $label $startPage-${startPage + 3} read for ${uid.joinToString("") { "%02X".format(it) }}: ${data.joinToString(" ") { "%02X".format(it) }}")
                            if (startPage >= 16 && (data.any { it == 0xFE.toByte() } || data.all { it == 0.toByte() })) {
                                break
                            }
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

            // Fallback för NDEF-taggar där MifareUltralight inte gav sidor men cachedNdefMessage finns
            if (sectorsRead[4] == null) {
                try {
                    val cached = Ndef.get(tag)?.cachedNdefMessage
                    val uri = cached?.records?.firstNotNullOfOrNull { rec ->
                        rec.toUri()?.toString()
                    }
                    if (uri != null && FasadNfcBridge.isFasadUri(uri)) {
                        FasadNfcBridge.encodeUriToUltralightPages(uri)?.let { encoded ->
                            sectorsRead = sectorsRead + encoded
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Cached NDEF fallback failed", e)
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

        if (settings?.hapticEnabled?.value != false) vibrate()
        if (settings?.soundEnabled?.value != false) playBeep()

        Log.d(TAG, "Tag detected: ${detected.uidHex} type=$type techs=$techList sectorsRead=${sectorsRead.keys}")

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

    override suspend fun readMifareClassicSector(uid: ByteArray, sector: Int, keyA: ByteArray?): ByteArray? {
        val currentTag = lastTag
        if (currentTag != null && currentTag.id.contentEquals(uid)) {
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
        val currentTag = lastTag
        if (currentTag != null && currentTag.id.contentEquals(uid)) {
            return writeMifareUltralightPageInternal(currentTag, page, data)
        }
        Log.w(TAG, "writeMifareUltralightPage: no live Tag available for UID")
        return false
    }

    override suspend fun writeMifareUltralightPages(uid: ByteArray, pages: Map<Int, ByteArray>): Boolean {
        if (pages.isEmpty()) return false
        val currentTag = lastTag
        if (currentTag != null && currentTag.id.contentEquals(uid)) {
            return writeMifareUltralightPagesInternal(currentTag, pages)
        }
        Log.w(TAG, "writeMifareUltralightPages: no live Tag available for UID")
        return false
    }

    private fun readMifareClassicSectorInternal(tag: Tag, sector: Int, keyA: ByteArray): ByteArray? {
        val mifare = MifareClassic.get(tag) ?: return null
        return try {
            mifare.connect()
            val sectorCount = mifare.sectorCount
            if (sector < 0 || sector >= sectorCount) return null

            val auth = if (keyA.isNotEmpty()) {
                mifare.authenticateSectorWithKeyA(sector, keyA)
            } else {
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
            mifare.readBlock(blockIndex)
        } catch (e: Exception) {
            Log.e(TAG, "Mifare read error sector $sector", e)
            null
        } finally {
            try { mifare.close() } catch (_: Exception) {}
        }
    }

    private fun writeMifareClassicBlockInternal(tag: Tag, block: Int, data: ByteArray, keyA: ByteArray): Boolean {
        val mifare = MifareClassic.get(tag) ?: return false
        return try {
            mifare.connect()
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

    private fun readMifareUltralightPagesInternal(tag: Tag, page: Int): ByteArray? {
        val mifareU = MifareUltralight.get(tag) ?: return null
        return try {
            mifareU.connect()
            mifareU.readPages(page)
        } catch (e: Exception) {
            Log.e(TAG, "Ultralight readPages error at page $page", e)
            null
        } finally {
            try { mifareU.close() } catch (_: Exception) {}
        }
    }

    private fun writeMifareUltralightPageInternal(tag: Tag, page: Int, data: ByteArray): Boolean {
        val mifareU = MifareUltralight.get(tag) ?: return false
        return try {
            mifareU.connect()
            val writeData = data.copyOf(4)
            mifareU.writePage(page, writeData)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Ultralight writePage error at page $page", e)
            false
        } finally {
            try { mifareU.close() } catch (_: Exception) {}
        }
    }

    private fun writeMifareUltralightPagesInternal(tag: Tag, pages: Map<Int, ByteArray>): Boolean {
        val mifareU = MifareUltralight.get(tag)
        if (mifareU != null) {
            return try {
                mifareU.connect()
                for ((page, data) in pages.entries.sortedBy { it.key }) {
                    mifareU.writePage(page, data.copyOf(4))
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "Ultralight multi-page write error", e)
                false
            } finally {
                try { mifareU.close() } catch (_: Exception) {}
            }
        }

        // Fallback via Ndef / NdefFormatable om MifareUltralight inte exponeras på enheten
        val uri = FasadNfcBridge.decodeFasadUriFromPages(pages) ?: return false
        val message = NdefMessage(arrayOf(NdefRecord.createUri(uri)))
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            return try {
                ndef.connect()
                if (!ndef.isWritable || ndef.maxSize < message.toByteArray().size) {
                    false
                } else {
                    ndef.writeNdefMessage(message)
                    true
                }
            } catch (e: Exception) {
                Log.e(TAG, "NDEF write error", e)
                false
            } finally {
                try { ndef.close() } catch (_: Exception) {}
            }
        }
        val formatable = NdefFormatable.get(tag)
        if (formatable != null) {
            return try {
                formatable.connect()
                formatable.format(message)
                true
            } catch (e: Exception) {
                Log.e(TAG, "NdefFormatable write error", e)
                false
            } finally {
                try { formatable.close() } catch (_: Exception) {}
            }
        }
        return false
    }

    fun onTagDiscovered(tag: Tag) {
        handleTag(tag)
    }
}
