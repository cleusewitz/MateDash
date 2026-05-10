package com.soooool.matedash.ui.cluster

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.soooool.matedash.data.media.NowPlayingInfo
import com.soooool.matedash.data.model.CarState
import com.soooool.matedash.data.repository.ApiConnectionState
import com.soooool.matedash.ui.util.toDisplayKm
import com.soooool.matedash.ui.util.toDisplayTemp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val ClusterBg = Color(0xFF050505)
private val TextPrimary = Color.White
private val TextDim = Color(0xFF8E8E93)
private val TextMuted = Color(0xFF48484A)
private val PillBg = Color(0xFF1C1C1E)
private val BatteryGreen = Color(0xFF34C759)
private val BatteryYellow = Color(0xFFFFCC00)
private val BatteryRed = Color(0xFFFF3B30)
private val ChargingBlue = Color(0xFF00C7FF)
private val TrackColor = Color(0xFF1C1C1E)
private val GaugeTrack = Color(0xFF2A2A2E)

@Composable
fun ClusterScreen(
    car: CarState,
    connectionState: ApiConnectionState = ApiConnectionState.CONNECTED,
    onDismiss: () -> Unit,
) {
    // TeslaMate가 publish하는 차량 미디어 정보를 사용 (iOS/Android 무관, MQTT 활성 시 1초 갱신)
    val nowPlaying: NowPlayingInfo? = if (car.mediaTitle.isNotBlank()) {
        NowPlayingInfo(
            title = car.mediaTitle,
            artist = car.mediaArtist,
            isPlaying = car.mediaStatus.equals("Playing", ignoreCase = true),
        )
    } else null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        val safeInsets = WindowInsets.safeDrawing.asPaddingValues()

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(safeInsets),
        ) {
            val isLandscape = maxWidth > maxHeight

            if (isLandscape) {
                LandscapeCluster(car, connectionState, nowPlaying)
            } else {
                PortraitCluster(car, connectionState, nowPlaying)
            }

            // 닫기 버튼
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(36.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color(0xFF1C1C1E),
                    contentColor = TextDim,
                ),
            ) {
                Icon(Icons.Filled.Close, contentDescription = "닫기", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun LandscapeCluster(car: CarState, connectionState: ApiConnectionState, nowPlaying: NowPlayingInfo?) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        StatusBar(car, connectionState)

        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LeftPanel(car, modifier = Modifier.weight(1f).fillMaxHeight())
            CenterGauge(car, modifier = Modifier.weight(1.2f).fillMaxHeight())

            // 오른쪽: Now Playing + 차량 상태
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().padding(start = 16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.End,
            ) {
                NowPlayingCard(nowPlaying)
                Spacer(Modifier.height(20.dp))
                RightPanelContent(car)
            }
        }

        BottomBar(car)
    }
}

@Composable
private fun PortraitCluster(car: CarState, connectionState: ApiConnectionState, nowPlaying: NowPlayingInfo?) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(40.dp))

        StatusBar(car, connectionState)

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            InfoLabel("실내", car.insideTemp.toDisplayTemp())
            InfoLabel("실외", car.outsideTemp.toDisplayTemp())
            InfoLabel("전력", "${car.power} kW")
        }

        CenterGauge(car, modifier = Modifier.fillMaxWidth().weight(1f))

        NowPlayingCard(nowPlaying)
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("위치", fontSize = 10.sp, color = TextMuted)
                Text(
                    car.geofence.ifEmpty { "-" },
                    fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Medium,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("잠금", fontSize = 10.sp, color = TextMuted)
                Text(
                    if (car.isLocked) "잠김" else "열림",
                    fontSize = 14.sp,
                    color = if (car.isLocked) TextDim else BatteryYellow,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        BottomBar(car)
    }
}

// ── 상단 상태바 ──

@Composable
private fun StatusBar(car: CarState, connectionState: ApiConnectionState) {
    var currentTime by remember { mutableStateOf(currentTimeString()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            currentTime = currentTimeString()
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusPill { Text(currentTime, fontSize = 12.sp, color = TextPrimary) }
            StatusPill { Text(car.insideTemp.toDisplayTemp(), fontSize = 12.sp, color = TextPrimary) }
            StatusPill { Text(car.outsideTemp.toDisplayTemp(), fontSize = 12.sp, color = TextDim) }
            StatusPill {
                val connColor = when (connectionState) {
                    ApiConnectionState.CONNECTED -> BatteryGreen
                    ApiConnectionState.CONNECTING -> BatteryYellow
                    else -> BatteryRed
                }
                val connText = when (connectionState) {
                    ApiConnectionState.CONNECTED -> "연결됨"
                    ApiConnectionState.CONNECTING -> "연결 중"
                    else -> "오프라인"
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(6.dp).background(connColor, CircleShape))
                    Text(connText, fontSize = 12.sp, color = connColor)
                }
            }
        }
        if (car.isClimateOn) {
            StatusPill {
                Text("공조 ON", fontSize = 12.sp, color = ChargingBlue)
            }
        }
    }
}

@Composable
private fun StatusPill(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .background(PillBg, RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        content()
    }
}

// ── 왼쪽 패널 (위치/차량 정보) ──

@Composable
private fun LeftPanel(car: CarState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(end = 16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        if (car.activeRouteDestination.isNotBlank()) {
            // 내비 활성화 시 — NavigationCard만 표시. 전력은 오른쪽 음악 아래로 이동.
            NavigationCard(car)
        } else {
            if (car.geofence.isNotEmpty()) {
                Text(car.geofence, fontSize = 16.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(16.dp))
            }
            InfoLabel("속도", "${car.speed} km/h")
            Spacer(Modifier.height(14.dp))
            InfoLabel("방위", "${headingToDirection(car.heading)} ${car.heading}°")
            if (car.elevation != 0) {
                Spacer(Modifier.height(14.dp))
                InfoLabel("고도", "${car.elevation}m")
            }
        }
    }
}

@Composable
private fun NavigationCard(car: CarState) {
    val mileToKm = 1.609344
    val km = car.activeRouteMilesToArrival * mileToKm
    val minutes = car.activeRouteMinutesToArrival
    val arrival = computeArrivalTime(minutes)
    val battery = car.activeRouteEnergyAtArrival

    Column(modifier = Modifier.fillMaxWidth()) {
        // 헤더: 화살표 아이콘 + 목적지 이름
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.NearMe,
                contentDescription = null,
                tint = ChargingBlue,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = car.activeRouteDestination,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
            )
        }
        Spacer(Modifier.height(14.dp))

        Text("남은 거리", fontSize = 12.sp, color = TextDim)
        Text(
            "${formatKm(km)} km",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
        )
        Spacer(Modifier.height(14.dp))

        Text("도착 시간", fontSize = 12.sp, color = TextDim)
        Text(
            arrival,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
        )
        Text(
            if (minutes > 0) "${minutes}분" else "-",
            fontSize = 14.sp,
            color = TextDim,
        )
        Spacer(Modifier.height(14.dp))

        Text("예상 배터리", fontSize = 12.sp, color = TextDim)
        Text(
            if (battery > 0) "$battery%" else "-",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = batteryColorFor(battery),
        )
        if (car.activeRouteTrafficMinutesDelay > 0) {
            Spacer(Modifier.height(6.dp))
            Text(
                "교통 지연 ${car.activeRouteTrafficMinutesDelay}분",
                fontSize = 11.sp,
                color = BatteryYellow,
            )
        }
    }
}

private fun batteryColorFor(pct: Int): Color = when {
    pct >= 50 -> BatteryGreen
    pct >= 20 -> BatteryYellow
    pct > 0 -> BatteryRed
    else -> TextDim
}

private fun formatKm(km: Double): String {
    val rounded = (km * 10).roundToInt()
    return "${rounded / 10}.${kotlin.math.abs(rounded % 10)}"
}

private fun computeArrivalTime(minutesFromNow: Int): String {
    if (minutesFromNow <= 0) return "-"
    val now = kotlin.time.Clock.System.now()
    val arrival = (now + kotlin.time.Duration.parse("${minutesFromNow}m"))
        .toLocalDateTime(TimeZone.currentSystemDefault())
    val h = arrival.hour.toString().padStart(2, '0')
    val m = arrival.minute.toString().padStart(2, '0')
    return "$h:$m"
}

// ── 오른쪽 패널 (차량 상태) ──

@Composable
private fun RightPanelContent(car: CarState) {
    // 전력 — 음악 카드 바로 아래에 배치 (구 LeftPanel에서 옮김). 회생 시 초록, 가속 시 주황.
    InfoLabel(
        label = "전력",
        value = "${car.power} kW",
        valueColor = if (car.power < 0) BatteryGreen else Color(0xFFFF9500),
        align = Alignment.End,
    )
    Spacer(Modifier.height(14.dp))

    if (car.chargingState.lowercase() == "charging") {
        InfoLabel("충전", "${car.chargerPower} kW", valueColor = ChargingBlue, align = Alignment.End)
        Spacer(Modifier.height(14.dp))
        if (car.timeToFullCharge > 0) {
            val h = car.timeToFullCharge.toInt()
            val m = ((car.timeToFullCharge - h) * 60).roundToInt()
            val timeStr = if (h > 0) "${h}시간 ${m}분" else "${m}분"
            InfoLabel("완충까지", timeStr, align = Alignment.End)
        }
    }
}

@Composable
private fun NowPlayingCard(info: NowPlayingInfo?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1C1C1E), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (info != null) ChargingBlue.copy(alpha = 0.15f) else Color(0xFF2C2C2E),
                    RoundedCornerShape(10.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.MusicNote,
                contentDescription = null,
                tint = if (info != null) ChargingBlue else TextMuted,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            if (info != null) {
                Text(
                    info.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 1,
                )
                if (info.artist.isNotBlank()) {
                    Text(
                        info.artist,
                        fontSize = 11.sp,
                        color = TextDim,
                        maxLines = 1,
                    )
                }
            } else {
                Text(
                    "재생 중이 아님",
                    fontSize = 13.sp,
                    color = TextMuted,
                )
            }
        }
    }
}

// ── 중앙 속도 게이지 ──

@Composable
private fun CenterGauge(car: CarState, modifier: Modifier = Modifier) {
    val isCharging = car.chargingState.lowercase() == "charging"

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val gaugeSize = min(maxWidth, maxHeight) - 16.dp

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(gaugeSize),
                contentAlignment = Alignment.Center,
            ) {
                SpeedArc(
                    batteryLevel = car.batteryLevel,
                    chargeLimitSoc = car.chargeLimitSoc,
                    power = car.power,
                    isCharging = isCharging,
                    modifier = Modifier.fillMaxSize(),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${car.speed}",
                        fontSize = 80.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        lineHeight = 80.sp,
                    )
                    Text("km/h", fontSize = 14.sp, color = TextDim)
                }
            }

            Spacer(Modifier.height(8.dp))

            GearIndicator(shiftState = car.shiftState)
        }
    }
}

@Composable
private fun SpeedArc(
    batteryLevel: Int,
    chargeLimitSoc: Int,
    power: Int,
    isCharging: Boolean,
    modifier: Modifier = Modifier,
) {
    val batColor = when {
        isCharging -> ChargingBlue
        batteryLevel >= 50 -> BatteryGreen
        batteryLevel >= 20 -> BatteryYellow
        else -> BatteryRed
    }

    Canvas(modifier = modifier) {
        val strokeWidth = 10.dp.toPx()
        val padding = strokeWidth / 2 + 6.dp.toPx()
        val arcSize = Size(size.width - padding * 2, size.height - padding * 2)
        val topLeft = Offset(padding, padding)

        // 트랙
        drawArc(GaugeTrack, 135f, 270f, false, topLeft, arcSize, style = Stroke(strokeWidth, cap = StrokeCap.Round))

        // 충전 한도
        if (chargeLimitSoc in 1..99) {
            drawArc(
                Color(0xFF3A3A3C), 135f, 270f * (chargeLimitSoc / 100f), false,
                topLeft, arcSize, style = Stroke(strokeWidth, cap = StrokeCap.Round),
            )
        }

        // 배터리 레벨
        if (batteryLevel > 0) {
            drawArc(
                batColor, 135f, 270f * (batteryLevel / 100f), false,
                topLeft, arcSize, style = Stroke(strokeWidth, cap = StrokeCap.Round),
            )
        }

        // 파워 인디케이터 (내곽)
        val innerStroke = 3.dp.toPx()
        val innerPadding = padding + strokeWidth + 4.dp.toPx()
        val innerSize = Size(size.width - innerPadding * 2, size.height - innerPadding * 2)
        val innerTopLeft = Offset(innerPadding, innerPadding)

        val maxPower = 300f
        val powerRatio = (abs(power).toFloat() / maxPower).coerceIn(0f, 1f)
        if (powerRatio > 0.01f) {
            val pColor = if (power < 0) BatteryGreen else Color(0xFFFF9500)
            drawArc(
                pColor, 135f, 270f * powerRatio, false, innerTopLeft, innerSize,
                style = Stroke(innerStroke, cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
private fun GearIndicator(shiftState: String) {
    val gears = listOf("P", "R", "N", "D")
    val current = shiftState.uppercase().takeIf { it in gears } ?: "P"

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        gears.forEach { gear ->
            val isActive = gear == current
            val activeColor = when (gear) {
                "R" -> BatteryRed
                "D" -> BatteryGreen
                else -> TextPrimary
            }
            // 같은 크기의 박스로 모든 글자를 동일한 width/height로 잡아 레이아웃 흔들림 방지.
            // 활성화된 기어만 컬러 박스로 감싸 강조.
            Box(
                modifier = Modifier
                    .size(width = 28.dp, height = 28.dp)
                    .background(
                        color = if (isActive) activeColor else Color.Transparent,
                        shape = RoundedCornerShape(6.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = gear,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isActive) Color.Black else TextMuted,
                )
            }
        }
    }
}

// ── 하단 상태바 ──

@Composable
private fun BottomBar(car: CarState) {
    val isCharging = car.chargingState.lowercase() == "charging"
    val batColor = when {
        isCharging -> ChargingBlue
        car.batteryLevel >= 50 -> BatteryGreen
        car.batteryLevel >= 20 -> BatteryYellow
        else -> BatteryRed
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "ODO ${formatOdo(car.odometer)} km",
            fontSize = 12.sp,
            color = TextMuted,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier
                    .size(width = 18.dp, height = 10.dp)
                    .background(batColor.copy(alpha = 0.3f), RoundedCornerShape(2.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(car.batteryLevel / 100f)
                        .background(batColor, RoundedCornerShape(2.dp)),
                )
            }
            // est가 0이면 rated로 폴백 — 차량/TeslaMate 환경에 따라 한쪽만 채워지는 경우가 있음
            val displayRangeKm = when {
                car.estBatteryRangeKm > 0.0 -> car.estBatteryRangeKm
                car.ratedBatteryRangeKm > 0.0 -> car.ratedBatteryRangeKm
                else -> 0.0
            }
            Text(
                "${car.batteryLevel}% / ${displayRangeKm.toDisplayKm()}",
                fontSize = 12.sp,
                color = batColor,
            )
        }
    }
}

// ── 공통 컴포넌트 ──

@Composable
private fun InfoLabel(
    label: String,
    value: String,
    valueColor: Color = TextPrimary,
    align: Alignment.Horizontal = Alignment.Start,
) {
    Column(horizontalAlignment = align) {
        Text(label, fontSize = 10.sp, color = TextMuted)
        Text(value, fontSize = 16.sp, color = valueColor, fontWeight = FontWeight.Medium)
    }
}

private fun headingToDirection(heading: Int): String = when {
    heading < 23 || heading >= 338 -> "N"
    heading < 68 -> "NE"
    heading < 113 -> "E"
    heading < 158 -> "SE"
    heading < 203 -> "S"
    heading < 248 -> "SW"
    heading < 293 -> "W"
    else -> "NW"
}

private fun currentTimeString(): String {
    val now = kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return "${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}"
}

private fun formatOdo(odo: Double): String {
    val value = odo.toLong()
    return value.toString().reversed().chunked(3).joinToString(",").reversed()
}
