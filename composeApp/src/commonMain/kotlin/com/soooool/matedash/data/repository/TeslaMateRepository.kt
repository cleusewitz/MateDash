package com.soooool.matedash.data.repository

import com.soooool.matedash.data.api.TeslaMateApiClient
import com.soooool.matedash.data.api.toCarState
import com.soooool.matedash.data.model.ApiConfig
import com.soooool.matedash.data.model.CarState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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

    fun startPolling(config: ApiConfig, pollIntervalMs: Long = 30_000L) {
        stopPolling()
        _connectionState.value = ApiConnectionState.CONNECTING

        pollingJob = scope.launch {
            while (isActive) {
                try {
                    val dto = apiClient.getCarStatus(config)
                    _carState.value = dto.toCarState()
                    _connectionState.value = ApiConnectionState.CONNECTED
                    _errorMessage.value = null
                } catch (e: Exception) {
                    _connectionState.value = ApiConnectionState.ERROR
                    _errorMessage.value = e.message ?: "데이터 수신 실패"
                }
                delay(pollIntervalMs)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        _connectionState.value = ApiConnectionState.DISCONNECTED
    }
}
