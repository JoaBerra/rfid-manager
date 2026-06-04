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
import com.joakim.rfidmanager.ui.screens.RFIDManagerScreen
import com.joakim.rfidmanager.ui.model.RFIDTag
import com.joakim.rfidmanager.ui.theme.RFIDManagerTheme
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

/**
 * # MainActivity — Glue / Host (Architecture: UI Layer root + state hoisting)
 *
 * Detta är **roten** för appen. Den:
 * - Skapar AndroidNfcManager (NFC Layer).
 * - **Hoistar all state** (Compose best practice): detectedTags, selectedTagId, scanningEnabled, pendingWrite, writeStatusMessage.
 *   Varför? Så att lifecycle callbacks (onResume, onNewIntent, onPause) kan läsa/skriva samma state som Compose-trädet.
 * - Kopplar LaunchedEffect(scanningEnabled) → start/stop av NFC + **armed write execution**.
 * - Mappar domain RfidTag → ui.model.RFIDTag (toUiTag).
 * - Hanterar onNewIntent fallback för NFC chooser.
 *
 * **Centrala ID-begrepp (Architecture-Design-Källkod):**
 * - `selectedTagId: String?` → `selectedId` i RFIDManagerScreen/RFIDTagList (samma koncept).
 * - `onStartScan` / `onTagSelected` / `onWrite` callbacks → passerar ner till skärmen (se RFIDManagerScreen header).
 * - `pendingWrite` (data class här) + "armed" → det mönster som gjorde writes till eskortminne pålitliga (page 12 "test").
 * - `toUiTag()` → konverterar sectorsRead (domain, ByteArray, page/sector nycklar) till fullSectors (UI, hex strings).
 *
 * **Kotlin-novis nycklar:**
 * - `mutableStateListOf` + `mutableStateOf` → Compose observerar ändringar automatiskt (recomposition).
 * - `LaunchedEffect(key)` → kör side-effect (startScanning) när key ändras, cancellerar vid leave.
 * - `runBlocking` inuti callback för att köra suspend write synkront i detta fall (eftersom vi är i "fresh tag window").
 *
 * Se också onWrite lambda som armar, och LaunchedEffect som både startar scanning och exekverar pendingWrite när UID matchar.
 */
class MainActivity : ComponentActivity() {
    private lateinit var nfcManager: AndroidNfcManager

    // Hoisted state so lifecycle methods (onResume, onNewIntent) can access them.
    private val detectedTags = mutableStateListOf<RFIDTag>()
    private var scanningEnabled by mutableStateOf(false)
    private var selectedTagId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        nfcManager = AndroidNfcManager(this)

        setContent {
            RFIDManagerTheme {
                val scope = rememberCoroutineScope()

                /**
                 * **pendingWrite + armed execution (NFC-WRITE-ARMED central mönster)**
                 *
                 * Detta är den tekniska kärnan som gjorde "läsa och skriva till eskort minnen" möjligt.
                 *
                 * Flöde:
                 * 1. Användare väljer tagg i list (selectedTagId), går till WRITE, fyller data + target page (t.ex. 12).
                 * 2. Trycker "WRITE TO TAG" → onWrite lambda här armar `pendingWrite = PendingWrite(...)` + sätter status "Hold ... NOW".
                 * 3. Användaren håller taggen mot telefonen.
                 * 4. Ny detection kommer in via nfcManager.startScanning callback (som är inne i LaunchedEffect).
                 *    - UID matchar → runBlocking anropar writeMifare... (som använder live lastTag i AndroidNfcManager).
                 *    - Resultat loggas, Toast, status uppdateras.
                 *    - **Patch**: hex i detectedTags uppdateras direkt så SELECTED visar "74 65 73 74" utan att vänta på re-scan.
                 * 5. pending rensas.
                 *
                 * Varför detta? En Tag från en tidigare callback är "död" när du försöker writePage senare (TransceiveException).
                 * "Arma + vänta på nästa fresh detection" garanterar en levande Tag + lastTag.
                 *
                 * Se AndroidNfcManager: lastTag, handleTag, writeMifareUltralightPageInternal.
                 * Se log.md 2026-06-03: "User fick write to page 12 succeeded! (via pending)".
                 */
                data class PendingWrite(val uidHex: String, val addr: Int, val data: ByteArray, val isUltra: Boolean)
                var pendingWrite by remember { mutableStateOf<PendingWrite?>(null) }
                var writeStatusMessage by remember { mutableStateOf<String?>(null) }

                // Control scanning with a "enabled" flag.
                // LaunchedEffect reacts to changes while the composable is active.
                LaunchedEffect(scanningEnabled) {
                    if (scanningEnabled) {
                        detectedTags.clear()
                        nfcManager.startScanning { domainTag: DomainRfidTag ->
                            val uiTag = domainTag.toUiTag()
                            val existingIndex = detectedTags.indexOfFirst { it.uid == uiTag.uid }
                            if (existingIndex >= 0) {
                                detectedTags[existingIndex] = uiTag  // update with latest read (e.g. after write)
                            } else {
                                detectedTags.add(0, uiTag)
                            }

                            // If there is a pending write for this tag, execute it now while the tag (and lastTag) is fresh
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
                                    }
                                    pendingWrite = null
                                }
                            }
                        }
                    } else {
                        nfcManager.stopScanning()
                        pendingWrite = null
                        writeStatusMessage = null
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
                val onWrite: (String, Int) -> Unit = { text, addr ->
                    val selected = detectedTags.find { it.id == selectedTagId }
                    if (selected != null) {
                        val isUltra = selected.type.contains("ULTRALIGHT", ignoreCase = true) || selected.type.contains("NTAG", ignoreCase = true)
                        val data = if (isUltra) {
                            text.toByteArray(Charsets.UTF_8).copyOf(4)
                        } else {
                            text.toByteArray(Charsets.UTF_8).copyOf(16)
                        }
                        pendingWrite = PendingWrite(selected.uid, addr, data, isUltra)
                        writeStatusMessage = "Hold the selected tag to the phone NOW to write to ${if (isUltra) "page" else "block"} $addr"
                    } else {
                        writeStatusMessage = "No tag selected for write"
                    }
                }

                RFIDManagerScreen(
                    tags = detectedTags,
                    onStartScan = {
                        // Flip the desired scanning state. Effects and lifecycle will handle the rest.
                        scanningEnabled = !scanningEnabled
                    },
                    onTagSelected = { selectedTagId = it.id },
                    selectedId = selectedTagId,
                    isScanning = scanningEnabled,
                    onWrite = onWrite,
                    writeStatusMessage = writeStatusMessage
                )
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
        // If the user had scanning enabled before pause (e.g. screen off or app backgrounded),
        // re-enable reader mode now that we are resumed. This makes reader mode more reliable
        // across lifecycle events on some devices (including Samsung).
        if (::nfcManager.isInitialized && scanningEnabled) {
            // The LaunchedEffect will also react, but re-starting here ensures it's active
            // as soon as the activity is resumed.
            nfcManager.startScanning { domainTag: DomainRfidTag ->
                val uiTag = domainTag.toUiTag()
                if (detectedTags.none { it.uid == uiTag.uid }) {
                    detectedTags.add(0, uiTag)
                }
            }
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