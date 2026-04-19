package com.soooool.matedash.ui.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soooool.matedash.ServiceLocator
import com.soooool.matedash.data.api.DriveDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

data class TripStats(
    val totalDrives: Int = 0,
    val totalKm: Double = 0.0,
    val avgKmPerDrive: Double = 0.0,
    val avgEfficiency: Double = 0.0,   // km/kWh
    val maxSpeed: Int = 0,
    val longestDriveKm: Double = 0.0,
    val monthlyKm: Double = 0.0,
    val monthlyDrives: Int = 0,
    val weeklyKm: Double = 0.0,
    val weeklyDrives: Int = 0,
    val todayKm: Double = 0.0,
    val todayDrives: Int = 0,
)

class TripViewModel : ViewModel() {
    private val _stats = MutableStateFlow(TripStats())
    val stats: StateFlow<TripStats> = _stats.asStateFlow()

    private val _recentDrives = MutableStateFlow<List<DriveDto>>(emptyList())
    val recentDrives: StateFlow<List<DriveDto>> = _recentDrives.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadStats()
    }

    fun refresh() = loadStats()

    private fun loadStats() {
        val config = ServiceLocator.currentConfig ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val drives = ServiceLocator.apiClient.getDrives(config, limit = 200)
                _stats.value = calculateStats(drives)
                _recentDrives.value = drives.sortedByDescending { it.startDate }.take(20)
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun calculateStats(drives: List<DriveDto>): TripStats {
        if (drives.isEmpty()) return TripStats()

        val tz = TimeZone.currentSystemDefault()
        val now = kotlin.time.Clock.System.now().toLocalDateTime(tz).date
        val weekAgo = now.minus(6, DateTimeUnit.DAY)
        val monthPrefix = "${now.year}-${(now.month.ordinal + 1).toString().padStart(2, '0')}"
        val todayPrefix = "$monthPrefix-${now.day.toString().padStart(2, '0')}"
        val weekStr = weekAgo.toString()

        val drivesWithDate = drives.filter { !it.startDate.isNullOrEmpty() }

        val todayDrives   = drivesWithDate.filter { it.startDate!!.startsWith(todayPrefix) }
        val weeklyDrives  = drivesWithDate.filter { it.startDate!!.take(10) >= weekStr }
        val monthlyDrives = drivesWithDate.filter { it.startDate!!.startsWith(monthPrefix) }

        val totalKm = drives.sumOf { it.odometerDetails?.odometerDistance ?: 0.0 }
        val totalEnergy = drives.sumOf { it.energyConsumedNet ?: 0.0 }

        return TripStats(
            totalDrives    = drives.size,
            totalKm        = totalKm,
            avgKmPerDrive  = if (drives.isNotEmpty()) totalKm / drives.size else 0.0,
            avgEfficiency  = if (totalEnergy > 0) totalKm / totalEnergy else 0.0,
            maxSpeed       = drives.maxOfOrNull { it.speedMax ?: 0 } ?: 0,
            longestDriveKm = drives.maxOfOrNull { it.odometerDetails?.odometerDistance ?: 0.0 } ?: 0.0,
            monthlyKm      = monthlyDrives.sumOf { it.odometerDetails?.odometerDistance ?: 0.0 },
            monthlyDrives  = monthlyDrives.size,
            weeklyKm       = weeklyDrives.sumOf { it.odometerDetails?.odometerDistance ?: 0.0 },
            weeklyDrives   = weeklyDrives.size,
            todayKm        = todayDrives.sumOf { it.odometerDetails?.odometerDistance ?: 0.0 },
            todayDrives    = todayDrives.size,
        )
    }
}
