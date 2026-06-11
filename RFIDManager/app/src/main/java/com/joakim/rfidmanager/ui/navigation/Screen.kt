package com.joakim.rfidmanager.ui.navigation

/**
 * Sealed class for Fas 3 navigation destinations.
 *
 * This replaces the old TabRow-based navigation from Fas 2.
 * Enables dedicated screens with bottom navigation for better breathing room
 * and clearer separation of concerns (as per Fas 3 acceptance criteria).
 *
 * Routes are used with Compose Navigation (androidx.navigation.compose).
 * Each top-level screen gets its own destination for proper back stack and state.
 */
sealed class Screen(val route: String) {
    object Scan : Screen("scan")          // Main live scanning + Radar + overview (refactored from old RFIDManagerScreen)
    object Readings : Screen("readings")  // Dedicated readings view (VM-driven, filter, rich list)
    object Connectivity : Screen("connectivity") // Dedicated MqttStatusScreen (status, log, test publish)
    object Settings : Screen("settings")  // Placeholder for future (minimal in Fas 3)
}
