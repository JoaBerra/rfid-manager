package com.joakim.rfidmanager.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joakim.rfidmanager.domain.model.PersistedReading
import com.joakim.rfidmanager.ui.str
import com.joakim.rfidmanager.ui.theme.RFIDManagerTheme
import java.text.SimpleDateFormat
import java.util.*

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = reading.uidOrCode,
                    fontSize = (12 * fontSizeScale).sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = timeStr,
                    fontSize = (9 * fontSizeScale).sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = reading.source ?: str("common.unknown_source"),
                fontSize = (11 * fontSizeScale).sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = reading.dataPreview ?: "",
                    fontSize = (10 * fontSizeScale).sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (reading.transmitted) str("persisted_item.transmitted") else str("persisted_item.persisted"),
                    fontSize = (9 * fontSizeScale).sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (reading.transmitted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

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
