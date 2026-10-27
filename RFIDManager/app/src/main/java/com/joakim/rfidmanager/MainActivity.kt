package com.joakim.rfidmanager

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.joakim.rfidmanager.domain.model.PersistedReading
import com.joakim.rfidmanager.domain.model.RfidTag as DomainRfidTag
import com.joakim.rfidmanager.domain.model.TagType
import com.joakim.rfidmanager.nfc.AndroidNfcManager
import com.joakim.rfidmanager.nfc.FasadNfcBridge
import com.joakim.rfidmanager.nfc.LastRead
import com.joakim.rfidmanager.nfc.WriteMode
import com.joakim.rfidmanager.nfc.WriteModeState
import com.joakim.rfidmanager.nfc.WriteRequest
import com.joakim.rfidmanager.ui.LocalLocalization
import com.joakim.rfidmanager.ui.MainScreenHost
import com.joakim.rfidmanager.ui.model.RFIDTag
import com.joakim.rfidmanager.ui.theme.RFIDManagerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

data class FasadBindStatus(
    val uidHex: String,
    val uri: String,
    val success: Boolean
)

/**
 * MainActivity — Lifecycle host for NFC reader mode, Fasad NFC bridge intents, and app container initialization.
 */
class MainActivity : ComponentActivity() {
    private lateinit var nfcManager: AndroidNfcManager

    lateinit var appContainer: AppContainer

    private val detectedTags = mutableStateListOf<RFIDTag>()
    private var scanningEnabled by mutableStateOf(false)

    // Skrivläge (ren logik i nfc/WriteMode.kt). Hoistat hit så att onPause kan avbryta det.
    private var writeState by mutableStateOf<WriteModeState>(WriteModeState.Idle)
    private var lastRead by mutableStateOf<LastRead?>(null)
    private var nowMs by mutableLongStateOf(System.currentTimeMillis())

    // Fasad-taggbindning (fasad.rfid.WRITE_TAG) och felmeddelande vid öppning av Fasad
    private var fasadTargetUri by mutableStateOf<String?>(null)
    private var fasadBindStatus by mutableStateOf<FasadBindStatus?>(null)
    private var fasadErrorMessage by mutableStateOf<String?>(null)

    private fun s(key: String, fallback: String): String =
        appContainer.localizationManager.strings.value[key] ?: fallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appContainer = AppContainer(this)
        nfcManager = AndroidNfcManager(this, appContainer.settings)

        handleIncomingFasadIntent(intent)

        setContent {
            val themeMode by appContainer.settings.themeMode.collectAsState()
            RFIDManagerTheme(themeMode = themeMode) {
                val scope = rememberCoroutineScope()

                BackHandler(enabled = writeState !is WriteModeState.Idle || fasadTargetUri != null) {
                    val wasFasadBind = fasadTargetUri != null
                    writeState = WriteMode.cancel(writeState)
                    fasadTargetUri = null
                    if (wasFasadBind) {
                        setResult(Activity.RESULT_CANCELED)
                        if (callingActivity != null) finish()
                    }
                }

                LaunchedEffect(writeState, lastRead) {
                    while (true) {
                        nowMs = System.currentTimeMillis()
                        val before = writeState
                        writeState = WriteMode.tick(writeState, nowMs)
                        val after = writeState
                        if (before is WriteModeState.Armed && after is WriteModeState.Finished) {
                            Toast.makeText(
                                this@MainActivity,
                                s("screen.scan.write_timeout", "Write mode cancelled (timeout)"),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        if (!(writeState is WriteModeState.Armed || WriteMode.isRecent(lastRead, nowMs))) break
                        delay(1000)
                    }
                }
                var writeStatusMessage by remember { mutableStateOf<String?>(null) }
                var persistAfterWrite by remember { mutableStateOf(true) }

                LaunchedEffect(scanningEnabled) {
                    if (scanningEnabled) {
                        detectedTags.clear()
                        val keepArmedFasad = (writeState as? WriteModeState.Armed)?.request?.fasadUri != null
                        if (!keepArmedFasad) {
                            writeState = WriteMode.cancel(writeState)
                        }
                        lastRead = null
                        nfcManager.startScanning { domainTag: DomainRfidTag ->
                            val uiTag = domainTag.toUiTag()
                            Log.i("RFIDManager", "scanCallback: uid=${uiTag.uid} dataPreview='${uiTag.dataPreview}' fullSectors.size=${uiTag.fullSectors.size}")

                            if (scanningEnabled) {
                                val existingIndex = detectedTags.indexOfFirst { it.uid == uiTag.uid }
                                if (existingIndex >= 0) {
                                    detectedTags[existingIndex] = uiTag
                                } else {
                                    detectedTags.add(0, uiTag)
                                }
                            }

                            val readNow = System.currentTimeMillis()
                            lastRead = LastRead(uiTag.uid, readNow, WriteMode.isWritable(uiTag.type, uiTag.fullSectors))
                            val outcome = WriteMode.onTagRead(writeState, uiTag.uid, readNow)
                            writeState = outcome.state
                            outcome.execute?.let { pw ->
                                val uidB = pw.uidHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                                if (pw.fasadUri != null) {
                                    val pagesToWrite = FasadNfcBridge.encodeUriToUltralightPages(pw.fasadUri)
                                    val canWriteFasad = pagesToWrite != null &&
                                        WriteMode.isWritable(uiTag.type, uiTag.fullSectors) &&
                                        FasadNfcBridge.canWriteUriToTag(pw.fasadUri, uiTag.fullSectors)
                                    val ok = if (canWriteFasad && pagesToWrite != null) {
                                        runBlocking { nfcManager.writeMifareUltralightPages(uidB, pagesToWrite) }
                                    } else {
                                        false
                                    }
                                    Log.i("RFIDManager", "Fasad write uri='${pw.fasadUri}' uid=${uiTag.uid} result=$ok")
                                    writeStatusMessage = if (ok) {
                                        s("screen.scan.fasad_bind_done", "✓ Fasad tag bound ({uid})").replace("{uid}", uiTag.uid)
                                    } else {
                                        s("screen.scan.fasad_bind_failed", "✗ Failed to write Fasad tag (locked tag or lost contact)")
                                    }
                                    Toast.makeText(this@MainActivity, writeStatusMessage, Toast.LENGTH_SHORT).show()

                                    if (ok && pagesToWrite != null) {
                                        val idx = detectedTags.indexOfFirst { it.uid == uiTag.uid }
                                        if (idx >= 0) {
                                            val current = detectedTags[idx]
                                            val updatedFull = current.fullSectors.toMutableMap()
                                            for ((page, pageBytes) in pagesToWrite) {
                                                updatedFull[page] = pageBytes.joinToString(" ") { "%02X".format(it) }
                                            }
                                            detectedTags[idx] = current.copy(fullSectors = updatedFull)
                                        }
                                        if (persistAfterWrite) {
                                            val persisted = PersistedReading(
                                                id = 0,
                                                type = "RFID",
                                                uidOrCode = pw.uidHex,
                                                timestamp = System.currentTimeMillis(),
                                                source = "Fasad write",
                                                dataPreview = pw.fasadUri,
                                                status = "persisted",
                                                memoryBank = 3,
                                                address = FasadNfcBridge.USER_START_PAGE,
                                                length = pagesToWrite.size * 4,
                                                payload = pagesToWrite.values.flatMap { it.toList() }.joinToString("") { "%02X".format(it) },
                                                sparkplugJson = null,
                                                correlationId = null
                                            )
                                            scope.launch {
                                                appContainer.persistedReadingRepository.saveReading(persisted)
                                            }
                                        }
                                        val resultIntent = Intent()
                                            .putExtra(FasadNfcBridge.EXTRA_UID, uiTag.uid)
                                            .putExtra(FasadNfcBridge.EXTRA_URI, pw.fasadUri)
                                        setResult(Activity.RESULT_OK, resultIntent)
                                        fasadBindStatus = FasadBindStatus(uidHex = uiTag.uid, uri = pw.fasadUri, success = true)
                                        fasadTargetUri = null
                                        writeState = WriteMode.finish(writeState, true)
                                        if (callingActivity != null) {
                                            finish()
                                        }
                                    } else {
                                        fasadBindStatus = FasadBindStatus(uidHex = uiTag.uid, uri = pw.fasadUri, success = false)
                                        writeState = WriteMode.finish(writeState, false)
                                    }
                                } else {
                                    val ok = runBlocking {
                                        if (pw.isUltra) {
                                            nfcManager.writeMifareUltralightPage(uidB, pw.addr, pw.data)
                                        } else {
                                            nfcManager.writeMifareClassicBlock(uidB, pw.addr, pw.data, null)
                                        }
                                    }
                                    Log.i("RFIDManager", "Write ${if (pw.isUltra) "Ultralight page" else "Classic block"} ${pw.addr} result=$ok (via pending)")
                                    writeStatusMessage = if (ok) s("screen.scan.write_done", "Write succeeded ({addr})").replace("{addr}", pw.addr.toString())
                                    else s("screen.scan.write_failed", "Write failed (locked or tag lost during write)")
                                    Toast.makeText(this@MainActivity, writeStatusMessage, Toast.LENGTH_SHORT).show()
                                    if (ok) {
                                        val idx = detectedTags.indexOfFirst { it.uid == uiTag.uid }
                                        if (idx >= 0) {
                                            val current = detectedTags[idx]
                                            val updatedFull = current.fullSectors.toMutableMap()
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
                                            detectedTags[idx] = current.copy(fullSectors = updatedFull)
                                        }

                                        if (persistAfterWrite) {
                                            val persisted = PersistedReading(
                                                id = 0,
                                                type = "RFID",
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
                                    writeState = WriteMode.finish(writeState, ok)
                                }
                            }
                        }
                    } else {
                        nfcManager.stopScanning()
                        detectedTags.clear()
                        writeState = WriteMode.cancel(writeState)
                        lastRead = null
                    }
                }

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
                            val msg = s("screen.scan.write_invalid_hex", "Invalid hex data")
                            writeStatusMessage = msg
                            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                        } else {
                            val padded = if (isUltra) bytes.copyOf(4) else bytes.copyOf(16)
                            writeState = WriteMode.arm(writeState, WriteRequest(uidHex = uidHex, addr = addr, data = padded, isUltra = isUltra), System.currentTimeMillis())
                            val msg = s("screen.scan.write_armed", "Write mode \u2013 hold the tag to the phone")
                            writeStatusMessage = msg
                            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                val onPersist: (RFIDTag) -> Unit = { tag ->
                    scope.launch {
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

                CompositionLocalProvider(LocalLocalization provides appContainer.localizationManager) {
                    MainScreenHost(
                        mqttManager = appContainer.mqttManager,
                        persistedReadingRepository = appContainer.persistedReadingRepository,
                        settings = appContainer.settings,
                        scanningEnabled = scanningEnabled,
                        onToggleScan = { scanningEnabled = !scanningEnabled },
                        detectedTags = detectedTags,
                        onWrite = onWrite,
                        onPersist = onPersist,
                        writeState = writeState,
                        lastRead = lastRead,
                        nowMs = nowMs,
                        onEnterWrite = { uid -> writeState = WriteMode.enter(writeState, lastRead, uid, System.currentTimeMillis()) },
                        onCancelWrite = {
                            val wasFasad = fasadTargetUri != null
                            writeState = WriteMode.cancel(writeState)
                            fasadTargetUri = null
                            fasadBindStatus = null
                            fasadErrorMessage = null
                            if (wasFasad) {
                                setResult(Activity.RESULT_CANCELED)
                                if (callingActivity != null) finish()
                            }
                        },
                        fasadTargetUri = fasadTargetUri,
                        fasadBindStatus = fasadBindStatus,
                        fasadErrorMessage = fasadErrorMessage,
                        onArmFasadWrite = {
                            fasadTargetUri?.let { uri ->
                                scanningEnabled = true
                                nowMs = System.currentTimeMillis()
                                writeState = WriteMode.armFasad(uri, nowMs)
                            }
                        },
                        onOpenInFasad = { uriString -> openInFasad(uriString) },
                        onDismissFasadMessage = {
                            fasadBindStatus = null
                            fasadErrorMessage = null
                        },
                        onSendNow = { appContainer.outboxScheduler.sendNow() },
                        onOutboxSettingsChanged = { appContainer.outboxScheduler.onSettingsChanged() }
                    )
                }
            }
        }
    }

    private fun handleIncomingFasadIntent(intent: Intent?): Boolean {
        if (intent?.action != FasadNfcBridge.ACTION_WRITE_TAG) return false
        val lgh = intent.getStringExtra(FasadNfcBridge.EXTRA_LGH)
        val rum = intent.getStringExtra(FasadNfcBridge.EXTRA_RUM)
        val del = intent.getStringExtra(FasadNfcBridge.EXTRA_DEL)
        val utrustning = intent.getStringExtra(FasadNfcBridge.EXTRA_UTRUSTNING)
        val targetUri = FasadNfcBridge.buildTargetUri(lgh = lgh, rum = rum, del = del, utrustning = utrustning)
        if (targetUri == null) {
            fasadTargetUri = null
            fasadBindStatus = null
            fasadErrorMessage = s("screen.scan.fasad_bind_invalid", "Invalid Fasad request (missing or invalid lgh)")
            setResult(Activity.RESULT_CANCELED)
            return true
        }
        fasadTargetUri = targetUri
        fasadBindStatus = null
        fasadErrorMessage = null
        scanningEnabled = true
        nowMs = System.currentTimeMillis()
        writeState = WriteMode.armFasad(targetUri, nowMs)
        Log.i("RFIDManager", "Armed Fasad tag bind for $targetUri")
        return true
    }

    private fun openInFasad(uriString: String) {
        fasadErrorMessage = null
        try {
            val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
            startActivity(viewIntent)
        } catch (e: ActivityNotFoundException) {
            val msg = s("screen.scan.fasad_not_installed", "The Fasad app (fasad.besiktning) is not installed on this phone")
            fasadErrorMessage = msg
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            Log.w("RFIDManager", "Fasad app not installed for URI $uriString", e)
        } catch (e: Exception) {
            val msg = s("screen.scan.fasad_not_installed", "The Fasad app (fasad.besiktning) is not installed on this phone")
            fasadErrorMessage = msg
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            Log.e("RFIDManager", "Failed launching Fasad for URI $uriString", e)
        }
    }

    override fun onPause() {
        super.onPause()
        writeState = WriteMode.cancel(writeState)
        if (::nfcManager.isInitialized) {
            nfcManager.stopScanning()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::nfcManager.isInitialized && scanningEnabled) {
            nfcManager.restartReaderMode()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (handleIncomingFasadIntent(intent)) {
            return
        }
        val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        if (tag != null) {
            processDiscoveredTag(tag)
            nfcManager.onTagDiscovered(tag)
        }
    }

    private fun processDiscoveredTag(tag: Tag) {
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
