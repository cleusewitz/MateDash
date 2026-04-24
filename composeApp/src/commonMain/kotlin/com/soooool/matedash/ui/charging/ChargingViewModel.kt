package com.soooool.matedash.ui.charging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soooool.matedash.ServiceLocator
import com.soooool.matedash.data.api.ChargeDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class ChargingViewModel : ViewModel() {
    private val now = kotlin.time.Clock.System.now().let {
        kotlinx.datetime.Instant.fromEpochMilliseconds(it.toEpochMilliseconds())
            .toLocalDateTime(TimeZone.currentSystemDefault())
    }

    private val _selectedYear = MutableStateFlow(now.year)
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    private val _selectedMonth = MutableStateFlow(now.month.ordinal + 1)
    val selectedMonth: StateFlow<Int> = _selectedMonth.asStateFlow()

    /** 선택된 월 기준 필터링된 충전 (요약 카드용) */
    private val _filteredCharges = MutableStateFlow<List<ChargeDto>>(emptyList())
    val filteredCharges: StateFlow<List<ChargeDto>> = _filteredCharges.asStateFlow()

    /** 전체 충전 기록 (리스트용, 최신순 정렬) */
    private val _allCharges = MutableStateFlow<List<ChargeDto>>(emptyList())
    val allCharges: StateFlow<List<ChargeDto>> = _allCharges.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _selectedChargeId = MutableStateFlow<Int?>(null)
    val selectedChargeId: StateFlow<Int?> = _selectedChargeId.asStateFlow()

    val selectedCharge: StateFlow<ChargeDto?> =
        combine(_allCharges, _selectedChargeId) { list, id ->
            if (id == null) null else list.firstOrNull { it.chargeId == id }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun selectCharge(id: Int?) {
        _selectedChargeId.value = id
    }

    fun clearSelection() {
        _selectedChargeId.value = null
    }

    private var loaded = false

    fun loadIfNeeded() {
        if (!loaded) {
            loaded = true
            loadAllCharges()
        }
    }

    fun setMonth(year: Int, month: Int) {
        _selectedYear.value = year
        _selectedMonth.value = month
        applyFilter()
    }

    fun previousMonth() {
        when (_selectedMonth.value) {
            0 -> {
                _selectedYear.value -= 1
                _selectedMonth.value = 12
            }
            1 -> _selectedMonth.value = 0
            else -> _selectedMonth.value -= 1
        }
        applyFilter()
    }

    fun nextMonth() {
        when (_selectedMonth.value) {
            0 -> _selectedMonth.value = 1
            12 -> {
                _selectedYear.value += 1
                _selectedMonth.value = 0
            }
            else -> _selectedMonth.value += 1
        }
        applyFilter()
    }

    private fun applyFilter() {
        val all = _allCharges.value
        val year = _selectedYear.value
        val month = _selectedMonth.value
        _filteredCharges.value = if (month == 0) {
            all.filter { it.startDate?.startsWith("$year-") == true }
        } else {
            val prefix = "$year-${month.toString().padStart(2, '0')}"
            all.filter { it.startDate?.startsWith(prefix) == true }
        }
    }

    fun retry() = loadAllCharges()

    private fun loadAllCharges() {
        val config = ServiceLocator.currentConfig
        if (config == null) {
            _errorMessage.value = "서버 설정이 없습니다"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val sorted = ServiceLocator.apiClient.getCharges(config, limit = 100)
                    .sortedByDescending { it.startDate }
                _allCharges.value = sorted

                // 가장 최근 충전 데이터의 월로 자동 설정
                val latest = sorted.firstOrNull()?.startDate
                if (latest != null && latest.length >= 7) {
                    val year = latest.substring(0, 4).toIntOrNull()
                    val month = latest.substring(5, 7).toIntOrNull()
                    if (year != null && month != null) {
                        _selectedYear.value = year
                        _selectedMonth.value = month
                    }
                }
                applyFilter()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "충전 기록 로드 실패"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
