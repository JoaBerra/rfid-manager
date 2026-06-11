package com.joakim.rfidmanager.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.joakim.rfidmanager.data.localization.LocalizationManager

val LocalLocalization = staticCompositionLocalOf<LocalizationManager> {
    error("No LocalizationManager provided")
}

@Composable
fun str(key: String): String {
    val strings by LocalLocalization.current.strings.collectAsState()
    return strings[key] ?: key
}

@Composable
fun currentLang(): String {
    val lang by LocalLocalization.current.currentLanguage.collectAsState()
    return lang
}
