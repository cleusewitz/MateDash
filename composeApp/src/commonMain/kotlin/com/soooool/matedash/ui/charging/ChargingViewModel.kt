package com.soooool.matedash.ui.charging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soooool.matedash.ServiceLocator
import com.soooool.matedash.data.api.ChargeDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChargingViewModel : ViewModel() {
    private val _charges = MutableStateFlow<List<ChargeDto>>(emptyList())
    val charges: StateFlow<List<ChargeDto>> = _charges.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadCharges()
    }

    fun loadCharges() {
        val config = ServiceLocator.currentConfig ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _charges.value = ServiceLocator.apiClient.getCharges(config)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "충전 기록 로드 실패"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
