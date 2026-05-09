package com.soooool.matedash.ui.connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soooool.matedash.ServiceLocator
import com.soooool.matedash.data.api.TeslaApiConfig
import com.soooool.matedash.data.api.TeslaOAuth
import com.soooool.matedash.data.model.ApiConfig
import com.soooool.matedash.data.persistence.saveApiConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ConnectionUiState(
    // TeslaMate
    val host: String = "soooool.synology.me",
    val port: String = "9999",
    val carId: String = "1",
    val apiToken: String = "",
    // Tesla Direct
    val teslaBaseUrl: String = "https://fleet-api.prd.na.vn.cloud.tesla.com",
    val teslaAuthCode: String = "",
    val teslaShowCodeInput: Boolean = false,
    val teslaVin: String = "",
    val teslaConnected: Boolean = false,
    // Common
    val isConnecting: Boolean = false,
    val errorMessage: String? = null,
    val statusMessage: String? = null,
)

class ConnectionViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ConnectionUiState())
    val uiState: StateFlow<ConnectionUiState> = _uiState.asStateFlow()

    init {
        // 기존 Tesla 설정이 있으면 connected로 표시
        val existing = ServiceLocator.teslaApiConfig
        if (existing != null && existing.accessToken.isNotBlank()) {
            _uiState.value = _uiState.value.copy(teslaConnected = true)
        }
    }

    // ── TeslaMate 입력 ──
    fun onHostChange(host: String) { _uiState.value = _uiState.value.copy(host = host) }
    fun onPortChange(port: String) { _uiState.value = _uiState.value.copy(port = port) }
    fun onCarIdChange(carId: String) { _uiState.value = _uiState.value.copy(carId = carId) }
    fun onApiTokenChange(token: String) { _uiState.value = _uiState.value.copy(apiToken = token) }

    // ── Tesla Direct 입력 ──
    fun onTeslaBaseUrlChange(url: String) { _uiState.value = _uiState.value.copy(teslaBaseUrl = url) }
    fun onTeslaAuthCodeChange(code: String) { _uiState.value = _uiState.value.copy(teslaAuthCode = code) }
    fun onTeslaShowCodeInputChange(v: Boolean) { _uiState.value = _uiState.value.copy(teslaShowCodeInput = v) }
    fun onTeslaVinChange(vin: String) { _uiState.value = _uiState.value.copy(teslaVin = vin.trim().uppercase()) }

    fun connectTeslaMate(onSuccess: () -> Unit) {
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

        viewModelScope.launch {
            try {
                ServiceLocator.apiClient.getCarStatus(config)
                ServiceLocator.repository.startPolling(config, ServiceLocator.appSettings.pollIntervalSeconds * 1000L)
                ServiceLocator.currentConfig = config
                saveApiConfig(config)
                ServiceLocator.applyMqttSettings()
                // TeslaMate 연결 성공 시 Fleet API 풀 폴러는 끔 (MQTT가 더 빠름)
                ServiceLocator.stopFullVehiclePolling()
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

    fun connectTeslaDirect(onSuccess: () -> Unit) {
        val state = _uiState.value
        val verifier = TeslaOAuth.getCodeVerifier()
        if (state.teslaAuthCode.isBlank() || verifier == null) {
            _uiState.value = state.copy(errorMessage = "인증 코드가 비어있습니다")
            return
        }
        val code = if (state.teslaAuthCode.contains("code=")) {
            state.teslaAuthCode.substringAfter("code=").substringBefore("&")
        } else state.teslaAuthCode.trim()

        _uiState.value = state.copy(isConnecting = true, errorMessage = null, statusMessage = "토큰 교환 중...")
        viewModelScope.launch {
            try {
                val token = ServiceLocator.teslaApiClient.exchangeCodeForToken(
                    clientId = TeslaOAuth.CLIENT_ID,
                    code = code,
                    codeVerifier = verifier,
                )
                val config = TeslaApiConfig(
                    baseUrl = state.teslaBaseUrl,
                    accessToken = token.accessToken,
                    refreshToken = token.refreshToken,
                    clientId = TeslaOAuth.CLIENT_ID,
                )
                _uiState.value = _uiState.value.copy(statusMessage = "리전 등록 중...")
                ServiceLocator.teslaApiClient.registerPartnerAccount(config)
                val vehicles = ServiceLocator.teslaApiClient.getVehicles(config)
                val finalConfig = if (vehicles.size == 1) {
                    config.copy(vehicleId = vehicles.first().id)
                } else config
                ServiceLocator.teslaApiConfig = finalConfig
                _uiState.value = _uiState.value.copy(
                    isConnecting = false,
                    teslaConnected = true,
                    statusMessage = if (vehicles.size == 1)
                        "연결됨 (${vehicles.first().displayName})"
                    else "연결됨 (${vehicles.size}대 — Tesla 탭에서 선택)",
                )
                // TeslaMate가 없으면 Fleet API 풀 폴러 시작
                if (ServiceLocator.currentConfig == null) {
                    ServiceLocator.startFullVehiclePolling()
                }
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isConnecting = false,
                    errorMessage = "연결 실패: ${e.message}",
                    statusMessage = null,
                )
            }
        }
    }

    fun proceedWithExistingTesla(onSuccess: () -> Unit) {
        if (ServiceLocator.teslaApiConfig?.accessToken?.isNotBlank() == true) {
            if (ServiceLocator.currentConfig == null) {
                ServiceLocator.startFullVehiclePolling()
            }
            onSuccess()
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
