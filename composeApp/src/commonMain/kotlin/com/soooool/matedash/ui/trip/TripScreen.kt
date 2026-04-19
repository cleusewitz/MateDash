package com.soooool.matedash.ui.trip

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import com.soooool.matedash.data.api.DriveDto
import kotlin.math.roundToInt

private val DarkBg      = Color(0xFF0B0B0B)
private val CardBg      = Color(0xFF1A1A1A)
private val AccentBlue  = Color(0xFF0A84FF)
private val AccentGreen = Color(0xFF34C759)
private val AccentRed   = Color(0xFFE31937)
private val TextPrimary   = Color.White
private val TextSecondary = Color(0xFF8E8E93)

@Composable
fun TripScreen() {
    val vm = viewModel { TripViewModel() }
    val stats by vm.stats.collectAsState()
    val recentDrives by vm.recentDrives.collectAsState()
    val isLoading by vm.isLoading.collectAsState()

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AccentRed)
        }
        return
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
                "주행 통계",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }

        // 기간별 카드 (오늘 / 이번 주 / 이번 달)
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PeriodCard("오늘",    stats.todayKm,   stats.todayDrives,   AccentBlue,  Modifier.weight(1f))
                PeriodCard("이번 주", stats.weeklyKm,  stats.weeklyDrives,  AccentGreen, Modifier.weight(1f))
                PeriodCard("이번 달", stats.monthlyKm, stats.monthlyDrives, AccentRed,   Modifier.weight(1f))
            }
        }

        // 누적 통계
        item { SectionLabel("누적 통계") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("총 주행 횟수", "${stats.totalDrives}회",    modifier = Modifier.weight(1f))
                StatCard("총 주행거리",  "${stats.totalKm.fmtKm()} km", modifier = Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("평균 주행거리", "${stats.avgKmPerDrive.fmtKm()} km", modifier = Modifier.weight(1f))
                StatCard("평균 효율",    "${stats.avgEfficiency.fmtEff()} km/kWh", modifier = Modifier.weight(1f))
            }
        }

        // 기록
        item { SectionLabel("기록") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("최고 속도",    "${stats.maxSpeed} km/h",          modifier = Modifier.weight(1f))
                StatCard("최장 주행",   "${stats.longestDriveKm.fmtKm()} km", modifier = Modifier.weight(1f))
            }
        }

        // 최근 주행 기록
        if (recentDrives.isNotEmpty()) {
            item { SectionLabel("최근 주행 기록") }
            items(recentDrives) { drive ->
                DriveHistoryCard(drive)
            }
        }

        item { Spacer(Modifier.height(100.dp)) }
    }
}

@Composable
private fun PeriodCard(
    label: String,
    km: Double,
    drives: Int,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(CardBg, RoundedCornerShape(20.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, fontSize = 11.sp, color = TextSecondary)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "${km.fmtKm()}",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor,
        )
        Text("km", fontSize = 10.sp, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Text("${drives}회", fontSize = 11.sp, color = TextSecondary)
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(CardBg, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Text(title, fontSize = 12.sp, color = TextSecondary)
        Spacer(Modifier.height(8.dp))
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
    )
}

@Composable
private fun DriveHistoryCard(drive: DriveDto) {
    val startTime = drive.startDate?.formatTime() ?: "-"
    val endTime = drive.endDate?.formatTime() ?: "-"
    val startAddr = drive.startAddress?.takeIf { it.isNotBlank() } ?: "알 수 없음"
    val endAddr = drive.endAddress?.takeIf { it.isNotBlank() } ?: "알 수 없음"
    val distance = drive.odometerDetails?.odometerDistance?.fmtKm() ?: "-"
    val startBattery = drive.batteryDetails?.startBatteryLevel
    val endBattery = drive.batteryDetails?.endBatteryLevel
    val batteryUsed = if (startBattery != null && endBattery != null) startBattery - endBattery else null
    val date = drive.startDate?.take(10) ?: ""

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 날짜 + 거리
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(date, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AccentBlue)
            Text("$distance km", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

        // 출발 → 도착
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("출발", fontSize = 10.sp, color = TextSecondary)
                Text(startTime, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                Text(startAddr, fontSize = 11.sp, color = TextSecondary, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("도착", fontSize = 10.sp, color = TextSecondary)
                Text(endTime, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                Text(endAddr, fontSize = 11.sp, color = TextSecondary, maxLines = 1)
            }
        }

        // 배터리 소모 + 주행시간
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (batteryUsed != null) {
                Row {
                    Text("배터리 ", fontSize = 12.sp, color = TextSecondary)
                    Text(
                        "${startBattery}% → ${endBattery}%",
                        fontSize = 12.sp,
                        color = TextPrimary,
                    )
                    Text(
                        " (-${batteryUsed}%)",
                        fontSize = 12.sp,
                        color = AccentRed,
                    )
                }
            }
            drive.durationStr?.let {
                Text(it, fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}

/** "2024-03-15T14:30:00Z" → "14:30" */
private fun String.formatTime(): String {
    val tIdx = indexOf('T')
    if (tIdx < 0) return this
    return substring(tIdx + 1).take(5)
}

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

private fun Double.fmtKm(): String {
    val r = (this * 10).roundToInt()
    val whole = (r / 10).withComma()
    return if (r % 10 == 0) whole else "$whole.${r % 10}"
}

private fun Double.fmtEff(): String {
    val r = (this * 10).roundToInt()
    return "${r / 10}.${r % 10}"
}
