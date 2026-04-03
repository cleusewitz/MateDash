package com.soooool.matedash.ui.charging

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soooool.matedash.data.api.ChargeDto
import kotlin.math.abs
import kotlin.math.roundToInt

private val DarkBg = Color(0xFF0B0B0B)
private val CardBg = Color(0xFF1A1A1A)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFF8E8E93)
private val ChargingBlue = Color(0xFF00C7FF)
private val BatteryGreen = Color(0xFF34C759)
private val BatteryYellow = Color(0xFFFFCC00)
private val TeslaRed = Color(0xFFE31937)
private val CostGold = Color(0xFFFFD60A)

private fun Double.fmt1(): String {
    val rounded = (this * 10).roundToInt()
    return "${rounded / 10}.${abs(rounded % 10)}"
}

private fun Int.withComma(): String {
    val s = toString()
    val sb = StringBuilder()
    for (i in s.indices) {
        if (i > 0 && (s.length - i) % 3 == 0) sb.append(',')
        sb.append(s[i])
    }
    return sb.toString()
}

private fun Double.fmt0Comma() = roundToInt().withComma()

private fun formatDate(dateStr: String?): String {
    if (dateStr == null) return ""
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

private fun chargeEfficiency(added: Double?, used: Double?): String? {
    if (added == null || used == null || used <= 0.0) return null
    val pct = (added / used * 100).roundToInt()
    return "$pct%"
}

@Composable
fun ChargingScreen() {
    val vm = viewModel { ChargingViewModel() }
    val charges by vm.charges.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val errorMsg by vm.errorMessage.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Spacer(Modifier.height(16.dp)) }

        item {
            Text(
                text = "충전 기록",
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
                Text(
                    text = "⚠️ $errorMsg",
                    color = TeslaRed,
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            item { ChargeSummaryCard(charges) }
            items(charges) { charge -> ChargeItem(charge) }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun ChargeSummaryCard(charges: List<ChargeDto>) {
    val totalCount = charges.size
    val totalEnergyKwh = charges.sumOf { it.chargeEnergyAdded ?: 0.0 }
    val totalCost = charges.mapNotNull { it.cost }.takeIf { it.isNotEmpty() }?.sum()
    val totalMinutes = charges.sumOf { it.durationMin ?: 0 }
    val avgMinutes = if (totalCount > 0) totalMinutes / totalCount else 0
    val avgEnergy = if (totalCount > 0) totalEnergyKwh / totalCount else 0.0
    val avgHours = avgMinutes / 60
    val avgRemainMin = avgMinutes % 60
    val avgTimeStr = if (avgHours > 0) "${avgHours}h ${avgRemainMin}m" else "${avgRemainMin}m"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(20.dp))
            .padding(20.dp),
    ) {
        Text(
            text = "총 ${totalCount}회 충전 · ${totalEnergyKwh.fmt1()} kWh",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ChargeSummaryItem("평균 충전시간", avgTimeStr)
            ChargeSummaryItem("평균 충전량", "${avgEnergy.fmt1()} kWh")
            if (totalCost != null) {
                ChargeSummaryItem("총 비용", "₩${totalCost.fmt0Comma()}", CostGold)
            }
        }
    }
}

@Composable
private fun ChargeSummaryItem(label: String, value: String, color: Color = TextPrimary) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = color)
        Text(label, fontSize = 11.sp, color = TextSecondary)
    }
}

@Composable
private fun ChargeItem(charge: ChargeDto) {
    val energyAdded = charge.chargeEnergyAdded ?: 0.0
    val startLevel = charge.batteryDetails?.startBatteryLevel ?: 0
    val endLevel = charge.batteryDetails?.endBatteryLevel ?: 0
    val durationStr = charge.durationStr ?: "${charge.durationMin ?: 0}분"
    val address = charge.address ?: "알 수 없는 위치"
    val efficiency = chargeEfficiency(charge.chargeEnergyAdded, charge.chargeEnergyUsed)
    val tempStr = charge.outsideTempAvg?.let { "${it.fmt1()}°C" }
    val odometerStr = charge.odometer?.let { "${it.fmt0Comma()} km" }
    val costStr = charge.cost?.let { "₩${it.fmt0Comma()}" }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // 위치 + 주행거리
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = address,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 2,
                modifier = Modifier.weight(1f),
            )
            if (odometerStr != null) {
                Text(
                    text = odometerStr,
                    fontSize = 11.sp,
                    color = TextSecondary,
                )
            }
        }

        Spacer(Modifier.height(3.dp))

        // 날짜 + 외기 온도
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatDate(charge.startDate),
                fontSize = 12.sp,
                color = TextSecondary,
            )
            if (tempStr != null) {
                Text(
                    text = "🌡 $tempStr",
                    fontSize = 12.sp,
                    color = TextSecondary,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // 주요 스탯
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ChargeStatChip(label = "충전량", value = "+${energyAdded.fmt1()} kWh", color = BatteryGreen)
            ChargeStatChip(label = "배터리", value = "$startLevel→$endLevel%", color = ChargingBlue)
            ChargeStatChip(label = "시간", value = durationStr, color = TextSecondary)
            if (efficiency != null) {
                ChargeStatChip(label = "효율", value = efficiency, color = BatteryYellow)
            }
        }

        // 비용 (있을 때만)
        if (costStr != null) {
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .background(CostGold.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(
                    text = "충전 비용 $costStr",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CostGold,
                )
            }
        }
    }
}

@Composable
private fun ChargeStatChip(label: String, value: String, color: Color) {
    Column {
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = color)
        Text(label, fontSize = 11.sp, color = TextSecondary)
    }
}
