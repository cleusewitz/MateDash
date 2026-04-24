package com.soooool.matedash.ui.driving

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soooool.matedash.ServiceLocator
import com.soooool.matedash.data.api.DriveDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DrivingViewModel : ViewModel() {
    private val _drives = MutableStateFlow<List<DriveDto>>(emptyList())
    val drives: StateFlow<List<DriveDto>> = _drives.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _selectedDriveId = MutableStateFlow<Int?>(null)
    val selectedDriveId: StateFlow<Int?> = _selectedDriveId.asStateFlow()

    val selectedDrive: StateFlow<DriveDto?> =
        combine(_drives, _selectedDriveId) { list, id ->
            if (id == null) null else list.firstOrNull { it.driveId == id }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var loaded = false

    fun loadDrivesIfNeeded() {
        if (!loaded) {
            loaded = true
            loadDrives()
        }
    }

    fun loadDrives() {
        val config = ServiceLocator.currentConfig ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _drives.value = ServiceLocator.apiClient.getDrives(config)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "주행 기록 로드 실패"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectDrive(id: Int?) {
        _selectedDriveId.value = id
    }

    fun clearSelection() {
        _selectedDriveId.value = null
    }
}
