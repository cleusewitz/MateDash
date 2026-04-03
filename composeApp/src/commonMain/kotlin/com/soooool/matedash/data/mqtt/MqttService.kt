package com.soooool.matedash.data.mqtt

import com.soooool.matedash.data.model.MqttConfig
import kotlinx.coroutines.flow.StateFlow

enum class MqttConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING, ERROR
}

interface MqttService {
    val connectionState: StateFlow<MqttConnectionState>
    fun connect(config: MqttConfig, onConnected: () -> Unit, onError: (String) -> Unit)
    fun subscribe(topic: String, onMessage: (String, String) -> Unit)
    fun disconnect()
}

expect fun createMqttService(): MqttService
