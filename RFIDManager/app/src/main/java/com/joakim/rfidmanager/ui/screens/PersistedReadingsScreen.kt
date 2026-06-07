package com.joakim.rfidmanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joakim.rfidmanager.data.repository.PersistedReadingRepository
import com.joakim.rfidmanager.domain.model.PersistedReading
import com.joakim.rfidmanager.ui.components.PersistedListItem
import com.joakim.rfidmanager.ui.theme.*
import kotlinx.coroutines.launch

/**
 * PersistedReadingsScreen – visar alla lokalt sparade läsningar (RFID + EAN).
 *
 * Följer Figma-Design-Spec-Fas2 + nomenclature.
 * Använder PersistedListItem för varje rad (med alla metadata-fält).
 */
@Composable
fun PersistedReadingsScreen(
    repository: PersistedReadingRepository,
    onBack: () -> Unit,
    onTransmit: (PersistedReading) -> Unit
) {
    val readings by repository.getAllReadings().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var filter by remember { mutableStateOf("All") } // All / RFID / EAN

    val filtered = when (filter) {
        "RFID" -> readings.filter { it.isRfid() }
        "EAN" -> readings.filter { it.isEan() }
        else -> readings
    }

    Column(modifier = Modifier.fillMaxSize().background(Background).padding(16.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("←", fontSize = 20.sp)
            }
            Text("PERSISTED READINGS", fontFamily = FontFamily.Monospace, fontSize = 18.sp, color = Foreground)
            Text("${filtered.size}", fontFamily = FontFamily.Monospace, fontSize = 16.sp, color = Primary)
        }

        Spacer(Modifier.height(12.dp))

        // Filter tabs (enligt spec)
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            listOf("All", "RFID", "EAN").forEach { f ->
                Text(
                    text = f,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = if (filter == f) Primary else MutedForeground,
                    modifier = Modifier
                        .clickable { filter = f }
                        .padding(bottom = 4.dp)
                )
            }
        }

        Divider(color = Border, thickness = 1.dp)

        Spacer(Modifier.height(8.dp))

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No persisted readings yet.\nScan or read an escort memory.", color = MutedForeground, fontFamily = FontFamily.Monospace)
            }
        } else {
            LazyColumn {
                items(filtered, key = { it.id }) { reading ->
                    PersistedListItem(
                        reading = reading,
                        onTransmit = { onTransmit(reading) },
                        onDetails = { /* TODO: detail modal */ }
                    )
                }
            }
        }
    }
}