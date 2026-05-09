package com.soooool.matedash.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ElectricCar
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soooool.matedash.ServiceLocator
import com.soooool.matedash.getPlatform

private enum class SettingsRoute {
    CONNECTION,
    TESLA_API,
    LIVE_ACTIVITY,
    DISPLAY,
    NAVIGATION,
    SHARE,
}

@Composable
fun SettingsScreen(onDisconnect: () -> Unit) {
    var currentRoute by remember { mutableStateOf<SettingsRoute?>(null) }

    when (currentRoute) {
        null -> SettingsMenuScreen(
            onNavigate = { currentRoute = it },
            onDisconnect = onDisconnect,
        )
        SettingsRoute.CONNECTION -> ConnectionSettingsScreen(
            onBack = { currentRoute = null },
            onDisconnect = onDisconnect,
        )
        SettingsRoute.TESLA_API -> TeslaApiSettingsScreen(
            onBack = { currentRoute = null },
        )
        SettingsRoute.LIVE_ACTIVITY -> LiveActivitySettingsScreen(
            onBack = { currentRoute = null },
        )
        SettingsRoute.DISPLAY -> DisplaySettingsScreen(
            onBack = { currentRoute = null },
        )
        SettingsRoute.NAVIGATION -> NavigationSettingsScreen(
            onBack = { currentRoute = null },
        )
        SettingsRoute.SHARE -> ShareSettingsScreen(
            onBack = { currentRoute = null },
        )
    }
}

@Composable
private fun SettingsMenuScreen(
    onNavigate: (SettingsRoute) -> Unit,
    onDisconnect: () -> Unit,
) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val isIos = getPlatform().name.lowercase().contains("ios")
    val teslaConfig by ServiceLocator.teslaConfigFlow.collectAsState()
    val teslaConnected = teslaConfig != null && teslaConfig!!.accessToken.isNotBlank()
    val tmConfig by ServiceLocator.currentConfigFlow.collectAsState()
    val teslaMateConnected = tmConfig != null
    val settings by ServiceLocator.settingsFlow.collectAsState()
    val mqttState by ServiceLocator.repository.mqttState.collectAsState()
    val mqttActive = settings.mqttEnabled &&
        mqttState == com.soooool.matedash.data.mqtt.MqttConnectionState.CONNECTED

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 16.dp)
            .padding(top = statusBarTop)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(24.dp))

        Text(
            text = "설정",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
        )

        Spacer(Modifier.height(24.dp))
        SectionTitle("연결")
        Spacer(Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg, RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            SettingsMenuItem(
                icon = Icons.Filled.Cloud,
                title = "TeslaMate",
                description = "서버 + MQTT 실시간 갱신",
                onClick = { onNavigate(SettingsRoute.CONNECTION) },
                statusText = when {
                    teslaMateConnected && mqttActive -> "연결됨 · MQTT"
                    teslaMateConnected -> "연결됨"
                    else -> null
                },
                statusColor = BatteryGreen,
            )
            HorizontalDivider(color = Color(0xFF2C2C2E), thickness = 0.5.dp)
            SettingsMenuItem(
                icon = Icons.Filled.ElectricCar,
                title = "Tesla Fleet API",
                description = "Tesla 계정 연동 및 가상 키 등록",
                onClick = { onNavigate(SettingsRoute.TESLA_API) },
                statusText = if (teslaConnected) "연결됨" else null,
                statusColor = BatteryGreen,
            )
        }

        Spacer(Modifier.height(24.dp))
        SectionTitle("기능")
        Spacer(Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg, RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            if (isIos) {
                SettingsMenuItem(
                    icon = Icons.Filled.Notifications,
                    title = "Live Activity",
                    description = "잠금화면 및 Dynamic Island 표시",
                    onClick = { onNavigate(SettingsRoute.LIVE_ACTIVITY) },
                )
                HorizontalDivider(color = Color(0xFF2C2C2E), thickness = 0.5.dp)
            }
            SettingsMenuItem(
                icon = Icons.Filled.Tune,
                title = "표시 설정",
                description = "지도, 갱신 주기, 단위",
                onClick = { onNavigate(SettingsRoute.DISPLAY) },
            )
            HorizontalDivider(color = Color(0xFF2C2C2E), thickness = 0.5.dp)
            SettingsMenuItem(
                icon = Icons.Filled.Navigation,
                title = "내비게이션",
                description = "TMap 링크 추출, 차량 내비 전송",
                onClick = { onNavigate(SettingsRoute.NAVIGATION) },
            )
            if (isIos) {
                HorizontalDivider(color = Color(0xFF2C2C2E), thickness = 0.5.dp)
                SettingsMenuItem(
                    icon = Icons.Filled.Share,
                    title = "공유 확장",
                    description = "다른 앱에서 공유 수신",
                    onClick = { onNavigate(SettingsRoute.SHARE) },
                )
            }
        }

        Spacer(Modifier.height(100.dp))
    }
}
