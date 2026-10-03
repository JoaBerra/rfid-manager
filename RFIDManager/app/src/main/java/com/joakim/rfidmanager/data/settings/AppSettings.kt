package com.joakim.rfidmanager.data.settings

import android.content.Context
import com.joakim.rfidmanager.outbox.core.RoundsConfig
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

    /**
     * Stabilt, slumpat id för den här installationen (8 hex-tecken). Ingår i varje MQTT-meddelande
     * (deviceId + messageId) så att dashboarden kan avduplicera även om flera telefoner använder
     * samma lokala post-id. Skapas första gången det efterfrågas och bevaras därefter.
     */
    val deviceId: String
        get() = synchronized(AppSettings::class.java) {
            prefs.getString(KEY_DEVICE_ID, null)?.takeIf { it.isNotBlank() }
                ?: java.util.UUID.randomUUID().toString().replace("-", "").take(8).also {
                    prefs.edit().putString(KEY_DEVICE_ID, it).commit()
                }
        }

    private val _outboxRounds = MutableStateFlow(readOutboxRounds())

    /**
     * Utkorgens omförsök i omgångar: försök per omgång, paus (minuter) och antal omgångar.
     * Standard 12 / 60 / 3. Sparas i samma SharedPreferences som övriga inställningar; läses av
     * OutboxWorker vid varje körning, så en ändring gäller direkt nästa körning.
     */
    val outboxRounds: StateFlow<RoundsConfig> = _outboxRounds.asStateFlow()

    /** Sparar giltig konfiguration (valideras av [OutboxRoundsInput] i UI:t; [RoundsConfig] kräver >= 1). */
    fun setOutboxRounds(config: RoundsConfig) {
        prefs.edit()
            .putInt(KEY_OUTBOX_ATTEMPTS, config.attemptsPerRound)
            .putInt(KEY_OUTBOX_PAUSE_MIN, config.pauseMinutes)
            .putInt(KEY_OUTBOX_ROUNDS, config.rounds)
            .apply()
        _outboxRounds.value = config
    }

    /** Okänt/ogiltigt sparat värde (utanför appens gränser) ersätts med standardvärdet. */
    private fun readOutboxRounds(): RoundsConfig {
        fun int(key: String, default: Int, range: IntRange) =
            prefs.getInt(key, default).takeIf { it in range } ?: default
        return RoundsConfig(
            attemptsPerRound = int(KEY_OUTBOX_ATTEMPTS, RoundsConfig.DEFAULT_ATTEMPTS_PER_ROUND, OutboxRoundsInput.ATTEMPTS_RANGE),
            pauseMinutes = int(KEY_OUTBOX_PAUSE_MIN, RoundsConfig.DEFAULT_PAUSE_MINUTES, OutboxRoundsInput.PAUSE_MINUTES_RANGE),
            rounds = int(KEY_OUTBOX_ROUNDS, RoundsConfig.DEFAULT_ROUNDS, OutboxRoundsInput.ROUNDS_RANGE)
        )
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
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_MQTT_USERNAME = "mqtt_username"
        private const val KEY_MQTT_PASSWORD_ENC = "mqtt_password_enc"
        private const val KEY_OUTBOX_ATTEMPTS = "outbox_attempts_per_round"
        private const val KEY_OUTBOX_PAUSE_MIN = "outbox_pause_minutes"
        private const val KEY_OUTBOX_ROUNDS = "outbox_rounds"
    }
}
