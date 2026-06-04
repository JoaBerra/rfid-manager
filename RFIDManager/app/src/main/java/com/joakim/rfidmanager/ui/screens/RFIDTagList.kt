package com.joakim.rfidmanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joakim.rfidmanager.ui.model.RFIDTag
import com.joakim.rfidmanager.ui.theme.*

/**
 * # UI Layer — RFIDTagList (Architecture: UI Component, READ LOG)
 *
 * Visar listan över detekterade taggar (från MainActivity.detectedTags).
 * Klick → onTagSelected(tag) → selectedId i föräldern → vänster SELECTED + WRITE default.
 *
 * **Design-förankring (Figma-to-Compose + 2026-06-01 ux):**
 * - Monospace UID + tid + preview.
 * - Type badge i Primary.
 * - Tre vertikala signalbars (färgkodade Primary/Accent/Muted) — proxy för RSSI.
 * - Grön vänster-accent + bg vid select (Primary.copy(alpha=0.06)).
 * - Empty state "NO TAGS DETECTED" + instruktion.
 * - Header-rad: "Tap a row below to select it for full memory details (incl. lock bytes) and write".
 *
 * **ID:** selectedId + onTagSelected är de centrala som binder READ LOG till SELECTED / armed write.
 * Data kommer från AndroidNfcManager.handleTag (via sectorsRead → dataPreview/fullSectors).
 */
@Composable
fun RFIDTagList(
    tags: List<RFIDTag>,
    selectedId: String?,
    onTagSelected: (RFIDTag) -> Unit,
    modifier: Modifier = Modifier
) {
    if (tags.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Simple text-based "radar" icon for now
                Text(
                    "◉",
                    fontSize = 48.sp,
                    color = MutedForeground.copy(alpha = 0.3f)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "NO TAGS DETECTED",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = MutedForeground,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Bring an RFID tag within range to scan",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = MutedForeground.copy(alpha = 0.55f)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            item {
                Text(
                    "Tap a row below to select it for full memory details (incl. lock bytes) and write",
                    fontSize = 9.sp,
                    color = MutedForeground,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            itemsIndexed(tags) { index, tag ->
                val isSelected = tag.id == selectedId

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSelected) Primary.copy(alpha = 0.06f)
                            else Color.Transparent
                        )
                        .clickable { onTagSelected(tag) }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    // Selected visual: Primary left "border" effect via bg + the list item bg (matches Figma + Architecture "selectedId" state).
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Index + UID
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "%02d".format(index + 1),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = MutedForeground,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = tag.uid,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp,
                                    letterSpacing = 0.8.sp,
                                    color = Foreground
                                )
                            }

                            Spacer(Modifier.height(4.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Type badge
                                Text(
                                    text = tag.type,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = Primary,
                                    modifier = Modifier
                                        .background(Primary.copy(alpha = 0.12f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )

                                // Timestamp
                                Text(
                                    text = tag.readAt,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = MutedForeground
                                )
                            }

                            // Data preview from actual read sectors (for escort memory)
                            if (tag.dataPreview.isNotEmpty()) {
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = tag.dataPreview,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = Accent
                                )
                            }
                        }

                        // Signal strength visualization
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // Three vertical signal bars
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                val strength = when {
                                    tag.rssi > -55 -> 3
                                    tag.rssi > -70 -> 2
                                    else -> 1
                                }

                                repeat(3) { bar ->
                                    val barHeight = when (bar) {
                                        0 -> 4.dp
                                        1 -> 7.dp
                                        else -> 10.dp
                                    }
                                    val barColor = when {
                                        bar < strength -> Primary
                                        bar == 1 && strength >= 2 -> Accent
                                        else -> MutedForeground.copy(alpha = 0.3f)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(barHeight)
                                            .background(barColor)
                                    )
                                }
                            }

                            Text(
                                text = "${tag.rssi} dBm",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = MutedForeground
                            )
                        }
                    }
                }

                if (index < tags.lastIndex) {
                    HorizontalDivider(
                        color = Border,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}
