package com.soooool.matedash.ui.tesla

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

private val DarkBg = Color(0xFF0B0B0B)
private val CardBg = Color(0xFF1A1A1A)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFF8E8E93)
private val TeslaRed = Color(0xFFE31937)
private val BatteryGreen = Color(0xFF34C759)
private val ChargingBlue = Color(0xFF00C7FF)
private val WarningYellow = Color(0xFFFFCC00)

@Composable
fun TeslaScreen() {
    val vm = viewModel { TeslaViewModel() }
    val state by vm.uiState.collectAsState()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val teslaConfig by com.soooool.matedash.ServiceLocator.teslaConfigFlow.collectAsState()
    val currentConfigExists = teslaConfig != null

    LaunchedEffect(currentConfigExists) {
        println("[MateDash] TeslaScreen LaunchedEffect: currentConfigExists=$currentConfigExists")
        vm.refreshConfig()
        if (currentConfigExists) {
            val config = com.soooool.matedash.ServiceLocator.teslaApiConfig
            println("[MateDash] LaunchedEffect: vehicleId=${config?.vehicleId}")
            if (config != null && config.vehicleId != 0L) {
                vm.loadVehicleData()
            } else {
                vm.loadVehicles()
            }
        }
    }

    if (!state.isConfigured) {
        NotConfiguredView()
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = statusBarTop + 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 헤더
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Tesla API",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
                IconButton(
                    onClick = {
                        val config = com.soooool.matedash.ServiceLocator.teslaApiConfig
                        if (config != null && config.vehicleId != 0L) {
                            vm.loadVehicleData()
                        } else {
                            vm.loadVehicles()
                        }
                    },
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = "새로고침", tint = TextSecondary)
                }
            }
        }

        // 에러 메시지
        if (state.error != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TeslaRed.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                ) {
                    Text(state.error ?: "", fontSize = 13.sp, color = TeslaRed)
                }
            }
        }

        // 명령 결과
        if (state.commandResult != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BatteryGreen.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                ) {
                    Text(state.commandResult ?: "", fontSize = 13.sp, color = BatteryGreen)
                }
            }
        }

        // 로딩
        if (state.isLoading) {
            item {
                Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TeslaRed, modifier = Modifier.size(32.dp))
                }
            }
        }

        // 차량 목록 (vehicleId 미설정 시)
        val config = com.soooool.matedash.ServiceLocator.teslaApiConfig
        if (config != null && config.vehicleId == 0L && state.vehicles.isNotEmpty()) {
            item {
                Text("차량 선택", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }
            items(state.vehicles) { vehicle ->
                VehicleListItem(vehicle) { vm.selectVehicle(vehicle) }
            }
        }

        // 차량 데이터
        val data = state.vehicleData
        if (data != null) {
            // 차량 상태 헤더
            item { VehicleStatusHeader(data) }

            // 배터리 / 충전
            item { BatterySection(data) }

            // 차량 제어
            item {
                Text("차량 제어", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }
            item { ControlGrid(vm) }

            // 온도
            item { ClimateSection(data) }
        }

        item { Spacer(Modifier.height(100.dp)) }
    }
}

@Composable
private fun NotConfiguredView() {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(top = statusBarTop + 48.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Tesla API", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(24.dp))
            Text(
                text = "설정에서 Tesla API를\n구성해주세요",
                fontSize = 15.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun VehicleListItem(vehicle: com.soooool.matedash.data.api.TeslaVehicle, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(vehicle.displayName.ifEmpty { "Tesla" }, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(vehicle.vin, fontSize = 12.sp, color = TextSecondary)
        }
        Text(
            text = vehicle.state.replaceFirstChar { it.uppercase() },
            fontSize = 13.sp,
            color = when (vehicle.state) {
                "online" -> BatteryGreen
                "asleep" -> TextSecondary
                else -> WarningYellow
            },
        )
    }
}

@Composable
private fun VehicleStatusHeader(data: com.soooool.matedash.data.api.TeslaVehicleData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = data.displayName.ifEmpty { "Tesla" },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = data.vehicleState?.carVersion?.substringBefore(" ") ?: "",
                fontSize = 12.sp,
                color = TextSecondary,
            )
        }
        Text(
            text = data.state.replaceFirstChar { it.uppercase() },
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = when (data.state) {
                "online" -> BatteryGreen
                "driving" -> ChargingBlue
                "charging" -> WarningYellow
                else -> TextSecondary
            },
        )
    }
}

@Composable
private fun BatterySection(data: com.soooool.matedash.data.api.TeslaVehicleData) {
    val charge = data.chargeState ?: return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("배터리 / 충전", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ChargingBlue)
        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

        InfoRow("배터리", "${charge.batteryLevel}%")
        InfoRow("사용 가능", "${charge.usableBatteryLevel}%")
        InfoRow("충전 상한", "${charge.chargeLimitSoc}%")
        InfoRow("예상 주행거리", "${(charge.estBatteryRange * 1.60934).toInt()} km")
        InfoRow("충전 상태", charge.chargingState.ifEmpty { "-" })

        if (charge.chargingState == "Charging") {
            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
            InfoRow("충전 전력", "${charge.chargerPower} kW")
            InfoRow("충전 전압", "${charge.chargerVoltage} V")
            InfoRow("추가 에너지", "${charge.chargeEnergyAdded} kWh")
            if (charge.timeToFullCharge > 0) {
                val h = charge.timeToFullCharge.toInt()
                val m = ((charge.timeToFullCharge - h) * 60).toInt()
                InfoRow("완충 예상", if (h > 0) "${h}시간 ${m}분" else "${m}분")
            }
        }
    }
}

@Composable
private fun ClimateSection(data: com.soooool.matedash.data.api.TeslaVehicleData) {
    val climate = data.climateState ?: return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("공조", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ChargingBlue)
        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

        InfoRow("실내 온도", climate.insideTemp?.let { "${it}°C" } ?: "-")
        InfoRow("실외 온도", climate.outsideTemp?.let { "${it}°C" } ?: "-")
        InfoRow("공조 상태", if (climate.isClimateOn) "켜짐" else "꺼짐")
        InfoRow("설정 온도", "${climate.driverTempSetting}°C")
    }
}

@Composable
private fun ControlGrid(vm: TeslaViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ControlButton(
                icon = Icons.Filled.Power,
                label = "Wake Up",
                color = BatteryGreen,
                modifier = Modifier.weight(1f),
            ) { vm.wakeUp() }
            ControlButton(
                icon = Icons.Filled.AcUnit,
                label = "공조 켜기",
                color = ChargingBlue,
                modifier = Modifier.weight(1f),
            ) { vm.sendCommand("auto_conditioning_start", "공조 켜기") }
            ControlButton(
                icon = Icons.Filled.AcUnit,
                label = "공조 끄기",
                color = TextSecondary,
                modifier = Modifier.weight(1f),
            ) { vm.sendCommand("auto_conditioning_stop", "공조 끄기") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ControlButton(
                icon = Icons.Filled.Lock,
                label = "잠금",
                color = BatteryGreen,
                modifier = Modifier.weight(1f),
            ) { vm.sendCommand("door_lock", "잠금") }
            ControlButton(
                icon = Icons.Filled.LockOpen,
                label = "잠금 해제",
                color = WarningYellow,
                modifier = Modifier.weight(1f),
            ) { vm.sendCommand("door_unlock", "잠금 해제") }
            ControlButton(
                icon = Icons.Filled.FlashOn,
                label = "라이트",
                color = WarningYellow,
                modifier = Modifier.weight(1f),
            ) { vm.sendCommand("flash_lights", "라이트") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ControlButton(
                icon = Icons.Filled.BatteryChargingFull,
                label = "충전 시작",
                color = ChargingBlue,
                modifier = Modifier.weight(1f),
            ) { vm.sendCommand("charge_start", "충전 시작") }
            ControlButton(
                icon = Icons.Filled.BatteryChargingFull,
                label = "충전 중지",
                color = TeslaRed,
                modifier = Modifier.weight(1f),
            ) { vm.sendCommand("charge_stop", "충전 중지") }
            ControlButton(
                icon = Icons.Filled.VolumeUp,
                label = "경적",
                color = TeslaRed,
                modifier = Modifier.weight(1f),
            ) { vm.sendCommand("honk_horn", "경적") }
        }
    }
}

@Composable
private fun ControlButton(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .background(CardBg, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 11.sp, color = TextSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 14.sp, color = TextSecondary)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
    }
}
