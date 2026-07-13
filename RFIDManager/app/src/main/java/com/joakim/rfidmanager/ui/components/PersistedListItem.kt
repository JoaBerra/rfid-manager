package com.joakim.rfidmanager.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joakim.rfidmanager.domain.model.PersistedReading
import com.joakim.rfidmanager.ui.str
import java.text.SimpleDateFormat
import java.util.*

private val LARGE_FONT_THRESHOLD = 1.3f

@Composable
fun PersistedListItem(
    reading: PersistedReading,
    onTransmit: () -> Unit,
    fontSizeScale: Float = 1.0f,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    val timeStr = dateFormat.format(Date(reading.timestamp))
    val isLargeText = fontSizeScale > LARGE_FONT_THRESHOLD

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            if (isLargeText) {
                Text(
                    text = reading.uidOrCode,
                    fontSize = (12 * fontSizeScale).sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = timeStr,
                    fontSize = (9 * fontSizeScale).sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = reading.uidOrCode,
                        fontSize = (12 * fontSizeScale).sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = timeStr,
                        fontSize = (9 * fontSizeScale).sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height((8 * fontSizeScale).dp))

            Text(
                text = reading.source ?: str("common.unknown_source"),
                fontSize = (11 * fontSizeScale).sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height((8 * fontSizeScale).dp))

            if (isLargeText) {
                Text(
                    text = reading.dataPreview ?: "",
                    fontSize = (10 * fontSizeScale).sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (reading.transmitted) str("persisted_item.transmitted") else str("persisted_item.persisted"),
                    fontSize = (9 * fontSizeScale).sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (reading.transmitted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = reading.dataPreview ?: "",
                        fontSize = (10 * fontSizeScale).sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (reading.transmitted) str("persisted_item.transmitted") else str("persisted_item.persisted"),
                        fontSize = (9 * fontSizeScale).sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (reading.transmitted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(Modifier.height((12 * fontSizeScale).dp))

            Button(
                onClick = onTransmit,
                enabled = !reading.transmitted,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    if (reading.transmitted) str("persisted_item.transmitted_check") else str("persisted_item.transmit"),
                    fontSize = (11 * fontSizeScale).sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
