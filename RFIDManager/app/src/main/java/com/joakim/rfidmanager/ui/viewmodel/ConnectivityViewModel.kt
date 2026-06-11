package com.joakim.rfidmanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.joakim.rfidmanager.data.mqtt.MqttConnectionManager
import kotlinx.coroutines.flow.StateFlow

class ConnectivityViewModel(
    private val mqttManager: MqttConnectionManager
) : ViewModel() {

    val status: StateFlow<String> = mqttManager.connectionStatus
    val lastHeartbeat: StateFlow<String> = mqttManager.lastHeartbeat
}
