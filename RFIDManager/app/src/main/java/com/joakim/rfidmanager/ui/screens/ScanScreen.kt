package com.joakim.rfidmanager.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joakim.rfidmanager.ui.theme.Dimens
import com.joakim.rfidmanager.ui.str
import kotlin.math.cos
import kotlin.math.sin

/**
 * ScanScreen – dedikerad vy för live scanning + Radar + översikt (Fas 3).
 *
 * Innehåller radarvisualisering, scan-knapp, detected-taggar och persist-flöde.
 */
@Composable
fun ScanScreen(
    scanningEnabled: Boolean = false,
    onToggleScan: () -> Unit = {},
    detectedTags: List<com.joakim.rfidmanager.ui.model.RFIDTag> = emptyList(),
    onTagSelected: (String) -> Unit = {},
    selectedTagUid: String? = null,
    onWrite: (String, Int) -> Unit = { _, _ -> },
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

                    // Blips for currently detected tags
                    val show = detectedTags.take(6)
                    show.forEachIndexed { idx, tag ->
                        val seed = tag.id.hashCode() + idx * 23
                        val ang = Math.toRadians((seed % 360).toDouble()).toFloat()
                        val dist = rMax * (0.32f + ((seed % 4) * 0.14f))
                        val bx = c.x + cos(ang) * dist
                        val by = c.y + sin(ang) * dist
                        drawCircle(color = primaryColor, radius = 3.5.dp.toPx(), center = Offset(bx, by))
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
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTagSelected(tag.id) }
                            .padding(horizontal = 4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(Dimens.cardPadding)) {
                            Text(tag.uid, fontFamily = FontFamily.Monospace, fontSize = (9 * fontSizeScale).sp)
                            Text("${str("screen.scan.type")}: ${tag.type}", fontSize = (12 * fontSizeScale).sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(Dimens.cardPadding)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, fontFamily = FontFamily.Monospace, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
        }
    }
}
