package com.joakim.rfidmanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joakim.rfidmanager.data.export.ReadingExporter
import com.joakim.rfidmanager.data.repository.PersistedReadingRepository
import com.joakim.rfidmanager.data.settings.AppSettings
import com.joakim.rfidmanager.ui.LocalLocalization
import com.joakim.rfidmanager.ui.str
import com.joakim.rfidmanager.ui.theme.Dimens
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    settings: AppSettings? = null,
    repository: PersistedReadingRepository? = null,
    modifier: Modifier = Modifier
) {
    val fontSizeScale by settings?.fontSizeScale?.collectAsState() ?: remember { mutableStateOf(1.0f) }
    val hapticEnabled by settings?.hapticEnabled?.collectAsState() ?: remember { mutableStateOf(true) }
    val soundEnabled by settings?.soundEnabled?.collectAsState() ?: remember { mutableStateOf(true) }
    val themeMode by settings?.themeMode?.collectAsState() ?: remember { mutableStateOf(com.joakim.rfidmanager.data.settings.ThemeMode.DARK) }
    val pageSize by settings?.pageSize?.collectAsState() ?: remember { mutableStateOf(50) }
    val loc = LocalLocalization.current
    val currentLang by loc.currentLanguage.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(str("screen.settings.title"), fontFamily = FontFamily.Monospace, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(Dimens.sectionSpacing))

        // Language picker
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(Dimens.cardPadding)) {
                Text(str("screen.settings.language"), fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(Dimens.smallGap))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    listOf("sv" to "Svenska", "en" to "English").forEach { (code, label) ->
                        FilterChip(
                            selected = currentLang == code,
                            onClick = { loc.setLanguage(code) },
                            label = { Text(label, fontFamily = FontFamily.Monospace, fontSize = 12.sp) }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(Dimens.sectionSpacing))

        // Storage mode card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(Dimens.cardPadding)) {
                Text(str("screen.settings.storage_mode"), fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(Dimens.smallGap))
                val mode = repository?.let {
                    when {
                        it.isUsingRealDatabase -> str("screen.settings.storage_room")
                        it.isUsingJsonFallback -> str("screen.settings.storage_json")
                        else -> str("screen.settings.storage_memory")
                    }
                } ?: str("screen.settings.storage_unknown")
                Text(mode, fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
            }
        }

        Spacer(Modifier.height(Dimens.sectionSpacing))

        // Font size slider
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(Dimens.cardPadding)) {
                Text(str("screen.settings.font_size"), fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(Dimens.smallGap))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("A", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = fontSizeScale,
                        onValueChange = { settings?.setFontSizeScale(it) },
                        valueRange = 1.0f..1.8f,
                        steps = 7,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                    Text("A", fontSize = 17.sp, color = MaterialTheme.colorScheme.onBackground)
                }
                Text(
                    "${(fontSizeScale * 100).roundToInt()}%",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        Spacer(Modifier.height(Dimens.sectionSpacing))

        // Haptic + sound toggles
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(Dimens.cardPadding)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(str("screen.settings.haptic"), fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground)
                    Switch(
                        checked = hapticEnabled,
                        onCheckedChange = { settings?.setHapticEnabled(it) }
                    )
                }
                Spacer(Modifier.height(Dimens.smallGap))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(str("screen.settings.sound"), fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground)
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = { settings?.setSoundEnabled(it) }
                    )
                }
            }
        }

        Spacer(Modifier.height(Dimens.sectionSpacing))

        // Dark mode toggle
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(Dimens.cardPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(str("screen.settings.dark_mode"), fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground)
                Spacer(Modifier.width(Dimens.smallGap))
                Switch(
                    checked = themeMode == com.joakim.rfidmanager.data.settings.ThemeMode.DARK,
                    onCheckedChange = { checked ->
                        settings?.setThemeMode(
                            if (checked) com.joakim.rfidmanager.data.settings.ThemeMode.DARK
                            else com.joakim.rfidmanager.data.settings.ThemeMode.LIGHT
                        )
                    }
                )
            }
        }

        Spacer(Modifier.height(Dimens.sectionSpacing))

        // Page size slider
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(Dimens.cardPadding)) {
                Text(str("screen.settings.page_size"), fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(Dimens.smallGap))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("10", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = (pageSize / 10).toFloat(),
                        onValueChange = { settings?.setPageSize((it.roundToInt() * 10).coerceIn(10, 50)) },
                        valueRange = 1f..5f,
                        steps = 3,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                    Text("50", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground)
                }
                Text(
                    "$pageSize",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        Spacer(Modifier.height(Dimens.sectionSpacing))

        // Export card
        val context = LocalContext.current
        var allReadings by remember(repository) { mutableStateOf<List<com.joakim.rfidmanager.domain.model.PersistedReading>>(emptyList()) }
        LaunchedEffect(repository) {
            repository?.let { repo ->
                repo.load()
                repo.getAllReadings().collect { allReadings = it }
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(Dimens.cardPadding)) {
                Text(str("screen.settings.export"), fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(Dimens.smallGap))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { ReadingExporter.shareFile(context, allReadings, "csv") },
                        enabled = allReadings.isNotEmpty()
                    ) {
                        Text(str("screen.settings.export_csv"), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    Button(
                        onClick = { ReadingExporter.shareFile(context, allReadings, "json") },
                        enabled = allReadings.isNotEmpty()
                    ) {
                        Text(str("screen.settings.export_json"), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        Spacer(Modifier.height(Dimens.sectionSpacing))

        // App info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(Dimens.cardPadding)) {
                Text(str("screen.settings.app_info"), fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(Dimens.smallGap))
                InfoRow(str("screen.settings.version"), "1.0.0 (debug)")
                InfoRow(str("screen.settings.build"), str("screen.settings.build_label"))
                InfoRow(str("screen.settings.framework"), "Compose + Material 3")
                InfoRow(str("screen.settings.mqtt"), "Paho 1.2.5")
            }
        }

        Spacer(Modifier.height(Dimens.sectionSpacing))

        Spacer(Modifier.height(16.dp))

        Text(
            str("screen.settings.footer"),
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground)
    }
}
