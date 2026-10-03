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
import com.joakim.rfidmanager.outbox.core.OutboxStatus
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
                    text = outboxStatusText(reading),
                    fontSize = (9 * fontSizeScale).sp,
                    fontFamily = FontFamily.Monospace,
                    color = outboxStatusColor(reading)
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
                        text = outboxStatusText(reading),
                        fontSize = (9 * fontSizeScale).sp,
                        fontFamily = FontFamily.Monospace,
                        color = outboxStatusColor(reading)
                    )
                }
            }

            // Felorsak för misslyckade/väntande poster som redan försökts
            if (reading.outboxStatus != OutboxStatus.SENT && !reading.lastError.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${str("outbox.last_error")}: ${reading.lastError}",
                    fontSize = (9 * fontSizeScale).sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height((12 * fontSizeScale).dp))

            Button(
                onClick = onTransmit,
                enabled = reading.outboxStatus != OutboxStatus.SENT,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    if (reading.outboxStatus == OutboxStatus.SENT) str("persisted_item.sent_check") else str("persisted_item.send_now"),
                    fontSize = (11 * fontSizeScale).sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

/** Väntar / Skickad / Misslyckad (+ antal försök när det finns). */
@Composable
internal fun outboxStatusText(reading: PersistedReading): String {
    val base = when (reading.outboxStatus) {
        OutboxStatus.PENDING -> str("outbox.status.pending")
        OutboxStatus.SENT -> str("outbox.status.sent")
        OutboxStatus.FAILED -> str("outbox.status.failed")
    }
    return if (reading.attempts > 0 && reading.outboxStatus != OutboxStatus.SENT) {
        "$base · ${str("outbox.attempts")} ${reading.attempts}"
    } else base
}

@Composable
internal fun outboxStatusColor(reading: PersistedReading) = when (reading.outboxStatus) {
    OutboxStatus.SENT -> MaterialTheme.colorScheme.primary
    OutboxStatus.FAILED -> MaterialTheme.colorScheme.error
    OutboxStatus.PENDING -> MaterialTheme.colorScheme.secondary
}
