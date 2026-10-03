package com.joakim.rfidmanager

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import com.joakim.rfidmanager.nfc.AndroidNfcManager
import com.joakim.rfidmanager.domain.model.RfidTag as DomainRfidTag
import com.joakim.rfidmanager.domain.model.TagType
import com.joakim.rfidmanager.domain.model.PersistedReading
import com.joakim.rfidmanager.ui.model.RFIDTag
import com.joakim.rfidmanager.ui.theme.RFIDManagerTheme
import com.joakim.rfidmanager.ui.MainScreenHost
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

import com.joakim.rfidmanager.ui.LocalLocalization
import com.joakim.rfidmanager.AppContainer

/**
 * MainActivity — Lifecycle host for NFC reader mode and app container initialization.
 */
class MainActivity : ComponentActivity() {
    private lateinit var nfcManager: AndroidNfcManager

    // A1 - Wire up databasen
    lateinit var appContainer: AppContainer

    // Hoisted state so lifecycle methods (onResume, onNewIntent) can access them.
    private val detectedTags = mutableStateListOf<RFIDTag>()
    private var scanningEnabled by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // A1 - Initiera AppContainer (databas + repositories)
        appContainer = AppContainer(this)

        nfcManager = AndroidNfcManager(this, appContainer.settings)

        setContent {
            val themeMode by appContainer.settings.themeMode.collectAsState()
            RFIDManagerTheme(themeMode = themeMode) {
                // === Armed NFC write logic (kept inside composable for remember/LaunchedEffect) ===
                // The low-level "hold tag NOW to write" mechanism still lives here because
                // the NFC callbacks and lastTag live in the Activity lifecycle.
                // In a later micro-step this will be moved/refactored so the write form in ScanScreen
                // can pass a callback instead of duplicating all this state.
                // Previous manual attempts' hybrid code removed.
                val scope = rememberCoroutineScope()

                data class PendingWrite(val uidHex: String, val addr: Int, val data: ByteArray, val isUltra: Boolean)
                var pendingWrite by remember { mutableStateOf<PendingWrite?>(null) }
                var writeStatusMessage by remember { mutableStateOf<String?>(null) }

                // A1 + A4: persistens states
                var persistAfterWrite by remember { mutableStateOf(true) }
                var showMqttStatus by remember { mutableStateOf(false) }

                // Control scanning with a "enabled" flag.
                LaunchedEffect(scanningEnabled) {
                    if (scanningEnabled) {
                        detectedTags.clear()
                        nfcManager.startScanning { domainTag: DomainRfidTag ->
                            val uiTag = domainTag.toUiTag()
                            Log.i("RFIDManager", "scanCallback: uid=${uiTag.uid} dataPreview='${uiTag.dataPreview}' fullSectors.size=${uiTag.fullSectors.size}")

                            // Only update the UI list when scanning is active
                            if (scanningEnabled) {
                                val existingIndex = detectedTags.indexOfFirst { it.uid == uiTag.uid }
                                if (existingIndex >= 0) {
                                    detectedTags[existingIndex] = uiTag  // update with latest read (e.g. after write)
                                    Log.i("RFIDManager", "scanCallback: UPDATED index=$existingIndex")
                                } else {
                                    detectedTags.add(0, uiTag)
                                    Log.i("RFIDManager", "scanCallback: ADDED at front, list now has ${detectedTags.size} entries")
                                }
                            }

                            // Always check pending writes (works even when scanning is "paused")
                            pendingWrite?.let { pw ->
                                if (uiTag.uid == pw.uidHex) {
                                    val uidB = pw.uidHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                                    val ok = runBlocking {
                                        if (pw.isUltra) {
                                            nfcManager.writeMifareUltralightPage(uidB, pw.addr, pw.data)
                                        } else {
                                            nfcManager.writeMifareClassicBlock(uidB, pw.addr, pw.data, null)
                                        }
                                    }
                                    Log.i("RFIDManager", "Write ${if (pw.isUltra) "Ultralight page" else "Classic block"} ${pw.addr} result=$ok (via pending)")
                                    writeStatusMessage = if (ok) "Write to ${if (pw.isUltra) "page" else "block"} ${pw.addr} succeeded!" else "Write failed (locked or tag lost during write)"
                                    Toast.makeText(this@MainActivity, writeStatusMessage, Toast.LENGTH_SHORT).show()
                                    if (ok) {
                                        // Patch the hex in the list entry for immediate UI verification without re-scan
                                        val idx = detectedTags.indexOfFirst { it.uid == uiTag.uid }
                                        if (idx >= 0) {
                                            val current = detectedTags[idx]
                                            val updatedFull = current.fullSectors.toMutableMap()
                                            // For Ultralight, blocks are 16-byte under start keys like 12 for pages 12-15
                                            val blockStart = updatedFull.keys.filter { it <= pw.addr }.maxOrNull() ?: pw.addr
                                            if (updatedFull.containsKey(blockStart)) {
                                                val oldHexList = updatedFull[blockStart]!!.split(" ").toMutableList()
                                                val writeHexList = pw.data.map { "%02X".format(it) }
                                                val byteOffset = (pw.addr - blockStart) * 4
                                                for (i in 0 until 4) {
                                                    if (byteOffset + i < oldHexList.size) {
                                                        oldHexList[byteOffset + i] = writeHexList.getOrNull(i) ?: "00"
                                                    }
                                                }
                                                updatedFull[blockStart] = oldHexList.joinToString(" ")
                                            }
                                            val patched = current.copy(fullSectors = updatedFull)
                                            detectedTags[idx] = patched
                                        }

                                        // A4: auto-persist efter lyckad write (enkelt exempel)
                                        if (persistAfterWrite) {
                                            val persisted = PersistedReading(
                                                id = 0,
                                                type = if (pw.isUltra) "RFID" else "RFID", // förenklat
                                                uidOrCode = pw.uidHex,
                                                timestamp = System.currentTimeMillis(),
                                                source = "Manual write page ${pw.addr}",
                                                dataPreview = pw.data.joinToString(" ") { "%02X".format(it) },
                                                status = "persisted",
                                                memoryBank = if (pw.isUltra) 3 else null,
                                                address = pw.addr,
                                                length = if (pw.isUltra) 4 else 16,
                                                payload = pw.data.joinToString("") { "%02X".format(it) },
                                                sparkplugJson = null,
                                                correlationId = null
                                            )
                                            scope.launch {
                                                appContainer.persistedReadingRepository.saveReading(persisted)
                                            }
                                        }
                                    }
                                    pendingWrite = null
                                }
                            }
                        }
                    } else {
                        nfcManager.stopScanning()
                        detectedTags.clear()
                        // Keep reader mode alive for background NDEF — don't null pendingWrite
                    }
                }

                /**
                 * **onWrite (UI → armed)**
                 *
                 * Anropas från WriteTagForm (via writeWithFeedback).
                 * Kollar selectedTagId (Architecture selectedId), bestämmer Ultra/Classic, kopierar data till 4/16 byte,
                 * **armar** pendingWrite, sätter UI-status som visas i Accent i formen.
                 * Den faktiska writen sker senare i LaunchedEffect-callbacken när en fresh detection matchar.
                 */
                val onWrite: (String, Int, String) -> Unit = { text, addr, uidHex ->
                    val matchTag = detectedTags.firstOrNull { it.uid == uidHex }
                    if (matchTag == null) {
                        Log.w("RFIDManager", "onWrite: no matching tag for uid=$uidHex")
                    } else {
                        val isUltra = matchTag.type.contains("ULTRALIGHT", ignoreCase = true) || matchTag.type.contains("NTAG", ignoreCase = true)
                        val cleanHex = text.filter { !it.isWhitespace() }
                        val bytes = try {
                            if (cleanHex.length % 2 == 0) {
                                cleanHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                            } else {
                                byteArrayOf()
                            }
                        } catch (e: NumberFormatException) {
                            Log.e("RFIDManager", "write: invalid hex \"$cleanHex\" — ${e.message}")
                            byteArrayOf()
                        }
                        if (bytes.isEmpty()) {
                            writeStatusMessage = "Invalid hex data"
                            Toast.makeText(this@MainActivity, "Invalid hex data", Toast.LENGTH_SHORT).show()
                        } else {
                            val padded = if (isUltra) bytes.copyOf(4) else bytes.copyOf(16)
                            pendingWrite = PendingWrite(uidHex = uidHex, addr = addr, data = padded, isUltra = isUltra)
                            val msg = "\u26A1 Save ready \u2013 hold tag to the phone"
                            writeStatusMessage = msg
                            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                            Log.i("RFIDManager", "onWrite: armed write for $uidHex addr=$addr isUltra=$isUltra data=${padded.joinToString(" ") { "%02X".format(it) }}")
                        }
                    }
                }

                val onPersist: (com.joakim.rfidmanager.ui.model.RFIDTag) -> Unit = { tag ->
                    // Direct persist from the Scan "Persist this read" button using the *exact* tag object
                    // from the moment the button was pressed. This guarantees that different detected
                    // tags (different UID, different read data, different tech type) produce distinct
                    // PersistedReading records instead of always re-using the first one's data.
                    scope.launch {
                        Log.i("RFIDManager", "onPersist: uid=${tag.uid} dataPreview='${tag.dataPreview}' fullSectors.size=${tag.fullSectors.size}")
                        val persisted = PersistedReading(
                            id = System.currentTimeMillis(),
                            type = "RFID",
                            uidOrCode = tag.uid,
                            timestamp = System.currentTimeMillis(),
                            source = "NFC",
                            dataPreview = tag.dataPreview,
                            memoryBank = null,
                            address = 0,
                            length = 0,
                            payload = if (tag.fullSectors.isNotEmpty()) {
                                tag.fullSectors.values.firstOrNull() ?: ""
                            } else "",
                            sparkplugJson = null,
                            correlationId = null
                        )
                        val saved = appContainer.persistedReadingRepository.saveReading(persisted)
                        val msg = if (saved) "Read persisted for ${tag.uid}" else "FAILED to persist read for ${tag.uid}"
                        writeStatusMessage = msg
                        Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                    }
                }

                // MainScreenHost with bottom navigation (4 dedicated screens).
                CompositionLocalProvider(LocalLocalization provides appContainer.localizationManager) {
                    MainScreenHost(
                        mqttManager = appContainer.mqttManager,
                        persistedReadingRepository = appContainer.persistedReadingRepository,
                        settings = appContainer.settings,
                        // Scan wiring for Punkt 4 (test scanning in new structure)
                        scanningEnabled = scanningEnabled,
                        onToggleScan = { scanningEnabled = !scanningEnabled },
                        detectedTags = detectedTags,
                        onWrite = onWrite,
                        onPersist = onPersist,
                        onSendNow = { appContainer.outboxScheduler.sendNow() }
                    )
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (::nfcManager.isInitialized) {
            nfcManager.stopScanning()
        }
    }

    override fun onResume() {
        super.onResume()
        // Restart reader mode on resume (system may have paused it in background)
        // WITHOUT replacing the scanning callback (preserves pendingWrite handler from LaunchedEffect).
        if (::nfcManager.isInitialized && scanningEnabled) {
            nfcManager.restartReaderMode()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        if (tag != null) {
            // Always process when the app is explicitly chosen from the NFC chooser.
            // This ensures the READ LOG gets populated even if reader mode didn't catch it.
            // Se AndroidNfcManager.onTagDiscovered + AndroidManifest TECH/NDEF filters.
            processDiscoveredTag(tag)
            // Also let the manager do its full handling (reads, lastTag, vibrate, logs)
            nfcManager.onTagDiscovered(tag)
        }
    }

    private fun processDiscoveredTag(tag: Tag) {
        /**
         * Fallback-path för chooser (när enableReaderMode inte tog taggen).
         * Skapar en minimal ui-tag (ingen full sectorsRead här — det gör handleTag via onTagDiscovered).
         * Se AndroidManifest + log "Fix: System chooser appearing".
         */
        val uid = tag.id ?: byteArrayOf()
        val uidHex = uid.joinToString("") { "%02X".format(it) }
        val techList = tag.techList?.toList() ?: emptyList()
        val typeStr = when {
            techList.any { it.contains("MifareClassic") } -> {
                if (MifareClassic.get(tag)?.size == MifareClassic.SIZE_1K) "MIFARE Classic 1K"
                else "MIFARE Classic 4K"
            }
            techList.any { it.contains("MifareUltralight") } -> "MIFARE Ultralight"
            techList.any { it.contains("Ndef") || it.contains("NdefFormatable") } -> "NTAG / NDEF"
            else -> "UNKNOWN"
        }
        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val uiTag = RFIDTag(
            id = uidHex,
            uid = uidHex,
            type = typeStr,
            rssi = -50,
            readAt = timeStr,
            dataPreview = "",
            fullSectors = emptyMap()
        )
        if (detectedTags.none { it.uid == uiTag.uid }) {
            detectedTags.add(0, uiTag)
        }
        Log.i("RFIDManager", "Processed tag via onNewIntent (chooser): $uidHex")
    }
}

private fun DomainRfidTag.toUiTag(): RFIDTag {
    /**
     * **toUiTag (Domain → UI mapping)**
     *
     * Konverterar den rena domain RfidTag (från NFC Layer / AndroidNfcManager.handleTag) till
     * ui.model.RFIDTag som Compose kan konsumera (strings, hex maps).
     *
     * Viktigt:
     * - sectorsRead nycklar (Int page/sector) behålls som fullSectors.
     * - Första entryn → dataPreview med "P12: 74 65 73 74 ..." eller "S0: ...".
     * - Används både vid första add och vid re-detect efter write (så SELECTED uppdateras).
     *
     * Detta är bron mellan NFC-lagrets "sanning" (bytes + live Tag) och UI:ns "SELECTED TAG MEMORY" + list.
     */
    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        .format(Date(this.timestamp))
    val typeStr = when (this.type) {
        TagType.MIFARE_CLASSIC_1K -> "MIFARE Classic 1K"
        TagType.MIFARE_CLASSIC_4K -> "MIFARE Classic 4K"
        TagType.MIFARE_ULTRALIGHT -> "MIFARE Ultralight"
        TagType.NTAG -> "NTAG"
        else -> "UNKNOWN"
    }

    val isUltra = typeStr.contains("ULTRALIGHT", ignoreCase = true) || typeStr.contains("NTAG", ignoreCase = true)
    val prefix = if (isUltra) "P" else "S"
    val preview = this.sectorsRead.entries.firstOrNull()?.let { (addr, bytes) ->
        "$prefix$addr: " + bytes.take(8).joinToString(" ") { "%02X".format(it) } + "..."
    } ?: ""

    val fullSectors = this.sectorsRead.mapValues { (_, bytes) ->
        bytes.joinToString(" ") { "%02X".format(it) }
    }

    return RFIDTag(
        id = this.uidHex,
        uid = this.uidHex,
        type = typeStr,
        rssi = this.rssi ?: -55,
        readAt = timeStr,
        dataPreview = preview,
        fullSectors = fullSectors
    )
}