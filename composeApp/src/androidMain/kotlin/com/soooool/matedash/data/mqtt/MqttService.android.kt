package com.soooool.matedash.data.mqtt

import com.soooool.matedash.data.model.MqttConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

actual fun createMqttService(): MqttService = AndroidMqttService()

private class AndroidMqttService : MqttService {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _connectionState = MutableStateFlow(MqttConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<MqttConnectionState> = _connectionState.asStateFlow()
    private var client: MqttClient? = null

    override fun connect(config: MqttConfig, onConnected: () -> Unit, onError: (String) -> Unit) {
        scope.launch {
            try {
                _connectionState.value = MqttConnectionState.CONNECTING
                val serverUri = "tcp://${config.host}:${config.port}"
                val clientId = "MateDash-${System.currentTimeMillis()}"
                val mqttClient = MqttClient(serverUri, clientId, MemoryPersistence())

                val options = MqttConnectOptions().apply {
                    isCleanSession = true
                    connectionTimeout = 10
                    keepAliveInterval = 60
                    isAutomaticReconnect = true
                    if (config.username.isNotEmpty()) {
                        userName = config.username
                        password = config.password.toCharArray()
                    }
                }

                mqttClient.setCallback(object : MqttCallbackExtended {
                    override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                        _connectionState.value = MqttConnectionState.CONNECTED
                    }
                    override fun connectionLost(cause: Throwable?) {
                        _connectionState.value = MqttConnectionState.RECONNECTING
                    }
                    override fun messageArrived(topic: String?, message: MqttMessage?) {}
                    override fun deliveryComplete(token: IMqttDeliveryToken?) {}
                })

                mqttClient.connect(options)
                client = mqttClient
                _connectionState.value = MqttConnectionState.CONNECTED
                withContext(Dispatchers.Main) { onConnected() }
            } catch (e: Exception) {
                _connectionState.value = MqttConnectionState.ERROR
                withContext(Dispatchers.Main) { onError(e.message ?: "연결 실패") }
            }
        }
    }

    override fun subscribe(topic: String, onMessage: (String, String) -> Unit) {
        try {
            client?.subscribe(topic, 0) { t, message ->
                message?.let { onMessage(t ?: topic, it.toString()) }
            }
        } catch (_: Exception) {}
    }

    override fun disconnect() {
        scope.launch {
            try {
                client?.disconnect()
                client = null
            } catch (_: Exception) {}
            _connectionState.value = MqttConnectionState.DISCONNECTED
        }
    }
}
