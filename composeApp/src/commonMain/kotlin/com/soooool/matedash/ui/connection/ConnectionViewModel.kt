package com.soooool.matedash.ui.connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soooool.matedash.ServiceLocator
import com.soooool.matedash.data.model.ApiConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ConnectionUiState(
    val host: String = "soooool.synology.me",
    val port: String = "9999",
    val carId: String = "1",
    val apiToken: String = "",
    val isConnecting: Boolean = false,
    val errorMessage: String? = null,
)

class ConnectionViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ConnectionUiState())
    val uiState: StateFlow<ConnectionUiState> = _uiState.asStateFlow()

    fun onHostChange(host: String) { _uiState.value = _uiState.value.copy(host = host) }
    fun onPortChange(port: String) { _uiState.value = _uiState.value.copy(port = port) }
    fun onCarIdChange(carId: String) { _uiState.value = _uiState.value.copy(carId = carId) }
    fun onApiTokenChange(token: String) { _uiState.value = _uiState.value.copy(apiToken = token) }

    fun connect(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.host.isBlank()) {
            _uiState.value = state.copy(errorMessage = "호스트 주소를 입력해주세요")
            return
        }
        val config = ApiConfig(
            baseUrl = "http://${state.host.trim()}:${state.port.trim()}",
            apiToken = state.apiToken.trim(),
            carId = state.carId.toIntOrNull() ?: 1,
        )
        _uiState.value = state.copy(isConnecting = true, errorMessage = null)

        // 연결 테스트: 첫 API 호출
        viewModelScope.launch {
            try {
                ServiceLocator.apiClient.getCarStatus(config)
                ServiceLocator.repository.startPolling(config)
                ServiceLocator.currentConfig = config
                _uiState.value = _uiState.value.copy(isConnecting = false)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isConnecting = false,
                    errorMessage = "연결 실패: ${e.message}",
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
