package com.joakim.rfidmanager.data.mqtt

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.eclipse.paho.client.mqttv3.*
import java.text.SimpleDateFormat
import java.util.*

class MqttConnectionManager(
    private val brokerUrl: String = "tcp://192.168.50.107:1883",
    private val clientId: String = "rfid-android-client"
) : MqttCallback {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _connectionStatus = MutableStateFlow("DISCONNECTED")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val _lastHeartbeat = MutableStateFlow("")
    val lastHeartbeat: StateFlow<String> = _lastHeartbeat.asStateFlow()

    var client: MqttClient? = null
        private set

    private val tag = "MqttConnectionManager"

    private var keepAliveJob: Job? = null

    init {
        connect()
    }

    fun connect() {
        scope.launch {
            connectInternal()
        }
        keepAliveJob?.cancel()
        keepAliveJob = scope.launch {
            delay(35_000)
            while (isActive) {
                if (client?.isConnected == true) {
                    updateHeartbeat("Alive")
                    Log.d(tag, "Keep-alive OK")
                } else if (_connectionStatus.value != "CONNECTING...") {
                    _connectionStatus.value = "DISCONNECTED"
                    connectInternal()
                }
                delay(30_000)
            }
        }
    }

    private suspend fun connectInternal() = withContext(Dispatchers.IO) {
        try {
            _connectionStatus.value = "CONNECTING..."
            val mqttClient = MqttClient(brokerUrl, clientId, null)
            mqttClient.setCallback(this@MqttConnectionManager)

            val options = MqttConnectOptions().apply {
                keepAliveInterval = 30
                connectionTimeout = 10
                isCleanSession = true
            }

            mqttClient.connect(options)
            client = mqttClient
            _connectionStatus.value = "CONNECTED"
            updateHeartbeat("Connected")
            Log.i(tag, "Connected to $brokerUrl")
        } catch (e: Exception) {
            Log.e(tag, "Connection failed", e)
            _connectionStatus.value = "DISCONNECTED"
        }
    }

    fun disconnect() {
        keepAliveJob?.cancel()
        scope.cancel()
        try {
            client?.disconnect()
            client?.close()
            client = null
        } catch (e: Exception) {
            Log.e(tag, "Disconnect error", e)
        }
        _connectionStatus.value = "DISCONNECTED"
    }

    override fun connectionLost(cause: Throwable?) {
        Log.w(tag, "Connection lost", cause)
        _connectionStatus.value = "DISCONNECTED"
        updateHeartbeat("Lost")
    }

    override fun deliveryComplete(token: IMqttDeliveryToken) {
        updateHeartbeat("Delivered")
    }

    override fun messageArrived(topic: String?, message: MqttMessage?) {
        updateHeartbeat("Msg on $topic")
    }

    fun onPublished() {
        updateHeartbeat("Published")
    }

    private fun updateHeartbeat(event: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        _lastHeartbeat.value = "$event $time"
    }
}
