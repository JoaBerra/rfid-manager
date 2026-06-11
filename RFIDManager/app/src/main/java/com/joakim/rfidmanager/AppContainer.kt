package com.joakim.rfidmanager

import android.content.Context
import android.util.Log
import com.joakim.rfidmanager.data.localization.LocalizationManager
import com.joakim.rfidmanager.data.mqtt.MqttConnectionManager
import com.joakim.rfidmanager.data.mqtt.MqttSender
import com.joakim.rfidmanager.data.repository.PersistedReadingRepository
import com.joakim.rfidmanager.data.settings.AppSettings

class AppContainer(context: Context) {

    val settings: AppSettings by lazy { AppSettings(context) }

    val localizationManager: LocalizationManager by lazy { LocalizationManager(context) }

    val mqttManager: MqttConnectionManager by lazy {
        MqttConnectionManager().also {
            MqttSender.init(it)
        }
    }

    private val dao = try {
        val db = com.joakim.rfidmanager.data.local.DatabaseProvider.getDatabase(context)
        db.persistedReadingDao()
    } catch (e: Exception) {
        Log.e("AppContainer", "Room DAO unavailable — using JSON fallback", e)
        null
    }

    val persistedReadingRepository: PersistedReadingRepository by lazy {
        PersistedReadingRepository(dao = dao, appContext = context)
    }
}
