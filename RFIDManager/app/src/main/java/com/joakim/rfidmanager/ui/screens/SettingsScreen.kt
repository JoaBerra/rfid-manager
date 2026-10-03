package com.joakim.rfidmanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joakim.rfidmanager.BuildConfig
import com.joakim.rfidmanager.data.export.ReadingExporter
import com.joakim.rfidmanager.data.repository.PersistedReadingRepository
import com.joakim.rfidmanager.data.settings.AppSettings
import com.joakim.rfidmanager.data.settings.BrokerDefaults
import com.joakim.rfidmanager.ui.LocalLocalization
import com.joakim.rfidmanager.ui.str
import com.joakim.rfidmanager.ui.theme.Dimens
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    settings: AppSettings? = null,
    repository: PersistedReadingRepository? = null,
    mqttStatus: StateFlow<String>? = null,
    mqttError: StateFlow<String>? = null,
    onReconnect: (host: String, port: Int, username: String, password: String) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val fontSizeScale by settings?.fontSizeScale?.collectAsState() ?: remember { mutableStateOf(1.0f) }
    val hapticEnabled by settings?.hapticEnabled?.collectAsState() ?: remember { mutableStateOf(true) }
    val soundEnabled by settings?.soundEnabled?.collectAsState() ?: remember { mutableStateOf(true) }
    val themeMode by settings?.themeMode?.collectAsState() ?: remember { mutableStateOf(com.joakim.rfidmanager.data.settings.ThemeMode.DARK) }
    val pageSize by settings?.pageSize?.collectAsState() ?: remember { mutableStateOf(50) }
    val brokerHost by settings?.brokerHost?.collectAsState() ?: remember { mutableStateOf(BrokerDefaults.HOST) }
    val brokerPort by settings?.brokerPort?.collectAsState() ?: remember { mutableStateOf(BrokerDefaults.PORT) }
    var hostInput by remember { mutableStateOf(brokerHost) }
    var portInput by remember { mutableStateOf(brokerPort.toString()) }
    val savedUsername by settings?.mqttUsername?.collectAsState() ?: remember { mutableStateOf("") }
    val hasSavedPassword by settings?.hasMqttPassword?.collectAsState() ?: remember { mutableStateOf(false) }
    var usernameInput by remember { mutableStateOf(savedUsername) }
    // Lösenordsfältet fylls aldrig i med det sparade lösenordet; tomt fält = behåll sparat.
    var passwordInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val loc = LocalLocalization.current
    val currentLang by loc.currentLanguage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val connectingStr = str("screen.settings.broker_connecting")
    val connectionState by mqttStatus?.collectAsState() ?: remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { focusManager.clearFocus() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                val mode = repository?.let { str("screen.settings.storage_room") }
                    ?: str("screen.settings.storage_unknown")
                Text(mode, fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
                // Status för engångsmigreringen JSON -> Room samt ev. lagringsfel (tydligt, aldrig tyst)
                val migration = repository?.migrationResult?.collectAsState()?.value
                val storageError = repository?.storageError?.collectAsState()?.value
                if (migration is com.joakim.rfidmanager.data.migration.MigrationResult.Failed) {
                    Spacer(Modifier.height(Dimens.smallGap))
                    Text(
                        str("screen.settings.migration_failed") + " " + migration.message,
                        fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.error
                    )
                } else if (migration is com.joakim.rfidmanager.data.migration.MigrationResult.Migrated && migration.imported > 0) {
                    Spacer(Modifier.height(Dimens.smallGap))
                    Text(
                        str("screen.settings.migration_done") + " " + migration.imported,
                        fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground
                    )
                }
                if (storageError != null) {
                    Spacer(Modifier.height(Dimens.smallGap))
                    Text(
                        str("screen.settings.storage_error") + " " + storageError,
                        fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.error
                    )
                }
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

        // MQTT broker card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(Dimens.cardPadding)) {
                Text(str("screen.settings.broker_title"), fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(Dimens.smallGap))
                OutlinedTextField(
                    value = hostInput,
                    onValueChange = { hostInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(str("screen.settings.broker_host"), fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                )
                Spacer(Modifier.height(Dimens.smallGap))
                OutlinedTextField(
                    value = portInput,
                    onValueChange = { portInput = it.filter { c -> c.isDigit() } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(str("screen.settings.broker_port"), fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                )
                Spacer(Modifier.height(Dimens.smallGap))
                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = { usernameInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(str("screen.settings.broker_username"), fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                )
                Spacer(Modifier.height(Dimens.smallGap))
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(str("screen.settings.broker_password"), fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                    placeholder = {
                        if (hasSavedPassword) Text(str("screen.settings.broker_password_saved"), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                )
                Spacer(Modifier.height(Dimens.smallGap))
                Button(
                    onClick = {
                        val port = portInput.toIntOrNull() ?: BrokerDefaults.PORT
                        settings?.setBrokerHost(hostInput)
                        settings?.setBrokerPort(port)
                        // Tomt lösenordsfält = behåll sparat lösenord (om användarnamnet är kvar).
                        val password = if (passwordInput.isEmpty() && usernameInput.isNotBlank()) {
                            settings?.getMqttPassword() ?: ""
                        } else passwordInput
                        settings?.setMqttCredentials(usernameInput, password)
                        val username = usernameInput.trim()
                        passwordInput = ""
                        onReconnect(hostInput, port, username, if (username.isEmpty()) "" else password)
                        coroutineScope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            // "Ansluter…" visas tills resultatet finns (blockerar inte väntan på status).
                            val connectingJob = launch {
                                snackbarHostState.showSnackbar("$hostInput:$port — $connectingStr", duration = SnackbarDuration.Indefinite)
                            }
                            // reconnect() sätter CONNECTING... synkront, så första icke-CONNECTING-värdet är resultatet.
                            val status = mqttStatus?.let { flow ->
                                kotlinx.coroutines.withTimeoutOrNull(15_000L) { flow.first { it != "CONNECTING..." } }
                            }
                            connectingJob.cancel()
                            if (mqttStatus != null) {
                                val error = mqttError?.value.orEmpty()
                                val message = when {
                                    status == "CONNECTED" -> "$hostInput:$port — Ansluten ✓"
                                    status == null -> "$hostInput:$port — Misslyckades ✗ (tidsgräns)"
                                    error.isNotEmpty() -> "$hostInput:$port — Misslyckades ✗: $error"
                                    else -> "$hostInput:$port — Misslyckades ✗"
                                }
                                snackbarHostState.showSnackbar(message)
                            }
                        }
                    },
                    // Förhindra dubbla anslutningsförsök medan en anslutning pågår.
                    enabled = connectionState != "CONNECTING...",
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(str("screen.settings.broker_connect"), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
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
                InfoRow(str("screen.settings.version"), BuildConfig.VERSION_NAME)
                InfoRow(str("screen.settings.build"), "${BuildConfig.BUILD_TIME} · ${BuildConfig.GIT_COMMIT}")
                InfoRow(str("screen.settings.framework"), "Compose + Material 3")
                InfoRow(str("screen.settings.mqtt"), "Paho 1.2.5")
            }
        }

        Spacer(Modifier.height(Dimens.sectionSpacing))

        Spacer(Modifier.height(16.dp))

        Text(
            "${str("screen.settings.footer")} ${BuildConfig.VERSION_NAME}",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter)
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
