package com.soooool.matedash.data.repository

import com.soooool.matedash.data.api.TeslaMateApiClient
import com.soooool.matedash.data.api.toCarState
import com.soooool.matedash.data.model.ApiConfig
import com.soooool.matedash.data.model.CarState
import com.soooool.matedash.data.model.MqttConfig
import com.soooool.matedash.data.mqtt.MqttConnectionState
import com.soooool.matedash.data.mqtt.MqttService
import com.soooool.matedash.data.mqtt.applyTeslaMateTopic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.soooool.matedash.data.persistence.updateLiveActivityState

enum class ApiConnectionState { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

class TeslaMateRepository(private val apiClient: TeslaMateApiClient) {

    private val _carState = MutableStateFlow(CarState())
    val carState: StateFlow<CarState> = _carState.asStateFlow()

    private val _connectionState = MutableStateFlow(ApiConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ApiConnectionState> = _connectionState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var pollingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    private val _fastPollingRequested = MutableStateFlow(false)

    private var mqttService: MqttService? = null
    private val _mqttState = MutableStateFlow(MqttConnectionState.DISCONNECTED)
    val mqttState: StateFlow<MqttConnectionState> = _mqttState.asStateFlow()
    private val _mqttError = MutableStateFlow<String?>(null)
    val mqttError: StateFlow<String?> = _mqttError.asStateFlow()

    fun requestFastPolling(enabled: Boolean) {
        _fastPollingRequested.value = enabled
    }

    /** 외부 미디어 소스(Tesla Fleet API 등)에서 가져온 재생 정보를 CarState에 병합 */
    fun updateMediaInfo(title: String, artist: String, album: String, source: String, isPlaying: Boolean) {
        _carState.value = _carState.value.copy(
            mediaTitle = title,
            mediaArtist = artist,
            mediaAlbum = album,
            mediaPlaylist = source, // playlist 자리에 source(Apple Music/Spotify 등) 표시
            mediaStatus = if (isPlaying) "Playing" else if (title.isBlank()) "" else "Paused",
        )
    }

    fun startMqtt(service: MqttService, host: String, port: Int, carId: Int, username: String, password: String) {
        stopMqtt()
        if (host.isBlank()) {
            _mqttError.value = "호스트가 비어있습니다"
            return
        }
        mqttService = service
        _mqttError.value = null
        _mqttState.value = MqttConnectionState.CONNECTING
        scope.launch {
            service.connectionState.collect { _mqttState.value = it }
        }
        service.connect(
            config = MqttConfig(host = host, port = port, carId = carId, username = username, password = password),
            onConnected = {
                service.subscribe("teslamate/cars/$carId/+") { topic, payload ->
                    val attr = topic.substringAfterLast('/')
                    _carState.value = _carState.value.applyTeslaMateTopic(attr, payload)
                }
            },
            onError = { msg ->
                _mqttError.value = msg
                _mqttState.value = MqttConnectionState.ERROR
            },
        )
    }

    fun stopMqtt() {
        mqttService?.disconnect()
        mqttService = null
        _mqttState.value = MqttConnectionState.DISCONNECTED
    }

    fun startPolling(config: ApiConfig, pollIntervalMs: Long = 30_000L) {
        stopPolling()
        _connectionState.value = ApiConnectionState.CONNECTING

        pollingJob = scope.launch {
            while (isActive) {
                try {
                    val dto = apiClient.getCarStatus(config)
                    val newState = dto.toCarState()
                    _carState.value = newState
                    _connectionState.value = ApiConnectionState.CONNECTED
                    _errorMessage.value = null
                    updateLiveActivityState(newState)
                } catch (e: Exception) {
                    _connectionState.value = ApiConnectionState.ERROR
                    _errorMessage.value = e.message ?: "데이터 수신 실패"
                }
                val activeStates = listOf("driving", "charging")
                val interval = when {
                    _fastPollingRequested.value -> 5_000L
                    _carState.value.state.lowercase() in activeStates -> 5_000L
                    else -> pollIntervalMs
                }
                delay(interval)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        _connectionState.value = ApiConnectionState.DISCONNECTED
    }
}
