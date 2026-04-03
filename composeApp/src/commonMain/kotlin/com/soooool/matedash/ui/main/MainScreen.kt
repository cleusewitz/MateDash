package com.soooool.matedash.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soooool.matedash.ui.charging.ChargingScreen
import com.soooool.matedash.ui.dashboard.DashboardScreen
import com.soooool.matedash.ui.driving.DrivingScreen
import com.soooool.matedash.ui.settings.SettingsScreen
import com.soooool.matedash.ui.trip.TripScreen

private val NavBg = Color(0xFF121212)
private val TeslaRed = Color(0xFFE31937)
private val TextSecondary = Color(0xFF8E8E93)

private enum class Tab(val label: String, val icon: String) {
    DASHBOARD("대시보드", "🎛️"),
    DRIVING("주행", "🚗"),
    CHARGING("충전", "⚡"),
    TRIP("트립", "🗺️"),
    SETTINGS("설정", "⚙️"),
}

private class MainViewModel : ViewModel() {
    var selectedTab by mutableStateOf(Tab.DASHBOARD)
        private set

    fun selectTab(tab: Tab) { selectedTab = tab }
}

@Composable
fun MainScreen(onDisconnect: () -> Unit) {
    val vm = viewModel { MainViewModel() }

    Scaffold(
        containerColor = Color(0xFF0B0B0B),
        bottomBar = {
            NavigationBar(containerColor = NavBg) {
                Tab.entries.forEach { tab ->
                    val selected = vm.selectedTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { vm.selectTab(tab) },
                        icon = { Text(tab.icon, fontSize = 20.sp) },
                        label = { Text(tab.label, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TeslaRed,
                            selectedTextColor = TeslaRed,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = Color.Transparent,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (vm.selectedTab) {
                Tab.DASHBOARD -> DashboardScreen()
                Tab.DRIVING -> DrivingScreen()
                Tab.CHARGING -> ChargingScreen()
                Tab.TRIP -> TripScreen()
                Tab.SETTINGS -> SettingsScreen(onDisconnect = onDisconnect)
            }
        }
    }
}
