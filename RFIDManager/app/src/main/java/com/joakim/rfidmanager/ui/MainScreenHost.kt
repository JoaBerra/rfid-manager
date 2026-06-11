package com.joakim.rfidmanager.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.joakim.rfidmanager.ui.navigation.Screen
import com.joakim.rfidmanager.ui.screens.MqttStatusScreen
import com.joakim.rfidmanager.ui.screens.ScanScreen
import com.joakim.rfidmanager.ui.screens.SettingsScreen
import com.joakim.rfidmanager.ui.components.PersistedListItem
import com.joakim.rfidmanager.data.repository.PersistedReadingRepository
import com.joakim.rfidmanager.data.settings.AppSettings
import com.joakim.rfidmanager.data.mqtt.MqttConnectionManager
import com.joakim.rfidmanager.ui.viewmodel.ConnectivityViewModel
import com.joakim.rfidmanager.ui.viewmodel.ReadingsViewModel
import com.joakim.rfidmanager.ui.viewmodel.ScanViewModel
import com.joakim.rfidmanager.ui.theme.Dimens
import com.joakim.rfidmanager.ui.str

/**
 * MainScreenHost for Fas 3.
 *
 * Replaces the old TabRow in RFIDManagerScreen with proper bottom navigation.
 * This directly addresses the "trång" (cramped) experience from Fas 2 by giving
 * each major view (Scan, Readings, Connectivity) its own dedicated screen with breathing room.
 *
 * Uses Compose Navigation as per locked architecture decision.
 * Bottom nav items match the 4-item structure in the Fas 3 design note and acceptance criteria.
 *
 * State (e.g. current readings, MQTT status) will be lifted to ViewModels in next step (Fas 3.2).
 */
/**
 * Fas 3 Delivery: MainScreenHost
 * 
 * This is the official bottom navigation host for the Fas 3 navigation foundation delivery.
 * 
 * To use from MainActivity (or a test host):
 *   MainScreenHost(persistedReadingRepository = appContainer.persistedReadingRepository)
 *
 * The repository is passed in for the Readings tab. In later steps this will be replaced
 * by proper ViewModel creation (using viewModel() factory) and full DI.
 */
@Composable
fun MainScreenHost(
    mqttManager: MqttConnectionManager? = null,
    persistedReadingRepository: PersistedReadingRepository? = null,
    settings: AppSettings? = null,
    // Scan props for Punkt 4 test (scanning in new structure)
    scanningEnabled: Boolean = false,
    onToggleScan: () -> Unit = {},
    detectedTags: List<com.joakim.rfidmanager.ui.model.RFIDTag> = emptyList(),
    onWrite: (String, Int) -> Unit = { _, _ -> },
    onPersist: (com.joakim.rfidmanager.ui.model.RFIDTag) -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Fas 3.2: ScanViewModel — flyttar selectedTagId från MainActivity
    val scanViewModel = remember { ScanViewModel() }

    // Session-scoped: tracks UIDs persisted in this session, survives nav because
    // remember is at MainScreenHost level (above NavHost composable blocks).
    val sessionPersistedUids = remember { mutableStateOf<Set<String>>(emptySet()) }
    val handlePersist: (com.joakim.rfidmanager.ui.model.RFIDTag) -> Unit = { tag ->
        onPersist(tag)
        sessionPersistedUids.value = sessionPersistedUids.value + tag.uid
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == Screen.Scan.route,
                    onClick = { navController.navigate(Screen.Scan.route) { launchSingleTop = true } },
                    icon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = { Text("Scan") }
                )
                NavigationBarItem(
                    selected = currentRoute == Screen.Readings.route,
                    onClick = { navController.navigate(Screen.Readings.route) { launchSingleTop = true } },
                    icon = { Icon(Icons.Default.List, contentDescription = null) },
                    label = { Text("Readings") }
                )
                NavigationBarItem(
                    selected = currentRoute == Screen.Connectivity.route,
                    onClick = { navController.navigate(Screen.Connectivity.route) { launchSingleTop = true } },
                    icon = { Icon(Icons.Default.SettingsInputAntenna, contentDescription = null) },
                    label = { Text("Connectivity") }
                )
                NavigationBarItem(
                    selected = currentRoute == Screen.Settings.route,
                    onClick = { navController.navigate(Screen.Settings.route) { launchSingleTop = true } },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Scan.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Scan.route) {
                val fontSizeScale by settings?.fontSizeScale?.collectAsState() ?: remember { mutableStateOf(1.0f) }
                ScanScreen(
                    scanningEnabled = scanningEnabled,
                    onToggleScan = onToggleScan,
                    detectedTags = detectedTags,
                    onTagSelected = { scanViewModel.selectTag(it) },
                    selectedTagUid = scanViewModel.selectedTagUid.collectAsState().value,
                    onWrite = onWrite,
                    onPersist = handlePersist,
                    persistedUids = sessionPersistedUids.value,
                    fontSizeScale = fontSizeScale
                )
            }
            composable(Screen.Readings.route) {
                // Dedicated Readings view – full screen, no internal tabs (Fas 3 goal).
                if (persistedReadingRepository != null) {
                    val viewModel = remember(persistedReadingRepository, settings) {
                        ReadingsViewModel(persistedReadingRepository, settings!!)
                    }
                    val readings by viewModel.readings.collectAsState()
                    val filter by viewModel.filterType.collectAsState()
                    val searchQuery by viewModel.searchQuery.collectAsState()
                    val hasMore by viewModel.hasMore.collectAsState()
                    val pageSize by settings?.pageSize?.collectAsState() ?: remember { mutableStateOf(50) }
                    val fontSizeScale by settings?.fontSizeScale?.collectAsState() ?: remember { mutableStateOf(1.0f) }
                    // Tracks loading delay for demo purposes
                    var isLoading by remember { mutableStateOf(true) }

                    LaunchedEffect(readings) {
                        if (readings.isNotEmpty()) isLoading = false
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Dimens.screenHorizontalPadding)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        Text(
                            str("screen.readings.title"),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(vertical = Dimens.smallGap)
                        )

                        // Search field
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(str("screen.readings.search_hint"), fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        Spacer(Modifier.height(Dimens.smallGap))

                        // Filter chips using VM
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            listOf(null, "RFID", "EAN").forEach { type ->
                                val label = when (type) {
                                    null -> str("screen.readings.filter_all")
                                    "RFID" -> str("screen.readings.filter_rfid")
                                    "EAN" -> str("screen.readings.filter_ean")
                                    else -> type
                                }
                                val isSelected = filter == type
                                Text(
                                    text = label,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .clickable { viewModel.setFilter(type) }
                                        .padding(4.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(Dimens.smallGap))

                        if (isLoading && readings.isEmpty()) {
                            // Loading state
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(Modifier.height(Dimens.smallGap))
                                    Text(
                                        str("screen.readings.loading"),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        } else if (readings.isEmpty()) {
                            // Empty state
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.BookmarkBorder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(Modifier.height(Dimens.smallGap))
                                    Text(
                                        str("screen.readings.empty_title"),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 14.sp
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        str("screen.readings.empty_instructions"),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            // Loading finished
                            LaunchedEffect(Unit) { isLoading = false }
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(Dimens.listItemSpacing),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(readings, key = { it.id }) { reading ->
                                    PersistedListItem(
                                        reading = reading,
                                        onTransmit = { viewModel.onTransmit(reading) },
                                        fontSizeScale = fontSizeScale
                                    )
                                }
                                if (hasMore) {
                                    item {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.sectionSpacing),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            OutlinedButton(
                                                onClick = { viewModel.loadMore() },
                                                modifier = Modifier.fillMaxWidth().padding(horizontal = Dimens.screenHorizontalPadding)
                                            ) {
                                                Text(
                                                    "${str("screen.readings.load_more")} ($pageSize)",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Dimens.screenHorizontalPadding)
                            .background(MaterialTheme.colorScheme.background),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(str("screen.readings.unavailable"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontFamily = FontFamily.Monospace)
                    }
                }
            }
            composable(Screen.Connectivity.route) {
                val connectivityViewModel = remember(mqttManager) {
                    mqttManager?.let { ConnectivityViewModel(it) }
                }
                if (connectivityViewModel != null) {
                    MqttStatusScreen(
                        viewModel = connectivityViewModel,
                        repository = persistedReadingRepository,
                        settings = settings,
                        modifier = Modifier.background(MaterialTheme.colorScheme.background)
                    )
                }
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    settings = settings,
                    repository = persistedReadingRepository,
                    modifier = Modifier.background(MaterialTheme.colorScheme.background)
                )
            }
        }
    }
}
