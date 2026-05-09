package com.soooool.matedash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soooool.matedash.data.persistence.clearApiConfig
import com.soooool.matedash.ui.connection.ConnectionScreen
import com.soooool.matedash.ui.main.MainScreen
import com.soooool.matedash.ui.theme.AppTheme
import kotlinx.coroutines.launch

private enum class Screen { CONNECTION, MAIN }

private class AppViewModel : ViewModel() {
    var screen by mutableStateOf(Screen.CONNECTION)
        private set

    init {
        ServiceLocator.loadSavedConfigs()
        val savedConfig = ServiceLocator.currentConfig
        val savedTesla = ServiceLocator.teslaApiConfig
        when {
            savedConfig != null -> {
                println("[MateDash] 저장된 TeslaMate 설정으로 자동 연결 시도: ${savedConfig.baseUrl}")
                screen = Screen.MAIN
                viewModelScope.launch {
                    try {
                        ServiceLocator.apiClient.getCarStatus(savedConfig)
                        ServiceLocator.repository.startPolling(savedConfig, ServiceLocator.appSettings.pollIntervalSeconds * 1000L)
                        ServiceLocator.applyMqttSettings()
                        println("[MateDash] 자동 연결 성공 (TeslaMate)")
                    } catch (e: Exception) {
                        println("[MateDash] 자동 연결 실패: ${e.message}")
                        ServiceLocator.currentConfig = null
                        clearApiConfig()
                        // TeslaMate 실패해도 Fleet API 있으면 그쪽으로 fallback
                        if (ServiceLocator.teslaApiConfig != null) {
                            ServiceLocator.startFullVehiclePolling()
                        } else {
                            screen = Screen.CONNECTION
                        }
                    }
                }
            }
            savedTesla != null && savedTesla.accessToken.isNotBlank() -> {
                println("[MateDash] TeslaMate 없음, Tesla Fleet API 단독 모드로 진입")
                screen = Screen.MAIN
                ServiceLocator.startFullVehiclePolling()
            }
        }
    }

    fun navigateTo(s: Screen) { screen = s }
}

@Composable
fun App() {
    AppTheme {
        val vm = viewModel { AppViewModel() }

        when (vm.screen) {
            Screen.CONNECTION -> ConnectionScreen(
                onConnected = { vm.navigateTo(Screen.MAIN) }
            )
            Screen.MAIN -> MainScreen(
                onDisconnect = { vm.navigateTo(Screen.CONNECTION) }
            )
        }
    }
}
