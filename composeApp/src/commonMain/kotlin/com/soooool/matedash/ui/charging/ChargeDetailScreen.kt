package com.soooool.matedash.ui.charging

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soooool.matedash.data.api.ChargeDto
import kotlin.math.abs
import kotlin.math.roundToInt

private val DarkBg = Color(0xFF0B0B0B)
private val CardBg = Color(0xFF1A1A1A)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFF8E8E93)
private val ChargingBlue = Color(0xFF00C7FF)
private val BatteryGreen = Color(0xFF34C759)
private val CostGold = Color(0xFFFFD60A)

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

private fun formatDateTime(dateStr: String?): String {
    if (dateStr == null) return "-"
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
fun ChargeDetailScreen(charge: ChargeDto, onClose: () -> Unit) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val energyAdded = charge.chargeEnergyAdded
    val durationMin = charge.durationMin
    val avgPowerText: String = if (energyAdded != null && durationMin != null && durationMin > 0) {
        "${((energyAdded * 60.0) / durationMin).fmt1()} kW"
    } else "-"

    val cost = charge.cost
    val perKwhText: String = if (cost != null && energyAdded != null && energyAdded > 0) {
        "${(cost / energyAdded).fmt0()} 원/kWh"
    } else "-"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = statusBarTop + 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 헤더
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = CardBg,
                        contentColor = TextPrimary,
                    ),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "닫기", modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "충전 상세",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = charge.address ?: "위치 없음",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        maxLines = 2,
                    )
                }
            }
        }

        // 충전 요약
        item {
            DetailCard("충전 요약", accent = ChargingBlue) {
                DetailRow("소요시간", charge.durationStr ?: charge.durationMin?.let { "${it}분" } ?: "-")
                DetailRow("추가 에너지", energyAdded?.let { "${it.fmt1()} kWh" } ?: "-")
                DetailRow("평균 충전 출력", avgPowerText)
                if (charge.chargeEnergyUsed != null) {
                    DetailRow("사용 에너지", "${charge.chargeEnergyUsed.fmt1()} kWh")
                }
                DetailRow("시작", formatDateTime(charge.startDate))
                DetailRow("종료", formatDateTime(charge.endDate))
            }
        }

        // 배터리
        item {
            DetailCard("배터리", accent = BatteryGreen) {
                val startBat = charge.batteryDetails?.startBatteryLevel
                val endBat = charge.batteryDetails?.endBatteryLevel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BatteryPill(label = "시작", percent = startBat)
                    Text("→", fontSize = 20.sp, color = TextSecondary)
                    BatteryPill(label = "종료", percent = endBat)
                }
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                val diff = if (startBat != null && endBat != null) "+${endBat - startBat}%p" else "-"
                DetailRow("증가", diff)
            }
        }

        // 비용 / 환경
        item {
            DetailCard("비용 / 환경", accent = CostGold) {
                DetailRow("비용", cost?.let { "${it.fmt0()} 원" } ?: "-")
                DetailRow("kWh 당", perKwhText)
                DetailRow("외기온 평균", charge.outsideTempAvg?.let { "${it.fmt1()} °C" } ?: "-")
                DetailRow("주행계", charge.odometer?.let { "${it.fmt0()} km" } ?: "-")
            }
        }

        item { Spacer(Modifier.height(100.dp)) }
    }
}

@Composable
private fun DetailCard(title: String, accent: Color, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = accent,
        )
        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
        content()
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 14.sp, color = TextSecondary)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
    }
}

@Composable
private fun BatteryPill(label: String, percent: Int?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(14.dp))
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(label, fontSize = 11.sp, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Text(
            text = percent?.let { "$it%" } ?: "-",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
        )
    }
}
