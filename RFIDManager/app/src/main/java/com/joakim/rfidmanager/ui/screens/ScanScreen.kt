package com.joakim.rfidmanager.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joakim.rfidmanager.FasadBindStatus
import com.joakim.rfidmanager.nfc.FasadNfcBridge
import com.joakim.rfidmanager.nfc.LastRead
import com.joakim.rfidmanager.nfc.TagLocks
import com.joakim.rfidmanager.nfc.WriteMode
import com.joakim.rfidmanager.nfc.WriteModeState
import com.joakim.rfidmanager.nfc.WriteResult
import com.joakim.rfidmanager.ui.str
import com.joakim.rfidmanager.ui.theme.Dimens
import com.joakim.rfidmanager.ui.theme.rememberOutboxPalette
import com.joakim.rfidmanager.ui.theme.toComposeColor
import kotlin.math.cos
import kotlin.math.sin

/**
 * ScanScreen — live scanning view with radar visualization, Fasad NFC bridge banner, detected tags and persist flow.
 */
@Composable
fun ScanScreen(
    scanningEnabled: Boolean = false,
    onToggleScan: () -> Unit = {},
    detectedTags: List<com.joakim.rfidmanager.ui.model.RFIDTag> = emptyList(),
    onTagSelected: (String) -> Unit = {},
    selectedTagUid: String? = null,
    onWrite: (String, Int, String) -> Unit = { _, _, _ -> },
    onPersist: (com.joakim.rfidmanager.ui.model.RFIDTag) -> Unit = {},
    persistedUids: Set<String> = emptySet(),
    writeState: WriteModeState = WriteModeState.Idle,
    lastRead: LastRead? = null,
    nowMs: Long = System.currentTimeMillis(),
    onEnterWrite: (String) -> Unit = {},
    onCancelWrite: () -> Unit = {},
    fasadTargetUri: String? = null,
    fasadBindStatus: FasadBindStatus? = null,
    fasadErrorMessage: String? = null,
    onArmFasadWrite: () -> Unit = {},
    onOpenInFasad: (String) -> Unit = {},
    onDismissFasadMessage: () -> Unit = {},
    fontSizeScale: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    val palette = rememberOutboxPalette()

    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val sweepAngleDeg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = Dimens.screenHorizontalPadding, vertical = Dimens.sectionSpacing)
    ) {
        Text(
            str("screen.scan.title"),
            fontFamily = FontFamily.Monospace,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(Dimens.smallGap))

        // Fasad taggbindningskort / statuskort (när anropad via fasad.rfid.WRITE_TAG eller vid fel/kvittens)
        if (fasadTargetUri != null || fasadBindStatus != null || fasadErrorMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = palette.cardBackground.toComposeColor()),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(Dimens.cardPadding)) {
                    if (fasadTargetUri != null) {
                        val target = remember(fasadTargetUri) { FasadNfcBridge.parseFasadUri(fasadTargetUri) }
                        val armedFasad = (writeState as? WriteModeState.Armed)?.takeIf { it.request.fasadUri != null }
                        val secsLeft = WriteMode.remainingSeconds(writeState, nowMs)
                        Text(
                            "${str("screen.scan.fasad_bind_title")}: ${target?.summaryLabel() ?: fasadTargetUri}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = (12 * fontSizeScale).sp,
                            color = palette.text.toComposeColor()
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            fasadTargetUri,
                            fontFamily = FontFamily.Monospace,
                            fontSize = (10 * fontSizeScale).sp,
                            color = palette.secondaryText.toComposeColor(),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        if (armedFasad != null) {
                            Text(
                                "${str("screen.scan.fasad_bind_hold")} ($secsLeft s)",
                                fontFamily = FontFamily.Monospace,
                                fontSize = (11 * fontSizeScale).sp,
                                color = palette.pending.toComposeColor()
                            )
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            )
                        } else {
                            Button(
                                onClick = onArmFasadWrite,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    str("screen.scan.fasad_bind_arm"),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = (11 * fontSizeScale).sp
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = onCancelWrite,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Text(str("screen.scan.write_cancel"), fontFamily = FontFamily.Monospace)
                        }
                    }

                    if (fasadBindStatus != null) {
                        val statusColor = if (fasadBindStatus.success) palette.sent.toComposeColor() else palette.errorText.toComposeColor()
                        val statusText = if (fasadBindStatus.success) {
                            str("screen.scan.fasad_bind_done").replace("{uid}", fasadBindStatus.uidHex)
                        } else {
                            str("screen.scan.fasad_bind_failed")
                        }
                        Text(
                            statusText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = (11 * fontSizeScale).sp,
                            color = statusColor
                        )
                        Text(
                            fasadBindStatus.uri,
                            fontFamily = FontFamily.Monospace,
                            fontSize = (10 * fontSizeScale).sp,
                            color = palette.text.toComposeColor()
                        )
                        if (fasadTargetUri == null) {
                            TextButton(onClick = onDismissFasadMessage) {
                                Text(str("screen.scan.write_dismiss"), fontFamily = FontFamily.Monospace, color = palette.text.toComposeColor())
                            }
                        }
                    }

                    if (fasadErrorMessage != null) {
                        Text(
                            fasadErrorMessage,
                            fontFamily = FontFamily.Monospace,
                            fontSize = (11 * fontSizeScale).sp,
                            color = palette.errorText.toComposeColor()
                        )
                        TextButton(onClick = onDismissFasadMessage) {
                            Text(str("screen.scan.write_dismiss"), fontFamily = FontFamily.Monospace, color = palette.text.toComposeColor())
                        }
                    }
                }
            }
            Spacer(Modifier.height(Dimens.smallGap))
        }

        // Radar area
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = Dimens.radarMaxHeight),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Dimens.cardPadding)
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val c = center
                    val rMax = size.minDimension / 2f * 0.88f
                    val ringColor = onSurfaceVariantColor.copy(alpha = 0.28f)

                    for (i in 1..4) {
                        val rad = rMax * i / 4f
                        drawCircle(color = ringColor, radius = rad, center = c, style = Stroke(width = 1.2.dp.toPx()))
                    }
                    drawCircle(color = primaryColor, radius = 2.5.dp.toPx(), center = c)

                    val show = detectedTags.take(6)
                    data class TagVisual(val angleDeg: Float, val dist: Float, val pos: Offset)
                    val tagVisuals = show.mapIndexed { idx, tag ->
                        val seed = tag.id.hashCode() + idx * 23
                        val angleDeg = (seed % 360).toFloat()
                        val dist = rMax * (0.32f + ((seed % 4) * 0.14f))
                        val angRad = Math.toRadians(angleDeg.toDouble()).toFloat()
                        TagVisual(angleDeg, dist, Offset(c.x + cos(angRad) * dist, c.y + sin(angRad) * dist))
                    }

                    if (scanningEnabled) {
                        tagVisuals.forEach { tv ->
                            val diff = (sweepAngleDeg - tv.angleDeg).mod(360f)
                            if (diff in 2f..<72f) {
                                val alpha = 0.4f * (1f - diff / 72f)
                                drawArc(
                                    color = primaryColor.copy(alpha = alpha),
                                    startAngle = tv.angleDeg,
                                    sweepAngle = diff,
                                    useCenter = false,
                                    style = Stroke(width = 4.dp.toPx()),
                                    topLeft = Offset(c.x - tv.dist, c.y - tv.dist),
                                    size = androidx.compose.ui.geometry.Size(tv.dist * 2, tv.dist * 2)
                                )
                            }
                        }
                    }

                    tagVisuals.forEach { tv ->
                        drawCircle(color = primaryColor, radius = 3.5.dp.toPx(), center = tv.pos)
                    }

                    val sweepRad = if (scanningEnabled) Math.toRadians(sweepAngleDeg.toDouble()).toFloat() else 0.7f
                    val sx = c.x + cos(sweepRad) * rMax * 0.92f
                    val sy = c.y + sin(sweepRad) * rMax * 0.92f
                    drawLine(
                        color = primaryColor.copy(alpha = if (scanningEnabled) 0.65f else 0.35f),
                        start = c,
                        end = Offset(sx, sy),
                        strokeWidth = 2.dp.toPx()
                    )

                    if (scanningEnabled) {
                        val trailR = rMax * 0.92f
                        val n = 72
                        for (i in 0 until n) {
                            val fraction = i.toFloat() / n
                            val alpha = 0.25f + 0.40f * fraction
                            val sliceDeg = 72f / n
                            drawArc(
                                color = primaryColor.copy(alpha = alpha),
                                startAngle = sweepAngleDeg - 72f + i * sliceDeg,
                                sweepAngle = sliceDeg,
                                useCenter = true,
                                style = Fill,
                                topLeft = Offset(c.x - trailR, c.y - trailR),
                                size = androidx.compose.ui.geometry.Size(trailR * 2, trailR * 2)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (scanningEnabled) str("screen.scan.radar_text_live") else str("screen.scan.radar_text_static"),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = if (scanningEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(Dimens.sectionSpacing))

        Button(
            onClick = onToggleScan,
            modifier = Modifier.fillMaxWidth().heightIn(min = Dimens.minTouchTarget)
        ) {
            Text(if (scanningEnabled) str("screen.scan.stop") else str("screen.scan.start"), fontFamily = FontFamily.Monospace)
        }

        if (scanningEnabled) {
            Spacer(Modifier.height(Dimens.smallGap))
            Text(
                str("screen.scan.scanning_active"),
                color = MaterialTheme.colorScheme.primary,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        Spacer(Modifier.height(Dimens.smallGap))

        Text(str("screen.scan.detected"), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Dimens.smallGap))

        if (detectedTags.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (scanningEnabled) Icons.Default.Nfc else Icons.Default.SearchOff,
                        contentDescription = null,
                        tint = if (scanningEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(Dimens.smallGap))
                    Text(
                        text = if (scanningEnabled) str("screen.scan.listening") else str("screen.scan.no_tags"),
                        color = if (scanningEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (scanningEnabled) str("screen.scan.hold_tag")
                        else str("screen.scan.start_scan_hold"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
            if (scanningEnabled) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(Dimens.listItemSpacing)
            ) {
                items(detectedTags.toList(), key = { it.id }) { tag ->
                    val isSelected = selectedTagUid == tag.uid
                    var writeAddress by remember { mutableStateOf("") }
                    var writeData by remember { mutableStateOf("") }
                    val detectedFasadUri = remember(tag.fullSectors) {
                        FasadNfcBridge.decodeFasadUriFromHexSectors(tag.fullSectors)
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTagSelected(tag.id) }
                            .padding(horizontal = 4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
                        colors = if (isSelected) CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        ) else CardDefaults.cardColors()
                    ) {
                        Column(modifier = Modifier.padding(Dimens.cardPadding)) {
                            Text(tag.uid, fontFamily = FontFamily.Monospace, fontSize = (9 * fontSizeScale).sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${str("screen.scan.type")}: ${tag.type}", fontSize = (12 * fontSizeScale).sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(4.dp))

                            // --- Fasad NFC-brygga: Läge 1 (inskriven fasad://besiktning-URI) eller Läge 2 (endast UID) ---
                            if (detectedFasadUri != null) {
                                val parsedTarget = remember(detectedFasadUri) { FasadNfcBridge.parseFasadUri(detectedFasadUri) }
                                val uriWithUid = remember(detectedFasadUri, tag.uid) {
                                    FasadNfcBridge.appendUidToFasadUri(detectedFasadUri, tag.uid)
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(palette.cardBackground.toComposeColor())
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        "${str("screen.scan.fasad_tag_label")}: ${parsedTarget?.summaryLabel() ?: detectedFasadUri}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = (11 * fontSizeScale).sp,
                                        color = palette.sent.toComposeColor()
                                    )
                                    Text(
                                        detectedFasadUri,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = (9 * fontSizeScale).sp,
                                        color = palette.text.toComposeColor(),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Button(
                                    onClick = { onOpenInFasad(uriWithUid) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        str("screen.scan.fasad_open"),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = (12 * fontSizeScale).sp
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                            } else {
                                OutlinedButton(
                                    onClick = { onOpenInFasad(FasadNfcBridge.buildUidOnlyUri(tag.uid)) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        str("screen.scan.fasad_send_uid"),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = (11 * fontSizeScale).sp
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                            }

                            val alreadyPersisted = tag.uid in persistedUids
                            Button(
                                onClick = {
                                    onTagSelected(tag.id)
                                    onPersist(tag)
                                },
                                enabled = !alreadyPersisted,
                                colors = if (alreadyPersisted) ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    contentColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                ) else ButtonDefaults.buttonColors()
                            ) {
                                Text(
                                    if (alreadyPersisted) str("screen.scan.persisted") else "${str("screen.scan.persist_hint")}${tag.uid.takeLast(6)}]",
                                    fontSize = (12 * fontSizeScale).sp
                                )
                            }

                            // --- Skrivläge: bara efter tryck på "Skriv till tagg" (se nfc/WriteMode.kt) ---
                            val editing = (writeState as? WriteModeState.Editing)?.takeIf { it.uidHex == tag.uid }
                            val armed = (writeState as? WriteModeState.Armed)?.takeIf { it.request.uidHex == tag.uid }
                            val finished = (writeState as? WriteModeState.Finished)?.takeIf { it.uidHex == tag.uid }
                            val isLastRead = lastRead?.uidHex == tag.uid

                            if (editing == null && armed == null) {
                                Spacer(Modifier.height(4.dp))
                                OutlinedButton(
                                    onClick = { onEnterWrite(tag.uid) },
                                    enabled = WriteMode.canStartWrite(writeState, lastRead, tag.uid, nowMs),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(str("screen.scan.write_button"), fontFamily = FontFamily.Monospace, fontSize = (12 * fontSizeScale).sp)
                                }
                                if (isLastRead && lastRead?.writable == false) {
                                    Text(str("screen.scan.write_not_writable"), fontSize = (10 * fontSizeScale).sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.error)
                                }
                            }

                            if (finished != null) {
                                Spacer(Modifier.height(4.dp))
                                val ok = finished.result == WriteResult.WRITTEN
                                Text(
                                    when (finished.result) {
                                        WriteResult.WRITTEN -> str("screen.scan.write_done").replace("{addr}", finished.addr.toString())
                                        WriteResult.FAILED -> str("screen.scan.write_failed")
                                        WriteResult.TIMEOUT -> str("screen.scan.write_timeout")
                                    },
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = (11 * fontSizeScale).sp,
                                    color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                                TextButton(onClick = onCancelWrite) { Text(str("screen.scan.write_dismiss"), fontFamily = FontFamily.Monospace) }
                            }

                            if (armed != null) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    str("screen.scan.write_armed") + " (" + WriteMode.remainingSeconds(writeState, nowMs) + " s)",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = (12 * fontSizeScale).sp,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                                OutlinedButton(onClick = onCancelWrite, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                                    Text(str("screen.scan.write_cancel"), fontFamily = FontFamily.Monospace)
                                }
                            }

                            if (editing != null) {
                                val lockedPages = remember(tag.fullSectors) { TagLocks.parseLockedPages(tag.fullSectors) }
                                val addrInt = writeAddress.toIntOrNull()
                                val addrLocked = addrInt?.let { TagLocks.isPageLocked(it, lockedPages) } ?: false
                                val addrColor = if (addrInt == null) MaterialTheme.colorScheme.onSurfaceVariant
                                    else if (addrLocked) Color(0xFFEF4444) else Color(0xFF22C55E)

                                Spacer(Modifier.height(8.dp))
                                Text(str("screen.scan.write_title"), fontFamily = FontFamily.Monospace, fontSize = (12 * fontSizeScale).sp, color = MaterialTheme.colorScheme.tertiary)
                                Spacer(Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = writeAddress,
                                    onValueChange = { writeAddress = it },
                                    label = { Text(str("screen.scan.write_address"), fontFamily = FontFamily.Monospace, fontSize = (10 * fontSizeScale).sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = (12 * fontSizeScale).sp, color = addrColor),
                                    supportingText = if (addrInt != null) {
                                        {
                                            Text(
                                                if (addrLocked) "🔒 Sidan är låst" else "✓ Sidan är skrivbar",
                                                fontSize = (9 * fontSizeScale).sp,
                                                color = addrColor
                                            )
                                        }
                                    } else null,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = addrColor,
                                        unfocusedBorderColor = addrColor.copy(alpha = 0.5f)
                                    )
                                )
                                Spacer(Modifier.height(2.dp))
                                MemoryMapSection(
                                    fullSectors = tag.fullSectors,
                                    currentAddress = writeAddress,
                                    onAddressClick = { writeAddress = it }
                                )
                                Spacer(Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = writeData,
                                    onValueChange = { writeData = it },
                                    label = { Text(str("screen.scan.write_data"), fontFamily = FontFamily.Monospace, fontSize = (10 * fontSizeScale).sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = (12 * fontSizeScale).sp)
                                )
                                Spacer(Modifier.height(4.dp))
                                Button(
                                    onClick = {
                                        val addr = writeAddress.toIntOrNull() ?: 0
                                        onWrite(writeData, addr, tag.uid)
                                        writeAddress = ""
                                        writeData = ""
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (addrLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                                        contentColor = if (addrLocked) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onTertiary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        if (addrLocked) "⚠ Låst adress" else str("screen.scan.write_save"),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = (12 * fontSizeScale).sp
                                    )
                                }
                                OutlinedButton(onClick = onCancelWrite, modifier = Modifier.fillMaxWidth()) {
                                    Text(str("screen.scan.write_cancel"), fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(Dimens.sectionSpacing))
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun MemoryMapSection(
    fullSectors: Map<Int, String>,
    currentAddress: String,
    onAddressClick: (String) -> Unit
) {
    val lockedPages = remember(fullSectors) { TagLocks.parseLockedPages(fullSectors) }
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        TextButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Text(
                if (expanded) "▲ Minne (klicka för att dölja)" else "▼ Minne (klicka för att visa)",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (expanded) {
            for (block in 0..7) {
                val start = 4 + block * 4
                val end = start + 3
                val blockLocked = (start..end).any { it in lockedPages }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAddressClick(start.toString()) }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                if (blockLocked) Color(0xFFEF4444) else Color(0xFF22C55E),
                                CircleShape
                            )
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "p$start\u2013p$end",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (blockLocked) " l\u00E5st" else " skrivbar",
                        fontSize = 10.sp,
                        color = if (blockLocked) Color(0xFFEF4444) else Color(0xFF22C55E)
                    )
                }
            }
        }
    }
}
