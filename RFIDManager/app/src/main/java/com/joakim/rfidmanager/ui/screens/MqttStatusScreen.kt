package com.joakim.rfidmanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joakim.rfidmanager.data.mqtt.MqttSender
import com.joakim.rfidmanager.ui.theme.*
import kotlinx.coroutines.launch

/**
 * MqttStatusScreen – första version för att visa Sparkplug/MQTT status,
 * last transmitted (från spec), recent messages, och test publish.
 *
 * Följer Figma-Design-Spec-Fas2 + nomenclature.
 * Använder MqttSender för demo.
 */
@Composable
fun MqttStatusScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("SPARKPLUG CONNECTED ✓") }
    var lastHeartbeat by remember { mutableStateOf("Last heartbeat: 10:41:37.224 | Session: 14h 22m") }
    var lastTransmitted by remember { mutableStateOf(
        """{
  "type": "ReadEscortMemory",
  "uid": "E4:9A:7C:2B:11:FF",
  "timestamp": "2025-02-18T10:41:29.881Z",
  "data": {
    "memoryBank": 3,
    "address": 0,
    "length": 64,
    "payload": "4D4F44454C3A45532D523132..."
  },
  "sparkplug": true
}"""
    ) }
    val messages = remember { mutableStateListOf(
        "✓ ReadEscortMemory E4:9A:7C:2B:11:FF 10:41:29 [PUB]",
        "⭐ NodeBirth EdgeNode-PlantA-RFID01 10:39:12 [BIRTH]",
        "✓ ReadEscortMemory D83F:91:44:A2:7E 10:37:05 [PUB]",
        "🕐 DeviceData Status update 10:34:51 [SUB]"
    ) }

    Column(modifier = modifier.fillMaxSize().background(Background).padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onClose) { Text("← Close") }
            Text("MQTT / Sparkplug", fontFamily = FontFamily.Monospace, fontSize = 18.sp, color = Foreground)
        }

        Spacer(Modifier.height(16.dp))

        // Large status badge (från spec)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = StatusConnected)
        ) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(status, fontFamily = FontFamily.Monospace, fontSize = 16.sp, color = Color.White)
                Text(lastHeartbeat, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Color.White)
            }
        }

        Spacer(Modifier.height(16.dp))

        // LAST TRANSMITTED (från spec och bild)
        Text("LAST TRANSMITTED", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MutedForeground)
        Card(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            colors = CardDefaults.cardColors(containerColor = Card)
        ) {
            Text(
                lastTransmitted,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = Foreground,
                modifier = Modifier.padding(12.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        // Actions
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    scope.launch {
                        // Demo publish
                        val demo = com.joakim.rfidmanager.domain.model.PersistedReading(
                            id = 0,
                            type = "RFID",
                            uidOrCode = "E4:9A:7C:2B:11:FF",
                            timestamp = System.currentTimeMillis(),
                            source = "Test",
                            dataPreview = "memoryBank:3 ...",
                            status = "sending",
                            transmitted = false
                        )
                        MqttSender.sendReading(demo)
                        messages.add(0, "✓ ReadEscortMemory ${demo.uidOrCode} ${java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date())} [PUB]")
                        lastTransmitted = """{"type":"ReadEscortMemory","uid":"${demo.uidOrCode}","timestamp":"...","sparkplug":true}"""
                        status = "SPARKPLUG CONNECTED ✓"
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Test Publish Reading", fontFamily = FontFamily.Monospace)
            }
            OutlinedButton(onClick = { /* expand log */ }, modifier = Modifier.weight(1f)) {
                Text("View Log", fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(Modifier.height(16.dp))

        // RECENT MQTT MESSAGES
        Text("RECENT MQTT MESSAGES", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MutedForeground)
        LazyColumn {
            items(messages) { msg ->
                Text(
                    msg,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = Foreground,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Divider(color = Border)
            }
        }
    }
}