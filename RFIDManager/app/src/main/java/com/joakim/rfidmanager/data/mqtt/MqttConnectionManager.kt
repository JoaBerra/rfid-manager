package com.joakim.rfidmanager.data.mqtt

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.joakim.rfidmanager.data.settings.BrokerDefaults
import org.eclipse.paho.client.mqttv3.*
import java.text.SimpleDateFormat
import java.util.*

class MqttConnectionManager(
    host: String = BrokerDefaults.HOST,
    port: Int = BrokerDefaults.PORT,
    username: String = "",
    password: String = "",
    private val clientId: String = "rfid-android-client"
) : MqttCallback {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var brokerUrl = "tcp://$host:$port"
    private var username: String = username
    private var password: String = password

    private val _connectionStatus = MutableStateFlow("DISCONNECTED")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val _lastError = MutableStateFlow("")
    /** Felorsak från senaste misslyckade anslutningsförsöket; tom sträng vid lyckad anslutning. */
    val lastError: StateFlow<String> = _lastError.asStateFlow()

    private val _lastHeartbeat = MutableStateFlow("")
    val lastHeartbeat: StateFlow<String> = _lastHeartbeat.asStateFlow()

    var client: MqttClient? = null
        private set

    private val tag = "MqttConnectionManager"

    private var keepAliveJob: Job? = null

    init {
        connect()
    }

    fun connect(previous: MqttClient? = null) {
        keepAliveJob?.cancel()
        scope.launch {
            // Gammal klient stängs på IO-tråden (inte huvudtråden) innan ny anslutning byggs.
            previous?.let {
                try { it.disconnect() } catch (_: Exception) {}
                try { it.close() } catch (_: Exception) {}
            }
            connectInternal()
        }
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

    fun reconnect(host: String, port: Int, username: String = "", password: String = "") {
        brokerUrl = "tcp://$host:$port"
        this.username = username
        this.password = password
        keepAliveJob?.cancel()
        val previous = client
        client = null
        // Sätts direkt (synkront) så att UI:t aldrig missar övergången till CONNECTING.
        _lastError.value = ""
        _connectionStatus.value = "CONNECTING..."
        connect(previous)
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
                // Inloggning endast om användarnamn finns; annars anonymt som förut.
                if (username.isNotEmpty()) {
                    userName = username
                    password = this@MqttConnectionManager.password.toCharArray()
                }
            }

            mqttClient.connect(options)
            client = mqttClient
            _lastError.value = ""
            _connectionStatus.value = "CONNECTED"
            updateHeartbeat("Connected")
            Log.i(tag, "Connected to $brokerUrl" + if (username.isNotEmpty()) " as $username" else "")
        } catch (e: Exception) {
            Log.e(tag, "Connection failed", e)
            _lastError.value = describeError(e)
            _connectionStatus.value = "DISCONNECTED"
        }
    }

    private fun describeError(e: Exception): String {
        val reason = if (e is MqttException) when (e.reasonCode) {
            4 -> "Fel användarnamn eller lösenord"
            5 -> "Ej behörig"
            32103 -> "Kan inte nå brokern"
            32110 -> "Anslutning pågår redan"
            else -> null
        } else null
        val detail = e.cause?.message ?: e.message ?: e.javaClass.simpleName
        return if (reason != null) "$reason ($detail)" else detail
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
