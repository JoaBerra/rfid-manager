package com.joakim.rfidmanager.data.localization

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

class LocalizationManager(context: Context) {

    private val prefs = context.getSharedPreferences("rfid_settings", Context.MODE_PRIVATE)
    private val assetManager = context.assets

    private val _currentLanguage = MutableStateFlow(prefs.getString(KEY_LANGUAGE, DEFAULT_LANG) ?: DEFAULT_LANG)
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private val _strings = MutableStateFlow<Map<String, String>>(emptyMap())
    val strings: StateFlow<Map<String, String>> = _strings.asStateFlow()

    init {
        loadStrings(_currentLanguage.value)
    }

    fun setLanguage(lang: String) {
        if (lang == _currentLanguage.value) return
        prefs.edit().putString(KEY_LANGUAGE, lang).apply()
        _currentLanguage.value = lang
        loadStrings(lang)
    }

    private fun loadStrings(lang: String) {
        try {
            val jsonString = assetManager.open("strings_$lang.json").bufferedReader().use { it.readText() }
            val map = mutableMapOf<String, String>()
            val json = JSONObject(jsonString)
            for (key in json.keys()) {
                map[key] = json.getString(key)
            }
            _strings.value = map
        } catch (e: Exception) {
            _strings.value = emptyMap()
        }
    }

    companion object {
        private const val KEY_LANGUAGE = "language"
        private const val DEFAULT_LANG = "sv"
    }
}
