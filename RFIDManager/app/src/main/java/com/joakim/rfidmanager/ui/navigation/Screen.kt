package com.joakim.rfidmanager.ui.navigation

/**
 * Navigation destinations for the bottom navigation host.
 */
sealed class Screen(val route: String) {
    object Scan : Screen("scan")
    object Readings : Screen("readings")
    object Connectivity : Screen("connectivity")
    object Settings : Screen("settings")
}
