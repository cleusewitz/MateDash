package com.soooool.matedash.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soooool.matedash.data.model.CarState
import com.soooool.matedash.data.repository.ApiConnectionState
import com.soooool.matedash.ui.map.LeafletMapView
import kotlin.math.abs
import kotlin.math.roundToInt

private val DarkBg = Color(0xFF0B0B0B)
private val CardBg = Color(0xFF1A1A1A)
private val TeslaRed = Color(0xFFE31937)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFF8E8E93)
private val BatteryGreen = Color(0xFF34C759)
private val BatteryYellow = Color(0xFFFFCC00)
private val BatteryRed = Color(0xFFFF3B30)
private val ChargingBlue = Color(0xFF00C7FF)
private val TrackColor = Color(0xFF2C2C2E)

private fun Int.withComma(): String {
    val s = toString()
    val sb = StringBuilder()
    for (i in s.indices) {
        if (i > 0 && (s.length - i) % 3 == 0) sb.append(',')
        sb.append(s[i])
    }
    return sb.toString()
}
private fun Double.fmt0() = roundToInt().toString()
private fun Double.fmt0Comma() = roundToInt().withComma()
private fun Double.fmt1(): String {
    val rounded = (this * 10).roundToInt()
    return "${rounded / 10}.${abs(rounded % 10)}"
}
private fun Double.fmt2(): String {
    val rounded = (this * 100).roundToInt()
    return "${rounded / 100}.${abs(rounded % 100).toString().padStart(2, '0')}"
}
private fun Double.toPsi() = this * 14.5038

@Composable
fun DashboardScreen() {
    val vm = viewModel { DashboardViewModel() }
    val car by vm.carState.collectAsState()
    val connState by vm.connectionState.collectAsState()
    val errorMsg by vm.errorMessage.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        // Header
        item { DashboardHeader(car, connState, errorMsg) }

        // Battery card
        item { BatteryCard(car) }

        // Location / map card
        item { LocationCard(car) }

        // Status grid 2x2
        item { StatusGrid(car) }

        // Tire pressure card
        if (car.tpmsFl > 0) {
            item { TirePressureCard(car) }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun DashboardHeader(car: CarState, connState: ApiConnectionState, errorMsg: String?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = car.displayName.ifEmpty { "Tesla" },
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StateChip(car.state)
                if (car.softwareVersion.isNotEmpty()) {
                    Text(car.softwareVersion, fontSize = 11.sp, color = Color(0xFF48484A))
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            ConnectionDot(connState)
            if (errorMsg != null) {
                Spacer(Modifier.width(8.dp))
                Text("⚠️", fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun BatteryCard(car: CarState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(20.dp))
            .padding(vertical = 24.dp, horizontal = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 왼쪽: 배터리 게이지
            Box(modifier = Modifier.weight(1.5f), contentAlignment = Alignment.Center) {
                BatteryGauge(
                    batteryLevel = car.batteryLevel,
                    chargeLimitSoc = car.chargeLimitSoc,
                    chargingState = car.chargingState,
                    estRangeKm = car.estBatteryRangeKm,
                )
            }
            // 오른쪽: 잠금 / 감시 / 창문 상태 (우측 정렬)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.End,
            ) {
                CarStatusItem(
                    title = "잠금",
                    value = if (car.isLocked) "잠김" else "해제",
                    active = car.isLocked,
                    activeColor = BatteryGreen,
                )
                CarStatusItem(
                    title = "감시",
                    value = if (car.sentryMode) "켜짐" else "꺼짐",
                    active = car.sentryMode,
                    activeColor = ChargingBlue,
                )
                CarStatusItem(
                    title = "창문",
                    value = if (car.windowsOpen) "열림" else "닫힘",
                    active = car.windowsOpen,
                    activeColor = BatteryYellow,
                )
            }
        }
        if (car.geofence.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "📍 ${car.geofence}",
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun CarStatusItem(title: String, value: String, active: Boolean, activeColor: Color) {
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = title,
            fontSize = 11.sp,
            color = Color(0xFF48484A),
            letterSpacing = 0.5.sp,
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .background(
                    color = if (active) activeColor.copy(alpha = 0.15f) else Color(0xFF2C2C2E),
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(horizontal = 12.dp, vertical = 5.dp),
        ) {
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (active) activeColor else Color(0xFF636366),
            )
        }
    }
}

@Composable
private fun LocationCard(car: CarState) {
    val hasLocation = car.latitude != 0.0 || car.longitude != 0.0
    // 좌표 없으면 서울 기본값으로 지도는 항상 표시
    val mapLat = if (hasLocation) car.latitude else 37.5665
    val mapLng = if (hasLocation) car.longitude else 126.9780
    val headingStr = when {
        car.heading < 23 || car.heading >= 338 -> "N"
        car.heading < 68 -> "NE"
        car.heading < 113 -> "E"
        car.heading < 158 -> "SE"
        car.heading < 203 -> "S"
        car.heading < 248 -> "SW"
        car.heading < 293 -> "W"
        else -> "NW"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1A1A2E)),
    ) {
        LeafletMapView(
            lat = mapLat,
            lng = mapLng,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = if (hasLocation) "📍 ${car.latitude.fmt1()}, ${car.longitude.fmt1()}" else "📍 위치 정보 없음",
                fontSize = 12.sp,
                color = TextSecondary,
            )
            if (hasLocation) {
                Text(
                    text = "고도 ${car.elevation}m · $headingStr ${car.heading}°",
                    fontSize = 12.sp,
                    color = TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun StatusGrid(car: CarState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusCard(
                title = "실내 온도",
                value = "${car.insideTemp.fmt1()}°C",
                subtitle = if (car.isClimateOn) "공조 켜짐" else "공조 꺼짐",
                modifier = Modifier.weight(1f),
            )
            StatusCard(
                title = "실외 온도",
                value = "${car.outsideTemp.fmt1()}°C",
                subtitle = "",
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusCard(
                title = "주행거리",
                value = "${car.odometer.fmt0Comma()} km",
                subtitle = "",
                modifier = Modifier.weight(1f),
            )
            StatusCard(
                title = "소프트웨어",
                value = car.softwareVersion.ifEmpty { "-" },
                subtitle = "",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatusCard(title: String, value: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(CardBg, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Text(title, fontSize = 12.sp, color = TextSecondary)
        Spacer(Modifier.height(8.dp))
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
        )
        if (subtitle.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 12.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun TirePressureCard(car: CarState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Text("타이어 공기압 (psi)", fontSize = 12.sp, color = TextSecondary)
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 왼쪽: 앞좌 (위) / 뒤좌 (아래)
            Column(
                modifier = Modifier
                    .width(72.dp)
                    .fillMaxHeight()
                    .padding(vertical = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TirePressureCell("앞좌", car.tpmsFl)
                TirePressureCell("뒤좌", car.tpmsRl)
            }
            // 중앙: 차량 탑뷰
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                CarTopDownView()
            }
            // 오른쪽: 앞우 (위) / 뒤우 (아래)
            Column(
                modifier = Modifier
                    .width(72.dp)
                    .fillMaxHeight()
                    .padding(vertical = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TirePressureCell("앞우", car.tpmsFr)
                TirePressureCell("뒤우", car.tpmsRr)
            }
        }
    }
}

@Composable
private fun CarTopDownView() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val bodyW = size.width * 0.48f
        val bodyH = size.height * 0.86f
        val bodyLeft = cx - bodyW / 2f
        val bodyRight = cx + bodyW / 2f
        val bodyTop = (size.height - bodyH) / 2f
        val bodyBottom = bodyTop + bodyH

        // ── Tesla Model 3 차체 외형 (앞은 좁고 뒤는 넓은 fastback 실루엣) ──
        val frontR = bodyW * 0.14f
        val rearR  = bodyW * 0.20f
        val bodyPath = Path().apply {
            moveTo(bodyLeft + frontR, bodyTop)
            lineTo(bodyRight - frontR, bodyTop)
            quadraticBezierTo(bodyRight, bodyTop, bodyRight, bodyTop + frontR)
            lineTo(bodyRight, bodyBottom - rearR)
            quadraticBezierTo(bodyRight, bodyBottom, bodyRight - rearR, bodyBottom)
            lineTo(bodyLeft + rearR, bodyBottom)
            quadraticBezierTo(bodyLeft, bodyBottom, bodyLeft, bodyBottom - rearR)
            lineTo(bodyLeft, bodyTop + frontR)
            quadraticBezierTo(bodyLeft, bodyTop, bodyLeft + frontR, bodyTop)
            close()
        }
        drawPath(bodyPath, color = Color(0xFF3A3A3C))

        // ── 파노라믹 유리 루프 (Tesla 시그니처 — 앞유리~뒷유리 하나로 연결) ──
        val roofHPad = bodyW * 0.09f
        drawRoundRect(
            color = Color(0xFF151C2A),
            topLeft = Offset(bodyLeft + roofHPad, bodyTop + bodyH * 0.10f),
            size = Size(bodyW - roofHPad * 2f, bodyH * 0.68f),
            cornerRadius = CornerRadius(6.dp.toPx()),
        )

        // ── 앞 노즈 (그릴 없는 매끈한 프런트 패시아) ──
        drawRoundRect(
            color = Color(0xFF2A2A2C),
            topLeft = Offset(bodyLeft + bodyW * 0.14f, bodyTop),
            size = Size(bodyW * 0.72f, bodyH * 0.055f),
            cornerRadius = CornerRadius(frontR, frontR),
        )

        // ── LED 헤드라이트 스트립 (좌/우 얇은 줄) ──
        val hlW = bodyW * 0.26f
        val hlH = bodyH * 0.022f
        val hlY = bodyTop + bodyH * 0.055f
        drawRoundRect(
            color = Color(0xFFD4CFA8),
            topLeft = Offset(bodyLeft + bodyW * 0.04f, hlY),
            size = Size(hlW, hlH),
            cornerRadius = CornerRadius(2.dp.toPx()),
        )
        drawRoundRect(
            color = Color(0xFFD4CFA8),
            topLeft = Offset(bodyRight - bodyW * 0.04f - hlW, hlY),
            size = Size(hlW, hlH),
            cornerRadius = CornerRadius(2.dp.toPx()),
        )

        // ── 풀-위드 테일라이트 바 (Tesla 시그니처 붉은 줄) ──
        drawRoundRect(
            color = Color(0xFFBB1515),
            topLeft = Offset(bodyLeft + bodyW * 0.07f, bodyBottom - bodyH * 0.052f),
            size = Size(bodyW * 0.86f, bodyH * 0.022f),
            cornerRadius = CornerRadius(2.dp.toPx()),
        )

        // ── 바퀴 + 림 디테일 ──
        val wW = bodyW * 0.30f
        val wH = bodyH * 0.15f
        val wOverlap = bodyW * 0.06f
        val wFrontY = bodyTop + bodyH * 0.10f
        val wRearY  = bodyTop + bodyH * 0.74f

        val wheelPositions = listOf(
            Offset(bodyLeft - wW + wOverlap, wFrontY),
            Offset(bodyRight - wOverlap,     wFrontY),
            Offset(bodyLeft - wW + wOverlap, wRearY),
            Offset(bodyRight - wOverlap,     wRearY),
        )
        wheelPositions.forEach { pos ->
            // 타이어
            drawRoundRect(
                color = Color(0xFF1A1A1A),
                topLeft = pos,
                size = Size(wW, wH),
                cornerRadius = CornerRadius(3.dp.toPx()),
            )
            // 림 (Aero 휠 느낌)
            drawRoundRect(
                color = Color(0xFF505050),
                topLeft = Offset(pos.x + wW * 0.18f, pos.y + wH * 0.18f),
                size = Size(wW * 0.64f, wH * 0.64f),
                cornerRadius = CornerRadius(2.dp.toPx()),
            )
        }
    }
}

@Composable
private fun TirePressureCell(label: String, pressure: Double, modifier: Modifier = Modifier) {
    val psi = pressure.toPsi()
    val color = when {
        pressure <= 0 -> TextSecondary
        psi < 36.3 -> BatteryRed
        psi < 39.2 -> BatteryYellow
        else -> BatteryGreen
    }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(psi.fmt1(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 11.sp, color = TextSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun BatteryGauge(batteryLevel: Int, chargeLimitSoc: Int, chargingState: String, estRangeKm: Double = 0.0) {
    val isCharging = chargingState.lowercase() == "charging"
    val batteryColor = when {
        isCharging -> ChargingBlue
        batteryLevel >= 50 -> BatteryGreen
        batteryLevel >= 20 -> BatteryYellow
        else -> BatteryRed
    }

    Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val padding = strokeWidth / 2
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(padding, padding)

            drawArc(TrackColor, 135f, 270f, false, topLeft, arcSize,
                style = Stroke(strokeWidth, cap = StrokeCap.Round))
            if (chargeLimitSoc in 1..99) {
                drawArc(Color(0xFF3A3A3C), 135f, 270f * (chargeLimitSoc / 100f), false,
                    topLeft, arcSize, style = Stroke(strokeWidth, cap = StrokeCap.Round))
            }
            if (batteryLevel > 0) {
                drawArc(batteryColor, 135f, 270f * (batteryLevel / 100f), false,
                    topLeft, arcSize, style = Stroke(strokeWidth, cap = StrokeCap.Round))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$batteryLevel%", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            if (estRangeKm > 0) {
                Text("${estRangeKm.fmt0Comma()} km", color = TextSecondary, fontSize = 13.sp)
            }
            if (isCharging) Text("⚡ 충전 중", color = ChargingBlue, fontSize = 13.sp)
        }
    }
}

@Composable
private fun StateChip(state: String) {
    val (label, color) = when (state.lowercase()) {
        "online", "parked" -> "주차 중" to Color(0xFF636366)
        "charging" -> "충전 중" to ChargingBlue
        "driving" -> "주행 중" to BatteryGreen
        "asleep", "sleeping" -> "절전 모드" to Color(0xFF5E5CE6)
        "offline" -> "오프라인" to BatteryRed
        "updating" -> "업데이트 중" to BatteryYellow
        else -> state to Color(0xFF636366)
    }
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ConnectionDot(state: ApiConnectionState) {
    val color = when (state) {
        ApiConnectionState.CONNECTED -> BatteryGreen
        ApiConnectionState.CONNECTING -> BatteryYellow
        else -> BatteryRed
    }
    Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
}
