package com.joakim.rfidmanager.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joakim.rfidmanager.domain.model.PersistedReading
import com.joakim.rfidmanager.outbox.core.OutboxStatus
import com.joakim.rfidmanager.outbox.core.RetryPolicy
import com.joakim.rfidmanager.outbox.core.RoundsConfig
import kotlinx.coroutines.delay
import com.joakim.rfidmanager.ui.str
import java.text.SimpleDateFormat
import java.util.*

private val LARGE_FONT_THRESHOLD = 1.3f

@Composable
fun PersistedListItem(
    reading: PersistedReading,
    onTransmit: () -> Unit,
    fontSizeScale: Float = 1.0f,
    roundsConfig: RoundsConfig = RoundsConfig(),
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    val timeStr = dateFormat.format(Date(reading.timestamp))
    val isLargeText = fontSizeScale > LARGE_FONT_THRESHOLD
    val policy = remember(roundsConfig) { RetryPolicy(roundsConfig) }
    val resumeAt = if (reading.outboxStatus == OutboxStatus.PENDING) {
        policy.nextRoundStartsAt(reading.attempts, reading.lastAttemptAt)
    } else null
    // Klockan tickar bara medan posten står i paus, så att "om N min" hålls aktuell.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(resumeAt) {
        while (resumeAt != null) {
            now = System.currentTimeMillis()
            delay(15_000L)
        }
    }

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
                    text = outboxStatusText(reading, policy),
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
                        text = outboxStatusText(reading, policy),
                        fontSize = (9 * fontSizeScale).sp,
                        fontFamily = FontFamily.Monospace,
                        color = outboxStatusColor(reading)
                    )
                }
            }

            // Paus mellan omgångar: när nästa omgång startar (felorsaken nedan står kvar)
            if (resumeAt != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = pauseText(policy, reading.attempts, resumeAt, now),
                    fontSize = (9 * fontSizeScale).sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
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
internal fun outboxStatusText(reading: PersistedReading, policy: RetryPolicy = RetryPolicy()): String {
    val base = when (reading.outboxStatus) {
        OutboxStatus.PENDING -> str("outbox.status.pending")
        OutboxStatus.SENT -> str("outbox.status.sent")
        OutboxStatus.FAILED -> str("outbox.status.failed")
    }
    val withAttempts = if (reading.attempts > 0 && reading.outboxStatus != OutboxStatus.SENT) {
        "$base · ${str("outbox.attempts")} ${reading.attempts}"
    } else base
    // Omgångsnummer visas bara när flera omgångar är inställda (med en omgång ser texten ut som förut).
    val rounds = policy.config.rounds
    return if (reading.outboxStatus == OutboxStatus.PENDING && reading.attempts > 0 && rounds > 1 && !policy.isPause(reading.attempts)) {
        "$withAttempts · " + str("outbox.round")
            .replace("{n}", policy.currentRound(reading.attempts).toString())
            .replace("{total}", rounds.toString())
    } else withAttempts
}

/** "Paus: omgång 1 av 3 klar. Nästa omgång startar kl 14:35 (om 59 min)" – eller att pausen är slut. */
@Composable
internal fun pauseText(policy: RetryPolicy, attempts: Int, resumeAt: Long, now: Long): String {
    val minutes = RetryPolicy.minutesUntil(resumeAt, now)
    if (minutes <= 0L) return str("outbox.pause_due")
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(resumeAt))
    return str("outbox.pause")
        .replace("{done}", (policy.currentRound(attempts) - 1).toString())
        .replace("{total}", policy.config.rounds.toString())
        .replace("{time}", time)
        .replace("{min}", minutes.toString())
}

@Composable
internal fun outboxStatusColor(reading: PersistedReading) = when (reading.outboxStatus) {
    OutboxStatus.SENT -> MaterialTheme.colorScheme.primary
    OutboxStatus.FAILED -> MaterialTheme.colorScheme.error
    OutboxStatus.PENDING -> MaterialTheme.colorScheme.secondary
}
