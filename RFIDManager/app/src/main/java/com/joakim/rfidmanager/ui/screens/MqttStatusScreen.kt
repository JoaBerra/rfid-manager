package com.joakim.rfidmanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joakim.rfidmanager.data.repository.PersistedReadingRepository
import com.joakim.rfidmanager.data.settings.AppSettings
import com.joakim.rfidmanager.domain.model.PersistedReading
import com.joakim.rfidmanager.ui.viewmodel.ConnectivityViewModel
import com.joakim.rfidmanager.ui.theme.Dimens

import com.joakim.rfidmanager.ui.str
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MqttStatusScreen(
    viewModel: ConnectivityViewModel,
    repository: PersistedReadingRepository? = null,
    settings: AppSettings? = null,
    modifier: Modifier = Modifier
) {
    val status by viewModel.status.collectAsState()
    val lastHeartbeat by viewModel.lastHeartbeat.collectAsState()
    val fontSizeScale by settings?.fontSizeScale?.collectAsState() ?: remember { mutableStateOf(1.0f) }

    val isConnected = status.contains("CONNECTED", ignoreCase = true)
    val statusColor = if (isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    val coroutineScope = rememberCoroutineScope()
    var allReadings by remember(repository) { mutableStateOf<List<PersistedReading>>(emptyList()) }
    var selectedReading by remember { mutableStateOf<PersistedReading?>(null) }

    LaunchedEffect(repository) {
        repository?.let { repo ->
            repo.load()
            repo.getAllReadings().collect { all ->
                allReadings = all
            }
        }
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(str("screen.connectivity.title"), fontFamily = FontFamily.Monospace, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
        }

        Spacer(Modifier.height(16.dp))

        // Status badge with dynamic color
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = statusColor)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.Wifi else Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(status, fontFamily = FontFamily.Monospace, fontSize = 16.sp, color = MaterialTheme.colorScheme.onPrimary)
                    Text(lastHeartbeat, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ALL READINGS (transactions)
                Text(str("screen.connectivity.readings"), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (allReadings.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        str("screen.connectivity.no_readings"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Dimens.listItemSpacing)
            ) {
                items(allReadings.sortedByDescending { it.timestamp }) { reading ->
                    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
                    val timeStr = dateFormat.format(Date(reading.timestamp))
                    val isPending = !reading.transmitted
                    val isLargeText = fontSizeScale > 1.3f
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { selectedReading = reading },
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isPending) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(Dimens.cardPadding)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    reading.uidOrCode,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = (9 * fontSizeScale).sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    color = if (isPending) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                    shape = MaterialTheme.shapes.extraSmall
                                ) {
                                    Text(
                                        if (isPending) str("screen.connectivity.pending") else str("screen.connectivity.transmitted"),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontSize = (9 * fontSizeScale).sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height((4 * fontSizeScale).dp))
                            Text(
                                "${str("screen.connectivity.type")}: ${reading.type} | $timeStr",
                                fontSize = (10 * fontSizeScale).sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = if (isLargeText) 2 else 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            reading.dataPreview?.let {
                                Text(
                                    "${str("screen.connectivity.data")}: $it",
                                    fontSize = (10 * fontSizeScale).sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }

    // Detail dialog
    selectedReading?.let { reading ->
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(reading.timestamp))
        AlertDialog(
            onDismissRequest = { selectedReading = null },
            title = {
                Text(reading.uidOrCode, fontFamily = FontFamily.Monospace)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DetailRow("UID", reading.uidOrCode)
                    DetailRow("Type", reading.type)
                    DetailRow("Timestamp", dateStr)
                    reading.source?.let { DetailRow("Source", it) }
                    reading.dataPreview?.let { DetailRow("Data Preview", it) }
                    DetailRow("Transmitted", if (reading.transmitted) "Yes" else "No")
                    reading.memoryBank?.let { DetailRow("Memory Bank", it.toString()) }
                    reading.address?.let { DetailRow("Address", it.toString()) }
                    reading.length?.let { DetailRow("Length", it.toString()) }
                    reading.payload?.let { DetailRow("Payload", it) }
                    reading.correlationId?.let { DetailRow("Correlation ID", it) }
                    reading.sparkplugJson?.let { DetailRow("Full JSON", it) }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedReading = null }) {
                    Text("Stäng")
                }
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            "$label: ",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Text(
            value,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}