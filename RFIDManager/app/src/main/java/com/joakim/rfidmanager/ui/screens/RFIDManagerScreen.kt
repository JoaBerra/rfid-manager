package com.joakim.rfidmanager.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joakim.rfidmanager.ui.model.RFIDTag
import com.joakim.rfidmanager.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * # UI Layer — RFIDManagerScreen (Architecture: UI Layer huvudskärm)
 *
 * **Huvudskärmen** för hela appen. Följer Figma-designen "RFID MANAGER" (se [[Figma-to-Compose]]).
 *
 * **Layout (Architecture + Design):**
 * - Vänster (0.55f, scrollable): RadarView (Canvas + InfiniteTransition), StatCard, SELECTED summary + detaljerad "SELECTED TAG MEMORY"
 *   (med full hex + LB0 lock parser för page 2), LAST DETECTED.
 * - Höger (0.45f): TabRow (READ / WRITE) + RFIDTagList eller WriteTagForm.
 *
 * **State & Callbacks (hoisting — Kotlin/Compose mönster för novis):**
 * - All mutable state (detectedTags, selectedTagId, scanningEnabled, pendingWrite, writeStatusMessage) ligger i MainActivity.
 * - Här tar vi emot `tags`, `selectedId`, `isScanning` som parametrar + callbacks:
 *   - `onStartScan` → togglar scanningEnabled → LaunchedEffect startar/stopar AndroidNfcManager.
 *   - `onTagSelected: (RFIDTag)->Unit` → sätter selectedTagId (drivande för vänster SELECTED + Write form default).
 *   - `onWrite: (text, addr)` → **armar** pendingWrite (se MainActivity). Visar "Hold ... NOW" i accent.
 *
 * **ID-begrepp som förankras (genomgående):**
 * - `selectedId: String?` (från Architecture "selectedTagId") → kopplar list-rad → vänster memory + write target.
 * - `fullSectors` + lock parser (page 2 LB0) → "use this to pick safe Target page on right".
 * - `onWrite` arming → NFC-WRITE-ARMED mönster (Architecture NFC + Design "WRITE TO TAG" instruktioner).
 *
 * **Scroll fix (2026-06-03):** Hela vänster Column har .verticalScroll så P12+ syns (användaren ville ha större fonter här).
 * Detaljerad SELECTED TAG MEMORY placerad tidigt (före radar) per feedback.
 *
 * Design tokens (Color.kt): Primary=#00FF88 neon, Accent=#F59E0B, Monospace tungt, Radius=2.dp, mörk terminal-estetik.
 */
@Composable
fun RFIDManagerScreen(
    tags: List<RFIDTag>,
    onStartScan: () -> Unit,
    onTagSelected: (RFIDTag) -> Unit,
    selectedId: String?,
    isScanning: Boolean = false,
    onWrite: (String, Int) -> Unit = { _, _ -> },
    writeStatusMessage: String? = null
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Read Log, 1 = Write Tag

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(16.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Segmented control
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Secondary)
            ) {
                TabButton(
                    text = "READ",
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                TabButton(
                    text = "WRITE",
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
            }

            Button(
                onClick = onStartScan,
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text(
                    if (isScanning) "STOP SCAN" else "START SCAN",
                    fontFamily = FontFamily.Monospace,
                    color = PrimaryForeground,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxSize()) {
            // Left panel - Status + Radar + Stats
            Column(
                modifier = Modifier
                    .weight(0.55f)
                    .fillMaxHeight()
                    .padding(end = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // System Status
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Card)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "RFID SYSTEM STATUS",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MutedForeground
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = Foreground
                            )
                            Spacer(Modifier.width(12.dp))
                            val statusText = if (isScanning) "SCANNING" else "STANDBY"
                            val statusColor = if (isScanning) Accent else Primary
                            Text(
                                statusText,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = statusColor,
                                modifier = Modifier
                                    .background(statusColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Selected indicator - always visible at top when a tag is chosen (tap row in right list)
                if (selectedId != null) {
                    val sel = tags.firstOrNull { it.id == selectedId }
                    if (sel != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Card)
                        ) {
                            Column(Modifier.padding(8.dp)) {
                                Text("SELECTED (tap in right list to change)", fontSize = 8.sp, color = MutedForeground, fontFamily = FontFamily.Monospace)
                                Text(sel.uid, fontSize = 11.sp, color = Primary, fontFamily = FontFamily.Monospace)
                                if (sel.dataPreview.isNotEmpty()) {
                                    Text("Data: " + sel.dataPreview, fontSize = 9.sp, color = Accent, fontFamily = FontFamily.Monospace)
                                }
                                // Short lock summary for quick view (full details in the detailed card below)
                                if (sel.fullSectors.containsKey(2) && (sel.type.contains("ULTRALIGHT", ignoreCase = true) || sel.type.contains("NTAG", ignoreCase = true))) {
                                    val hex = sel.fullSectors[2]!!
                                    val bytes = hex.split(" ").mapNotNull { it.toIntOrNull(16)?.toByte() }
                                    if (bytes.size >= 2) {
                                        val lb0 = bytes[0].toInt() and 0xFF
                                        val locked = (3..10).filter { p -> (lb0 and (1 shl (p - 3))) != 0 }
                                        if (locked.isNotEmpty()) {
                                            Text("Lock bits: pages ${locked.joinToString(",")} locked", fontSize = 8.sp, color = Accent, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }

                // Detailed SELECTED TAG MEMORY (larger fonts, full hex list) - placed early for visibility, left panel is scrollable
                /**
                 * **SELECTED TAG MEMORY (UI-STATE-SELECTED / Architecture UI + Design Figma)**
                 *
                 * Visar `fullSectors` från vald tagg (via selectedId).
                 * För Ultralight: "Page 2:", "Page 12:" etc. + explicit LB0 lock parser (samma logik som i Write defaultAddr).
                 * "Lock bits: pages X locked" i Accent (#F59E0B) = varning + guide för säker target page.
                 *
                 * Placerad tidigt (före radar) + större fonter (11/13/10/9sp) per användarfeedback 2026-06-03.
                 * Hela vänsterpanelen .verticalScroll(rememberScrollState()) så P12+ alltid nås.
                 *
                 * Förankring: Motsvarar Figma vänster "SELECTED" + "Memory pages (hex)".
                 * Data kommer från AndroidNfcManager.handleTag (page 2 alltid läst) → domain → toUiTag → hit.
                 */
                val selectedTag = tags.firstOrNull { it.id == selectedId }
                if (selectedTag != null) {
                    Spacer(Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Card)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("SELECTED TAG MEMORY", fontSize: 11.sp, color = MutedForeground, fontFamily = FontFamily.Monospace)
                            Text(selectedTag.uid, fontSize: 13.sp, color = Foreground, fontFamily = FontFamily.Monospace)
                            Spacer(Modifier.height(6.dp))
                            if (selectedTag.fullSectors.isEmpty()) {
                                Text("No page/sector data read yet (hold tag steady during scan)", fontSize: 9.sp, color = MutedForeground, fontFamily = FontFamily.Monospace)
                            } else {
                                val isUltra = selectedTag.type.contains("ULTRALIGHT", ignoreCase = true) || selectedTag.type.contains("NTAG", ignoreCase = true)
                                val labelPrefix = if (isUltra) "Page" else "Sector"
                                selectedTag.fullSectors.forEach { (addr, hex) ->
                                    Text("$labelPrefix $addr:", fontSize: 10.sp, color = Primary, fontFamily = FontFamily.Monospace)
                                    Text(hex, fontSize: 10.sp, color = Foreground, fontFamily = FontFamily.Monospace)
                                    if (isUltra && addr == 2) {
                                        val bytes = hex.split(" ").mapNotNull { it.toIntOrNull(16)?.toByte() }
                                        if (bytes.size >= 2) {
                                            val lb0 = bytes[0].toInt() and 0xFF
                                            val locked = (3..10).filter { p -> (lb0 and (1 shl (p - 3))) != 0 }
                                            if (locked.isNotEmpty()) {
                                                Text("  (Lock bits: pages ${locked.joinToString(",")} locked)", fontSize: 9.sp, color = Accent, fontFamily = FontFamily.Monospace)
                                            } else {
                                                Text("  (No basic locks in LB0)", fontSize: 9.sp, color = MutedForeground, fontFamily = FontFamily.Monospace)
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                }
                            }
                            Text("Full data also in logcat (AndroidNfcManager)", fontSize: 9.sp, color = MutedForeground, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                // Radar
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    colors = CardDefaults.cardColors(containerColor = Card)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        RadarView(modifier = Modifier.size(160.dp))
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Stats grid
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("TOTAL READS", tags.size.toString(), Modifier.weight(1f))
                    StatCard("READS / MIN", "0", Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("STRONG SIGNAL", tags.count { it.rssi > -60 }.toString(), Modifier.weight(1f))
                    StatCard("WEAK SIGNAL", tags.count { it.rssi <= -75 }.toString(), Modifier.weight(1f))
                }

                Spacer(Modifier.height(12.dp))

                // Last Detected
                if (tags.isNotEmpty()) {
                    val last = tags.maxByOrNull { it.readAt }
                    last?.let {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Card)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text("LAST DETECTED", fontSize = 10.sp, color = MutedForeground, fontFamily = FontFamily.Monospace)
                                Text(it.uid, fontSize = 14.sp, color = Foreground, fontFamily = FontFamily.Monospace)
                                if (it.dataPreview.isNotEmpty()) {
                                    Text(it.dataPreview, fontSize = 11.sp, color = Accent, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }

            // Right panel - Tabs + Content
            Column(
                modifier = Modifier
                    .weight(0.45f)
                    .fillMaxHeight()
                    .padding(start = 12.dp)
            ) {
                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Card,
                    contentColor = Primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("READ LOG", fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("WRITE TAG", fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
                    )
                }

                Spacer(Modifier.height(8.dp))

                if (selectedTab == 0) {
                    // READ LOG
                    Column {
                        Text(
                            "READ LOG",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MutedForeground,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        RFIDTagList(
                            tags = tags,
                            selectedId = selectedId,
                            onTagSelected = onTagSelected,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    // WRITE TAG - now wired to real NFC write (uses last detected tag)
                    /**
                     * **WRITE TAG flik (Design: Figma WRITE TAG + Architecture UI onWrite arming)**
                     *
                     * 1. Välj tagg i höger READ LOG (sätter selectedId).
                     * 2. Default targetAddr beräknas här från fullSectors[2] lock bits (LB0 parser) → hoppar över låsta pages.
                     * 3. onWrite(text, addr) armar pending i MainActivity + visar status "Hold the selected tag... NOW".
                     * 4. Användaren håller taggen → nästa detection (handleTag → lastTag fresh) exekverar writen via NfcManager.
                     *
                     * Detta är det "rika" flödet som gjorde page 12 write ("74 65 73 74") möjligt 2026-06-03.
                     * Instruktionstexten längst ner förklarar stegen (Kotlin-novis + användare).
                     */
                    val selectedTagForWrite = tags.firstOrNull { it.id == selectedId }
                    val isUltraWrite = selectedTagForWrite?.type?.contains("ULTRALIGHT", ignoreCase = true) == true ||
                                       selectedTagForWrite?.type?.contains("NTAG", ignoreCase = true) == true

                    // Suggest a writable page: prefer first user page that is not obviously locked.
                    // If we have lock info in page 2, use the parser logic to skip locked ones.
                    val defaultAddr = if (selectedTagForWrite != null && selectedTagForWrite.fullSectors.isNotEmpty()) {
                        val lockHex = if (isUltraWrite) selectedTagForWrite.fullSectors[2] else null
                        val lockedPages = if (lockHex != null) {
                            val bytes = lockHex.split(" ").mapNotNull { it.toIntOrNull(16) }
                            if (bytes.isNotEmpty()) {
                                val lb0 = bytes[0] and 0xFF
                                (3..10).filter { p -> (lb0 and (1 shl (p - 3))) != 0 }.toSet()
                            } else emptySet()
                        } else emptySet()

                        // Find smallest addr >=4 that has data and is not locked (for Ultra)
                        selectedTagForWrite.fullSectors.keys
                            .filter { it >= 4 && (it !in lockedPages || !isUltraWrite) }
                            .minOrNull() ?: 4
                    } else 4

                    var localFeedback by remember { mutableStateOf<String?>(null) }
                    val writeWithFeedback: (String, Int) -> Unit = { text, addr ->
                        onWrite(text, addr)
                        localFeedback = "Write to addr $addr armed. Present the selected tag to execute."
                    }
                    WriteTagForm(
                        onWrite = writeWithFeedback,
                        statusMessage = writeStatusMessage ?: localFeedback,
                        defaultTarget = defaultAddr.toString(),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun TabButton(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) Primary else Color.Transparent
    val fg = if (selected) PrimaryForeground else Foreground

    Box(
        modifier = Modifier
            .clickable { onClick() }
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text,
            color = fg,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Card)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 9.sp, color = MutedForeground, fontFamily = FontFamily.Monospace)
            Text(value, fontSize = 20.sp, color = Primary, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun RadarView(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarPhase"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val maxRadius = size.minDimension / 2

        // Rings
        for (i in 1..3) {
            val radius = maxRadius * (i / 3f)
            drawCircle(
                color = Primary.copy(alpha = 0.15f),
                radius = radius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        // Rotating sweep
        val sweepAngle = phase * 360f
        drawArc(
            color = Primary.copy(alpha = 0.35f),
            startAngle = sweepAngle - 90f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = Offset(center.x - maxRadius, center.y - maxRadius),
            size = androidx.compose.ui.geometry.Size(maxRadius * 2, maxRadius * 2)
        )

        // Center dot
        drawCircle(color = Primary, radius = 4.dp.toPx(), center = center)
    }
}

/**
 * # WRITE TAG Form (UI Component, inlined in RFIDManagerScreen)
 *
 * Formulär för "armed write".
 * - Textfält för data (4 byte för Ultra → UTF8.copyOf(4), 16 för Classic).
 * - Target page/block (användaren kan ändra; default från lock parser i föräldern).
 * - Knapp armar via onWrite-callback → MainActivity.pendingWrite + "Hold NOW" status (Accent).
 * - Instruktioner längst ner förankrar hela flödet (select → arm → hold → verify via SELECTED eller re-scan).
 *
 * **Architecture:** Ren presentational; all logik (pending, match UID, actual NfcManager.write, patch av list-hex) i MainActivity.
 * **Design:** Monospace, Primary/Accent, tight padding. Motsvarar Figma "WRITE TAG" + "Target page..." + status.
 *
 * Se användarens verifiering: page 12 ändrad till 74 65 73 74, syns i vänster SELECTED efter patch/re-detect.
 */
@Composable
fun WriteTagForm(
    onWrite: (String, Int) -> Unit,
    statusMessage: String? = null,
    defaultTarget: String = "4",
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    var targetAddr by remember { mutableStateOf(defaultTarget) }

    Column(modifier = modifier.padding(16.dp)) {
        Text("WRITE TAG", fontFamily = FontFamily.Monospace, color = MutedForeground, fontSize = 11.sp)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Data to write (text)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = targetAddr,
            onValueChange = { targetAddr = it.filter { c -> c.isDigit() } },
            label = { Text("Target page (Ultralight) / block (Classic)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                val addr = targetAddr.toIntOrNull() ?: 4
                if (text.isNotBlank()) onWrite(text, addr)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = text.isNotBlank()
        ) {
            Text("WRITE TO TAG", fontFamily = FontFamily.Monospace)
        }

        if (statusMessage != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                statusMessage,
                fontSize = 10.sp,
                color = Accent,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "1. Select tag in READ LOG. 2. Use Target page field (see lock bits in left panel for which pages are locked). 3. Enter text. 4. Press WRITE TO TAG (arms it + shows message). 5. Immediately hold tag to phone. Next detection executes the write. Check logcat for result.",
            fontSize = 10.sp,
            color = MutedForeground,
            fontFamily = FontFamily.Monospace
        )
    }
}
