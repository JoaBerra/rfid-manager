package com.joakim.rfidmanager.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.foundation.shape.CircleShape
import com.joakim.rfidmanager.ui.theme.Dimens
import com.joakim.rfidmanager.ui.str
import kotlin.math.cos
import kotlin.math.sin

/**
 * ScanScreen — live scanning view with radar visualization, detected tags and persist flow.
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
    fontSizeScale: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    // Radar sweep animation
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
        // Header
        Text(
            str("screen.scan.title"),
            fontFamily = FontFamily.Monospace,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(Dimens.smallGap))

        // Radar area – capped height for breathing room (35-40 % rule)
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

                    // Concentric radar rings
                    for (i in 1..4) {
                        val rad = rMax * i / 4f
                        drawCircle(color = ringColor, radius = rad, center = c, style = Stroke(width = 1.2.dp.toPx()))
                    }
                    // Center
                    drawCircle(color = primaryColor, radius = 2.5.dp.toPx(), center = c)

                    // Tag data — computed once per frame
                    val show = detectedTags.take(6)
                    data class TagVisual(val angleDeg: Float, val dist: Float, val pos: Offset)
                    val tagVisuals = show.mapIndexed { idx, tag ->
                        val seed = tag.id.hashCode() + idx * 23
                        val angleDeg = (seed % 360).toFloat()
                        val dist = rMax * (0.32f + ((seed % 4) * 0.14f))
                        val angRad = Math.toRadians(angleDeg.toDouble()).toFloat()
                        TagVisual(angleDeg, dist, Offset(c.x + cos(angRad) * dist, c.y + sin(angRad) * dist))
                    }

                    // Tag tails — arc som växer från punkten framåt i svepriktning (endast vid aktiv skanning)
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

                    // Tag blips (ritas ovanpå svansarna)
                    tagVisuals.forEach { tv ->
                        drawCircle(color = primaryColor, radius = 3.5.dp.toPx(), center = tv.pos)
                    }

                    // Animated sweep line
                    val sweepRad = if (scanningEnabled) Math.toRadians(sweepAngleDeg.toDouble()).toFloat() else 0.7f
                    val sx = c.x + cos(sweepRad) * rMax * 0.92f
                    val sy = c.y + sin(sweepRad) * rMax * 0.92f
                    drawLine(
                        color = primaryColor.copy(alpha = if (scanningEnabled) 0.65f else 0.35f),
                        start = c,
                        end = Offset(sx, sy),
                        strokeWidth = 2.dp.toPx()
                    )

                    // Trail/efterglöd — fading wedge bakom svep-linjen (endast vid aktiv skanning)
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

                // Overlay label (bottom)
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

        // Scan toggle – generous touch target
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

        // Detected tags list
        Text(str("screen.scan.detected"), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Dimens.smallGap))

        if (detectedTags.isEmpty()) {
            // Empty state with icon
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
            // Loading bar when scanning active
            if (scanningEnabled) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                )
            }
        } else {
            // SnapshotStateList does NOT trigger remember re-evaluation
            // (its equals() is referential identity — same object reference is "equal").
            // So we must NOT wrap in remember; read toList() directly each recomposition.
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(Dimens.listItemSpacing)
            ) {
                items(detectedTags.toList(), key = { it.id }) { tag ->
                    val isSelected = selectedTagUid == tag.uid
                    var writeAddress by remember { mutableStateOf("") }
                    var writeData by remember { mutableStateOf("") }
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

                            if (isSelected) {
                                val lockedPages = remember(tag.fullSectors) { parseLockedPages(tag.fullSectors) }
                                val addrInt = writeAddress.toIntOrNull()
                                val addrLocked = addrInt?.let { isPageLocked(it, lockedPages) } ?: false
                                val addrColor = if (addrInt == null) MaterialTheme.colorScheme.onSurfaceVariant
                                    else if (addrLocked) Color(0xFFEF4444) else Color(0xFF22C55E)

                                Spacer(Modifier.height(8.dp))
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

private fun parseLockedPages(fullSectors: Map<Int, String>): Set<Int> {
    val page2 = fullSectors[2] ?: return emptySet()
    val bytes = page2.split(" ").mapNotNull { it.toIntOrNull(16) }
    if (bytes.size < 2) return emptySet()
    val lb0 = bytes[0]
    val lb1 = bytes[1]
    val locked = mutableSetOf<Int>()
    for (bit in 0..3) {
        if ((lb0 shr bit) and 1 == 1) {
            locked.addAll((4 + bit * 4) until (8 + bit * 4))
        }
    }
    for (bit in 0..3) {
        if ((lb1 shr bit) and 1 == 1) {
            locked.addAll((20 + bit * 4) until (24 + bit * 4))
        }
    }
    return locked
}

private fun isPageLocked(page: Int, lockedPages: Set<Int>): Boolean {
    if (page < 4) return true
    return page in lockedPages
}

@Composable
private fun MemoryMapSection(
    fullSectors: Map<Int, String>,
    currentAddress: String,
    onAddressClick: (String) -> Unit
) {
    val lockedPages = remember(fullSectors) { parseLockedPages(fullSectors) }
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
