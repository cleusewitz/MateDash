package com.soooool.matedash.data.mqtt

import com.soooool.matedash.data.model.MqttConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual fun createMqttService(): MqttService = IosMqttService()

private class IosMqttService : MqttService {
    private val _connectionState = MutableStateFlow(MqttConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<MqttConnectionState> = _connectionState.asStateFlow()

    override fun connect(config: MqttConfig, onConnected: () -> Unit, onError: (String) -> Unit) {
        onError("iOS MQTT는 아직 지원되지 않습니다.")
    }

    override fun subscribe(topic: String, onMessage: (String, String) -> Unit) {}

    override fun disconnect() {
        _connectionState.value = MqttConnectionState.DISCONNECTED
    }
}
