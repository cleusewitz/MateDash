package com.soooool.matedash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soooool.matedash.ui.connection.ConnectionScreen
import com.soooool.matedash.ui.main.MainScreen
import com.soooool.matedash.ui.theme.AppTheme

private enum class Screen { CONNECTION, MAIN }

private class AppViewModel : ViewModel() {
    var screen by mutableStateOf(
        if (ServiceLocator.currentConfig != null) Screen.MAIN else Screen.CONNECTION
    )
        private set

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
