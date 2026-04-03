package com.soooool.matedash.ui.dashboard

import androidx.lifecycle.ViewModel
import com.soooool.matedash.ServiceLocator
import com.soooool.matedash.data.model.CarState
import com.soooool.matedash.data.repository.ApiConnectionState
import kotlinx.coroutines.flow.StateFlow

class DashboardViewModel : ViewModel() {
    val carState: StateFlow<CarState> = ServiceLocator.repository.carState
    val connectionState: StateFlow<ApiConnectionState> = ServiceLocator.repository.connectionState
    val errorMessage: StateFlow<String?> = ServiceLocator.repository.errorMessage

    fun disconnect(onDisconnected: () -> Unit) {
        ServiceLocator.repository.stopPolling()
        onDisconnected()
    }
}
