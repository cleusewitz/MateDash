package com.soooool.matedash.data.repository

import com.soooool.matedash.data.api.TeslaApiConfig
import com.soooool.matedash.data.api.TeslaVehicleData
import kotlinx.coroutines.flow.StateFlow

enum class DataSourceState { IDLE, CONNECTING, CONNECTED, ERROR }

interface VehicleDataSource {
    val vehicleData: StateFlow<TeslaVehicleData?>
    val state: StateFlow<DataSourceState>
    val error: StateFlow<String?>
    fun start(config: TeslaApiConfig, intervalMs: Long = 30_000L)
    fun stop()
}
