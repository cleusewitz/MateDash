package com.soooool.matedash.ui.driving

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soooool.matedash.data.api.DriveDto
import kotlin.math.abs
import kotlin.math.roundToInt

private val DarkBg = Color(0xFF0B0B0B)
private val CardBg = Color(0xFF1A1A1A)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFF8E8E93)
private val ChargingBlue = Color(0xFF00C7FF)
private val BatteryGreen = Color(0xFF34C759)
private val TeslaRed = Color(0xFFE31937)

private fun Int.withComma(): String {
    val neg = this < 0
    val s = (if (neg) -this else this).toString()
    val sb = StringBuilder()
    for (i in s.indices) {
        if (i > 0 && (s.length - i) % 3 == 0) sb.append(',')
        sb.append(s[i])
    }
    return if (neg) "-$sb" else sb.toString()
}

private fun Double.fmt1(): String {
    val rounded = (this * 10).roundToInt()
    return "${(rounded / 10).withComma()}.${abs(rounded % 10)}"
}

private fun Double.fmt0() = roundToInt().withComma()

private fun formatDate(dateStr: String?): String {
    if (dateStr == null) return ""
    // "2026-04-03T07:29:59+09:00" -> "2026-04-03 07:29"
    return try {
        val t = dateStr.indexOf('T')
        if (t < 0) return dateStr
        val datePart = dateStr.substring(0, t)
        val timePart = dateStr.substring(t + 1, minOf(t + 6, dateStr.length))
        "$datePart $timePart"
    } catch (_: Exception) {
        dateStr
    }
}

@Composable
fun DrivingScreen() {
    val vm = viewModel { DrivingViewModel() }
    val drives by vm.drives.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val errorMsg by vm.errorMessage.collectAsState()

    // 화면 진입 시 데이터 로드
    LaunchedEffect(Unit) {
        vm.loadDrivesIfNeeded()
        if (drives.isEmpty() && !isLoading && errorMsg != null) {
            vm.loadDrives()
        }
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = statusBarTop + 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {

        item {
            Text(
                text = "주행 기록",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
        }

        if (isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = TeslaRed, modifier = Modifier.size(32.dp))
                }
            }
        } else if (errorMsg != null) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "⚠️ $errorMsg",
                        color = TeslaRed,
                        fontSize = 13.sp,
                    )
                    Text(
                        text = "다시 시도",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ChargingBlue,
                        modifier = Modifier
                            .clickable { vm.loadDrives() }
                            .padding(8.dp),
                    )
                }
            }
        } else {
            // 요약 카드 (최근 기록 기반)
            item {
                DriveSummaryCard(drives)
            }

            items(drives) { drive ->
                DriveItem(drive)
            }
        }

        item { Spacer(Modifier.height(100.dp)) }
    }
}

@Composable
private fun DriveSummaryCard(drives: List<DriveDto>) {
    val totalDistanceKm = drives.sumOf { it.odometerDetails?.odometerDistance ?: 0.0 }
    val totalMinutes = drives.sumOf { it.durationMin ?: 0 }
    val totalEnergyKwh = drives.sumOf { it.energyConsumedNet ?: 0.0 }
    val avgConsumption = if (totalDistanceKm > 0) (totalEnergyKwh * 1000 / totalDistanceKm) else 0.0

    val totalHours = totalMinutes / 60
    val remainMin = totalMinutes % 60
    val timeStr = if (totalHours > 0) "${totalHours}h ${remainMin}m" else "${remainMin}m"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(20.dp))
            .padding(20.dp),
    ) {
        Text("최근 ${drives.size}회 주행", fontSize = 12.sp, color = TextSecondary)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "${totalDistanceKm.fmt1()} km",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SummaryItem("총 시간", timeStr)
            SummaryItem("총 에너지", "${totalEnergyKwh.fmt1()} kWh")
            SummaryItem("평균 소비", "${avgConsumption.fmt0()} Wh/km")
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Text(label, fontSize = 11.sp, color = TextSecondary)
    }
}

@Composable
private fun DriveItem(drive: DriveDto) {
    val distanceKm = drive.odometerDetails?.odometerDistance ?: 0.0
    val energyKwh = drive.energyConsumedNet ?: 0.0
    val durationStr = drive.durationStr ?: "${drive.durationMin ?: 0}분"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val from = drive.startAddress ?: "출발지"
                val to = drive.endAddress ?: "도착지"
                Text(
                    text = "$from → $to",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 2,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        // 출발/도착 시간
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "출발 ${formatDate(drive.startDate)}",
                fontSize = 12.sp,
                color = TextSecondary,
            )
            Text(
                text = " → ",
                fontSize = 12.sp,
                color = TextSecondary,
            )
            Text(
                text = "도착 ${formatDate(drive.endDate)}",
                fontSize = 12.sp,
                color = TextSecondary,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            StatChip(label = "거리", value = "${distanceKm.fmt1()} km", color = ChargingBlue)
            StatChip(label = "에너지", value = "${energyKwh.fmt1()} kWh", color = BatteryGreen)
            StatChip(label = "시간", value = durationStr, color = TextSecondary)
            // 배터리 소모
            val startBat = drive.batteryDetails?.startBatteryLevel
            val endBat = drive.batteryDetails?.endBatteryLevel
            if (startBat != null && endBat != null) {
                StatChip(label = "배터리", value = "${startBat}→${endBat}% (-${startBat - endBat}%)", color = TeslaRed)
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, color: Color) {
    Column {
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = color)
        Text(label, fontSize = 11.sp, color = TextSecondary)
    }
}
