package com.joakim.rfidmanager.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joakim.rfidmanager.domain.model.PersistedReading
import com.joakim.rfidmanager.ui.theme.*

/**
 * PersistedListItem – atomisk komponent för en rad i PersistedReadingsScreen.
 *
 * Följer exakt:
 * - Nomenclature-Figma-Android (namn + properties)
 * - Figma-Design-Spec-Fas2 (metadata-fält, layout, TypeBadge, status, source etc.)
 * - Referensbilder (fas2-persisted-readings-list.jpg)
 *
 * Metadata-fält som visas (från spec + bilder):
 * - uidOrCode (stor monospace)
 * - timestamp
 * - source (t.ex. "Source: Gate 3 - Warehouse A" eller "Line 7 - Palletizer")
 * - dataPreview
 * - type (RFID/EAN → TypeBadge)
 * - status / transmitted (badge + knapp)
 */
@Composable
fun PersistedListItem(
    reading: PersistedReading,
    onTransmit: () -> Unit,
    onDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Card)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Icon + TypeBadge (från spec)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Enkel ikon-plats (kan utökas med riktig ikon senare)
                Text(
                    text = if (reading.isRfid()) "🔶" else "📊",
                    fontSize = 24.sp
                )
                Spacer(Modifier.height(4.dp))
                TypeBadge(type = reading.type)
            }

            Spacer(Modifier.width(12.dp))

            // Center: Metadata stack (exakt de fält som syns i bilderna)
            Column(modifier = Modifier.weight(1f)) {
                // UID / Code
                Text(
                    text = reading.uidOrCode,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = Foreground
                )

                // Timestamp
                Text(
                    text = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                        .format(java.util.Date(reading.timestamp)),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = MutedForeground
                )

                // Source / Location (viktigt metadata-fält från spec)
                reading.source?.let {
                    Text(
                        text = it,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = MutedForeground
                    )
                }

                // Data preview
                reading.dataPreview?.let {
                    Text(
                        text = it,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Accent
                    )
                }
            }

            // Right: Status + buttons
            Column(horizontalAlignment = Alignment.End) {
                // Status badge
                val statusText = if (reading.transmitted) "⚡ Transmitted via Sparkplug" else "✓ Persisted"
                val statusColor = if (reading.transmitted) Accent else StatusConnected
                Text(
                    text = statusText,
                    fontSize = 9.sp,
                    color = statusColor,
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )

                Spacer(Modifier.height(8.dp))

                // Buttons - made more prominent for usability (was too subtle with pure TextButton on dark card)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Primary action - clearer tap target
                    TextButton(
                        onClick = onTransmit,
                        colors = ButtonDefaults.textButtonColors(contentColor = Primary),
                        modifier = Modifier
                            .background(Primary.copy(alpha = 0.15f), shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Transmit ↑", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Primary)
                    }
                    TextButton(
                        onClick = onDetails,
                        colors = ButtonDefaults.textButtonColors(contentColor = MutedForeground)
                    ) {
                        Text("Details", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeBadge(type: String) {
    val (text, color) = when {
        type.equals("RFID", ignoreCase = true) -> "RFID" to BadgeRFID
        type.equals("EAN", ignoreCase = true) -> "EAN" to BadgeEAN
        else -> type.uppercase() to MutedForeground
    }
    Text(
        text = text,
        fontSize = 9.sp,
        color = androidx.compose.ui.graphics.Color.White,
        modifier = Modifier
            .background(color)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        fontFamily = FontFamily.Monospace
    )
}