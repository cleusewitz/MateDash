package com.soooool.matedash.data.repository

import com.soooool.matedash.data.api.TeslaApiConfig
import com.soooool.matedash.data.api.TeslaFleetApiClient
import com.soooool.matedash.data.api.TeslaVehicleData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TeslaVehicleRepository(
    private val apiClient: TeslaFleetApiClient,
) : VehicleDataSource {

    private val _vehicleData = MutableStateFlow<TeslaVehicleData?>(null)
    override val vehicleData: StateFlow<TeslaVehicleData?> = _vehicleData.asStateFlow()

    private val _state = MutableStateFlow(DataSourceState.IDLE)
    override val state: StateFlow<DataSourceState> = _state.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error.asStateFlow()

    private var pollingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    override fun start(config: TeslaApiConfig, intervalMs: Long) {
        stop()
        if (config.accessToken.isBlank() || config.vehicleId == 0L) return

        _state.value = DataSourceState.CONNECTING
        println("[MateDash] TeslaVehicleRepository: polling start, vehicleId=${config.vehicleId}, interval=${intervalMs}ms")

        pollingJob = scope.launch {
            while (isActive) {
                try {
                    val data = apiClient.getVehicleData(config)
                    _vehicleData.value = data
                    _state.value = DataSourceState.CONNECTED
                    _error.value = null
                } catch (e: Exception) {
                    _state.value = DataSourceState.ERROR
                    _error.value = e.message ?: "차량 데이터 수신 실패"
                    println("[MateDash] TeslaVehicleRepository: poll error=${e.message}")
                }
                delay(intervalMs)
            }
        }
    }

    override fun stop() {
        pollingJob?.cancel()
        pollingJob = null
        _state.value = DataSourceState.IDLE
        println("[MateDash] TeslaVehicleRepository: polling stopped")
    }
}
