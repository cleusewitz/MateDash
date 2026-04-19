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
        if (savedConfig != null) {
            println("[MateDash] 저장된 TeslaMate 설정으로 자동 연결 시도: ${savedConfig.baseUrl}")
            screen = Screen.MAIN
            viewModelScope.launch {
                try {
                    ServiceLocator.apiClient.getCarStatus(savedConfig)
                    ServiceLocator.repository.startPolling(savedConfig, ServiceLocator.appSettings.pollIntervalSeconds * 1000L)
                    println("[MateDash] 자동 연결 성공")
                } catch (e: Exception) {
                    println("[MateDash] 자동 연결 실패: ${e.message}")
                    // 401 등 인증 실패 시 저장된 설정 초기화하고 연결 화면으로
                    ServiceLocator.currentConfig = null
                    clearApiConfig()
                    screen = Screen.CONNECTION
                }
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
