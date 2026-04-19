package com.soooool.matedash.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soooool.matedash.ServiceLocator
import com.soooool.matedash.data.api.ChargeDto
import com.soooool.matedash.data.api.DriveDto
import com.soooool.matedash.data.api.UpdateDto
import com.soooool.matedash.data.model.CarState
import com.soooool.matedash.data.repository.ApiConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class DailySummary(
    val driveCount: Int = 0,
    val driveDistanceKm: Double = 0.0,
    val driveDurationMin: Int = 0,
    val driveEnergyKwh: Double = 0.0,
    val chargeCount: Int = 0,
    val chargeEnergyKwh: Double = 0.0,
    val chargeDurationMin: Int = 0,
)

class DashboardViewModel : ViewModel() {
    val carState: StateFlow<CarState> = ServiceLocator.repository.carState
    val connectionState: StateFlow<ApiConnectionState> = ServiceLocator.repository.connectionState
    val errorMessage: StateFlow<String?> = ServiceLocator.repository.errorMessage

    private val _updates = MutableStateFlow<List<UpdateDto>>(emptyList())
    val updates: StateFlow<List<UpdateDto>> = _updates.asStateFlow()

    private val _dailySummary = MutableStateFlow(DailySummary())
    val dailySummary: StateFlow<DailySummary> = _dailySummary.asStateFlow()

    init {
        loadDailySummary()

        // connectionState가 CONNECTED로 바뀌면 dailySummary 재로드
        viewModelScope.launch {
            connectionState
                .map { it == ApiConnectionState.CONNECTED }
                .distinctUntilChanged()
                .collect { connected ->
                    if (connected) loadDailySummary()
                }
        }
    }

    fun loadUpdates() {
        val config = ServiceLocator.currentConfig ?: return
        viewModelScope.launch {
            try {
                _updates.value = ServiceLocator.apiClient.getUpdates(config)
            } catch (_: Exception) {
                _updates.value = emptyList()
            }
        }
    }

    private fun loadDailySummary() {
        val config = ServiceLocator.currentConfig ?: return
        viewModelScope.launch {
            try {
                val tz = TimeZone.currentSystemDefault()
                val today = kotlin.time.Clock.System.now().toLocalDateTime(tz).date.toString()

                val drives = ServiceLocator.apiClient.getDrives(config, limit = 50)
                val todayDrives = drives.filter { it.startDate?.startsWith(today) == true }

                val charges = ServiceLocator.apiClient.getCharges(config, limit = 50)
                val todayCharges = charges.filter { it.startDate?.startsWith(today) == true }

                _dailySummary.value = DailySummary(
                    driveCount = todayDrives.size,
                    driveDistanceKm = todayDrives.sumOf { it.odometerDetails?.odometerDistance ?: 0.0 },
                    driveDurationMin = todayDrives.sumOf { it.durationMin ?: 0 },
                    driveEnergyKwh = todayDrives.sumOf { it.energyConsumedNet ?: 0.0 },
                    chargeCount = todayCharges.size,
                    chargeEnergyKwh = todayCharges.sumOf { it.chargeEnergyAdded ?: 0.0 },
                    chargeDurationMin = todayCharges.sumOf { it.durationMin ?: 0 },
                )
            } catch (_: Exception) { }
        }
    }

    fun disconnect(onDisconnected: () -> Unit) {
        ServiceLocator.repository.stopPolling()
        onDisconnected()
    }
}
