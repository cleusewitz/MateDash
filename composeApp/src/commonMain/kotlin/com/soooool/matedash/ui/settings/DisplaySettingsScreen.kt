package com.soooool.matedash.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soooool.matedash.ServiceLocator
import com.soooool.matedash.data.model.DistanceUnit
import com.soooool.matedash.data.model.TemperatureUnit

@Composable
internal fun DisplaySettingsScreen(onBack: () -> Unit) {
    val settings by ServiceLocator.settingsFlow.collectAsState()

    SettingsDetailScaffold(title = "표시 설정", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg, RoundedCornerShape(20.dp))
                .padding(20.dp),
        ) {
            SettingSwitch(
                label = "지도 표시",
                description = "대시보드에 현재 위치 지도 표시",
                checked = settings.mapEnabled,
                onCheckedChange = { ServiceLocator.appSettings = settings.copy(mapEnabled = it) },
            )

            Spacer(Modifier.height(24.dp))

            Text("데이터 갱신 주기", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RegionChip("15초", "15", settings.pollIntervalSeconds.toString()) {
                    ServiceLocator.appSettings = settings.copy(pollIntervalSeconds = 15)
                }
                RegionChip("30초", "30", settings.pollIntervalSeconds.toString()) {
                    ServiceLocator.appSettings = settings.copy(pollIntervalSeconds = 30)
                }
                RegionChip("60초", "60", settings.pollIntervalSeconds.toString()) {
                    ServiceLocator.appSettings = settings.copy(pollIntervalSeconds = 60)
                }
            }

            Spacer(Modifier.height(24.dp))

            Text("거리 단위", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RegionChip("km", DistanceUnit.KM.name, settings.distanceUnit.name) {
                    ServiceLocator.appSettings = settings.copy(distanceUnit = DistanceUnit.KM)
                }
                RegionChip("miles", DistanceUnit.MILES.name, settings.distanceUnit.name) {
                    ServiceLocator.appSettings = settings.copy(distanceUnit = DistanceUnit.MILES)
                }
            }

            Spacer(Modifier.height(24.dp))

            Text("온도 단위", fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RegionChip("°C", TemperatureUnit.CELSIUS.name, settings.temperatureUnit.name) {
                    ServiceLocator.appSettings = settings.copy(temperatureUnit = TemperatureUnit.CELSIUS)
                }
                RegionChip("°F", TemperatureUnit.FAHRENHEIT.name, settings.temperatureUnit.name) {
                    ServiceLocator.appSettings = settings.copy(temperatureUnit = TemperatureUnit.FAHRENHEIT)
                }
            }
        }

    }
}
