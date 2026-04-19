package com.soooool.matedash.ui.tesla

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soooool.matedash.ServiceLocator
import com.soooool.matedash.data.api.TeslaApiConfig
import com.soooool.matedash.data.api.TeslaVehicle
import com.soooool.matedash.data.api.TeslaVehicleData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class TeslaUiState(
    val isConfigured: Boolean = false,
    val isLoading: Boolean = false,
    val vehicles: List<TeslaVehicle> = emptyList(),
    val vehicleData: TeslaVehicleData? = null,
    val commandResult: String? = null,
    val error: String? = null,
)

class TeslaViewModel : ViewModel() {
    private val client = ServiceLocator.teslaApiClient

    private val _uiState = MutableStateFlow(TeslaUiState())
    val uiState: StateFlow<TeslaUiState> = _uiState

    init {
        val configured = ServiceLocator.teslaApiConfig != null
        println("[MateDash] init: isConfigured=$configured")
        _uiState.value = _uiState.value.copy(isConfigured = configured)
    }

    fun refreshConfig() {
        val configured = ServiceLocator.teslaApiConfig != null
        println("[MateDash] refreshConfig: isConfigured=$configured")
        _uiState.value = _uiState.value.copy(isConfigured = configured)
    }

    fun loadVehicles() {
        val config = ServiceLocator.teslaApiConfig
        println("[MateDash] loadVehicles: config=${config != null}")
        if (config == null) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val vehicles = client.getVehicles(config)
                println("[MateDash] loadVehicles: success, count=${vehicles.size}")
                _uiState.value = _uiState.value.copy(vehicles = vehicles, isLoading = false)
            } catch (e: Exception) {
                println("[MateDash] loadVehicles: error=${e.message}")
                _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
            }
        }
    }

    fun loadVehicleData() {
        val config = ServiceLocator.teslaApiConfig
        println("[MateDash] loadVehicleData: config=${config != null}, vehicleId=${config?.vehicleId}")
        if (config == null) return
        if (config.vehicleId == 0L) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val data = client.getVehicleData(config)
                println("[MateDash] loadVehicleData: success, state=${data.state}")
                _uiState.value = _uiState.value.copy(vehicleData = data, isLoading = false)
            } catch (e: Exception) {
                println("[MateDash] loadVehicleData: error=${e.message}")
                _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
            }
        }
    }

    fun wakeUp() {
        val config = ServiceLocator.teslaApiConfig ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(commandResult = null, error = null)
            try {
                client.wakeUp(config)
                _uiState.value = _uiState.value.copy(commandResult = "Wake up 명령 전송")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun sendCommand(command: String, label: String) {
        val config = ServiceLocator.teslaApiConfig ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(commandResult = null, error = null)
            try {
                val result = client.sendCommand(config, command)
                _uiState.value = _uiState.value.copy(
                    commandResult = if (result.result) "$label 성공" else "$label 실패: ${result.reason}",
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun selectVehicle(vehicle: TeslaVehicle) {
        val config = ServiceLocator.teslaApiConfig ?: return
        ServiceLocator.teslaApiConfig = config.copy(vehicleId = vehicle.id) // setter persists
        loadVehicleData()
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(commandResult = null, error = null)
    }
}
