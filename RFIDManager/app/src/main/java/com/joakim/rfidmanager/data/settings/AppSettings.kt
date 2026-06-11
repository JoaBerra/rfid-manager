package com.joakim.rfidmanager.data.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode { LIGHT, DARK, SYSTEM }

class AppSettings(context: Context) {

    private val prefs = context.getSharedPreferences("rfid_settings", Context.MODE_PRIVATE)

    private val _fontSizeScale = MutableStateFlow(prefs.getFloat(KEY_FONT_SIZE, 1.0f))
    val fontSizeScale: StateFlow<Float> = _fontSizeScale.asStateFlow()

    private val _hapticEnabled = MutableStateFlow(prefs.getBoolean(KEY_HAPTIC, true))
    val hapticEnabled: StateFlow<Boolean> = _hapticEnabled.asStateFlow()

    private val _soundEnabled = MutableStateFlow(prefs.getBoolean(KEY_SOUND, true))
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _themeMode = MutableStateFlow(
        ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.DARK.name) ?: ThemeMode.DARK.name)
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _pageSize = MutableStateFlow(prefs.getInt(KEY_PAGE_SIZE, 50))
    val pageSize: StateFlow<Int> = _pageSize.asStateFlow()

    fun setFontSizeScale(scale: Float) {
        val clamped = scale.coerceIn(1.0f, 1.8f)
        prefs.edit().putFloat(KEY_FONT_SIZE, clamped).apply()
        _fontSizeScale.value = clamped
    }

    fun setHapticEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTIC, enabled).apply()
        _hapticEnabled.value = enabled
    }

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND, enabled).apply()
        _soundEnabled.value = enabled
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _themeMode.value = mode
    }

    fun setPageSize(size: Int) {
        val clamped = size.coerceIn(10, 50)
        prefs.edit().putInt(KEY_PAGE_SIZE, clamped).apply()
        _pageSize.value = clamped
    }

    companion object {
        private const val KEY_FONT_SIZE = "font_size_scale"
        private const val KEY_HAPTIC = "haptic_enabled"
        private const val KEY_SOUND = "sound_enabled"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_PAGE_SIZE = "page_size"
    }
}
