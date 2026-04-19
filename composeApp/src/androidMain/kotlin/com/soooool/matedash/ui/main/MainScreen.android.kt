package com.soooool.matedash.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ElectricCar
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soooool.matedash.ui.charging.ChargingScreen
import com.soooool.matedash.ui.dashboard.DashboardScreen
import com.soooool.matedash.ui.driving.DrivingScreen
import com.soooool.matedash.ui.settings.SettingsScreen
import com.soooool.matedash.ui.tesla.TeslaScreen
import kotlinx.coroutines.launch

private val NavBg = Color(0xFF121212)
private val TeslaRed = Color(0xFFE31937)
private val TextSecondary = Color(0xFF8E8E93)

private enum class Tab(val label: String, val icon: ImageVector) {
    DASHBOARD("대시보드", Icons.Filled.Speed),
    DRIVING("주행", Icons.Filled.DirectionsCar),
    CHARGING("충전", Icons.Filled.Bolt),
    TESLA("Tesla", Icons.Filled.ElectricCar),
    SETTINGS("설정", Icons.Filled.Settings),
}

@Composable
actual fun MainScreen(onDisconnect: () -> Unit) {
    val tabs = Tab.entries
    val pagerState = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color(0xFF0B0B0B),
        bottomBar = {
            NavigationBar(containerColor = NavBg) {
                tabs.forEachIndexed { index, tab ->
                    val selected = pagerState.currentPage == index
                    NavigationBarItem(
                        selected = selected,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                modifier = Modifier.size(22.dp),
                            )
                        },
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
        val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding()),
                beyondViewportPageCount = 0,
            ) { page ->
                when (tabs[page]) {
                    Tab.DASHBOARD -> DashboardScreen()
                    Tab.DRIVING -> DrivingScreen()
                    Tab.CHARGING -> ChargingScreen()
                    Tab.TESLA -> TeslaScreen()
                    Tab.SETTINGS -> SettingsScreen(onDisconnect = onDisconnect)
                }
            }

            // 스테이터스 바 그라데이션 오버레이
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(statusBarHeight + 20.dp)
                    .align(Alignment.TopStart)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0B0B0B),
                                Color(0xCC0B0B0B),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
        }
    }
}
