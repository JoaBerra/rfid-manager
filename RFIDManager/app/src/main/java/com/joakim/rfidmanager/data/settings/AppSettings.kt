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

    private val _brokerHost = MutableStateFlow(prefs.getString(KEY_BROKER_HOST, BrokerDefaults.HOST) ?: BrokerDefaults.HOST)
    val brokerHost: StateFlow<String> = _brokerHost.asStateFlow()

    private val _brokerPort = MutableStateFlow(prefs.getInt(KEY_BROKER_PORT, BrokerDefaults.PORT))
    val brokerPort: StateFlow<Int> = _brokerPort.asStateFlow()

    private val _mqttUsername = MutableStateFlow(prefs.getString(KEY_MQTT_USERNAME, "") ?: "")
    /** Tomt användarnamn = ingen MQTT-inloggning (anonymt). */
    val mqttUsername: StateFlow<String> = _mqttUsername.asStateFlow()

    private val _hasMqttPassword = MutableStateFlow(prefs.contains(KEY_MQTT_PASSWORD_ENC))
    val hasMqttPassword: StateFlow<Boolean> = _hasMqttPassword.asStateFlow()

    /** Dekrypterat lösenord, eller tom sträng om inget är sparat. Får aldrig loggas. */
    fun getMqttPassword(): String =
        prefs.getString(KEY_MQTT_PASSWORD_ENC, null)?.let { SecretCipher.decrypt(it) } ?: ""

    /** Sparar användarnamn och krypterat lösenord. Tomt användarnamn rensar även lösenordet. */
    fun setMqttCredentials(username: String, password: String) {
        val user = username.trim()
        val editor = prefs.edit().putString(KEY_MQTT_USERNAME, user)
        if (user.isEmpty() || password.isEmpty()) {
            editor.remove(KEY_MQTT_PASSWORD_ENC)
        } else {
            editor.putString(KEY_MQTT_PASSWORD_ENC, SecretCipher.encrypt(password))
        }
        editor.apply()
        _mqttUsername.value = user
        _hasMqttPassword.value = user.isNotEmpty() && password.isNotEmpty()
    }

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

    fun setBrokerHost(host: String) {
        prefs.edit().putString(KEY_BROKER_HOST, host).apply()
        _brokerHost.value = host
    }

    fun setBrokerPort(port: Int) {
        prefs.edit().putInt(KEY_BROKER_PORT, port).apply()
        _brokerPort.value = port
    }

    companion object {
        private const val KEY_FONT_SIZE = "font_size_scale"
        private const val KEY_HAPTIC = "haptic_enabled"
        private const val KEY_SOUND = "sound_enabled"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_PAGE_SIZE = "page_size"
        private const val KEY_BROKER_HOST = "broker_host"
        private const val KEY_BROKER_PORT = "broker_port"
        private const val KEY_MQTT_USERNAME = "mqtt_username"
        private const val KEY_MQTT_PASSWORD_ENC = "mqtt_password_enc"
    }
}
