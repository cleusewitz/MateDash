package com.soooool.matedash.ui.dashboard

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.soooool.matedash.ui.util.toDisplayTemp
import com.soooool.matedash.ui.util.toDisplayKm
import com.soooool.matedash.ui.util.toDisplaySpeed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.soooool.matedash.data.api.UpdateDto
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
private fun String.stripBuildHash() = trim().substringBefore(" ")
private fun Double.toPsi() = this

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen() {
    val vm = viewModel { DashboardViewModel() }
    val car by vm.carState.collectAsState()
    val connState by vm.connectionState.collectAsState()
    val errorMsg by vm.errorMessage.collectAsState()
    val settings by com.soooool.matedash.ServiceLocator.settingsFlow.collectAsState()
    val updates by vm.updates.collectAsState()

    var showSoftwareSheet by remember { mutableStateOf(false) }
    var showBatteryDetail by remember { mutableStateOf(false) }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(modifier = Modifier.fillMaxSize()) {
        // 메인 대시보드
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBg)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = statusBarTop + 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { DashboardHeader(car, connState, errorMsg) }
            item {
                BatteryCard(
                    car = car,
                    onTap = { showBatteryDetail = true },
                    onSoftwareClick = {
                        vm.loadUpdates()
                        showSoftwareSheet = true
                    },
                )
            }
            // 일일 요약
            item {
                val daily by vm.dailySummary.collectAsState()
                DailySummaryCard(daily)
            }
            if (settings.mapEnabled) {
                item { LocationCard(car) }
            }
            item { Spacer(Modifier.height(100.dp)) }
        }

        // 배터리 상세 화면 (슬라이드 전환)
        AnimatedVisibility(
            visible = showBatteryDetail,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = androidx.compose.animation.core.tween(300),
            ) + fadeIn(animationSpec = androidx.compose.animation.core.tween(300)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = androidx.compose.animation.core.tween(250),
            ) + fadeOut(animationSpec = androidx.compose.animation.core.tween(250)),
        ) {
            BatteryDetailScreen(car = car, onBack = { showBatteryDetail = false })
        }
    }

    // 소프트웨어 업데이트 내역 (슬라이드 전환)
    AnimatedVisibility(
        visible = showSoftwareSheet,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = androidx.compose.animation.core.tween(300),
        ) + fadeIn(animationSpec = androidx.compose.animation.core.tween(300)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = androidx.compose.animation.core.tween(250),
        ) + fadeOut(animationSpec = androidx.compose.animation.core.tween(250)),
    ) {
        SoftwareUpdatesScreen(
            currentVersion = car.softwareVersion,
            updates = updates,
            onBack = { showSoftwareSheet = false },
        )
    }
}

@Composable
private fun DashboardHeader(car: CarState, connState: ApiConnectionState, errorMsg: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = car.displayName.ifEmpty { "Tesla" },
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            StateChip(car.state)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (car.geofence.isNotEmpty()) {
                Text(car.geofence, fontSize = 12.sp, color = TextSecondary)
            }
            ConnectionDot(connState)
            if (errorMsg != null) {
                Text("⚠️", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun BatteryCard(car: CarState, onTap: () -> Unit = {}, onSoftwareClick: () -> Unit = {}) {
    val isCharging = car.chargingState.lowercase() == "charging"
    val batteryColor = when {
        isCharging -> ChargingBlue
        car.batteryLevel >= 50 -> BatteryGreen
        car.batteryLevel >= 20 -> BatteryYellow
        else -> BatteryRed
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(20.dp))
            .clickable { onTap() }
            .padding(20.dp),
    ) {
        // 배터리 퍼센트 + 상태
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${car.batteryLevel}",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    lineHeight = 48.sp,
                )
                Column(modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)) {
                    Text("%", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                    if (isCharging) {
                        Text("충전 중", fontSize = 11.sp, color = ChargingBlue)
                    }
                }
            }
            if (car.estBatteryRangeKm > 0) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(car.estBatteryRangeKm.toDisplayKm(), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("주행 가능", fontSize = 10.sp, color = TextSecondary)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // 배터리 바
        Box(modifier = Modifier.fillMaxWidth().height(8.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val barRadius = CornerRadius(4.dp.toPx())
                // 트랙
                drawRoundRect(TrackColor, cornerRadius = barRadius)
                // 충전 한도 마커
                if (car.chargeLimitSoc in 1..99) {
                    val limitX = size.width * (car.chargeLimitSoc / 100f)
                    drawRoundRect(
                        Color(0xFF3A3A3C),
                        size = Size(limitX, size.height),
                        cornerRadius = barRadius,
                    )
                }
                // 배터리 레벨
                if (car.batteryLevel > 0) {
                    val levelWidth = size.width * (car.batteryLevel / 100f)
                    drawRoundRect(
                        batteryColor,
                        size = Size(levelWidth, size.height),
                        cornerRadius = barRadius,
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // 하단 정보
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            InfoChip("주행거리", car.odometer.toDisplayKm())
            InfoChip("충전 한도", "${car.chargeLimitSoc}%")
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onSoftwareClick() },
            ) {
                Text("소프트웨어", fontSize = 10.sp, color = TextSecondary)
                Text(
                    car.softwareVersion.stripBuildHash().ifEmpty { "-" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = ChargingBlue,
                )
            }
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = TextSecondary)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
    }
}

@Composable
private fun ClimateInfoItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = TextSecondary)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}

@Composable
private fun BatteryDetailScreen(car: CarState, onBack: () -> Unit) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

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
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = CardBg,
                        contentColor = TextPrimary,
                    ),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "뒤로", modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "배터리 상세",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
            }
        }

        // 배터리 게이지 (큰 버전)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBg, RoundedCornerShape(20.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                BatteryGauge(
                    batteryLevel = car.batteryLevel,
                    chargeLimitSoc = car.chargeLimitSoc,
                    chargingState = car.chargingState,
                    estRangeKm = car.estBatteryRangeKm,
                )
            }
        }

        // 배터리 잔량
        item {
            DetailCard("배터리") {
                DetailRow("잔량", "${car.batteryLevel}%")
                DetailRow("사용 가능", "${car.usableBatteryLevel}%")
                DetailRow("충전 상한", "${car.chargeLimitSoc}%")
            }
        }

        // 주행거리
        item {
            DetailCard("주행거리") {
                DetailRow("예상 주행거리", car.estBatteryRangeKm.toDisplayKm())
                DetailRow("정격 주행거리", car.ratedBatteryRangeKm.toDisplayKm())
            }
        }

        // 충전 상태
        item {
            DetailCard("충전") {
                DetailRow("충전 포트", if (car.chargePortDoorOpen) "열림" else "닫힘")
                DetailRow("플러그", if (car.isPluggedIn) "연결됨" else "미연결")
                if (car.isPluggedIn) {
                    DetailRow("충전 상태", car.chargingState.ifEmpty { "-" })
                    if (car.chargerPower > 0) {
                        DetailRow("충전 전력", "${car.chargerPower} kW")
                    }
                    if (car.chargerVoltage > 0) {
                        DetailRow("충전 전압", "${car.chargerVoltage} V")
                    }
                    if (car.chargeEnergyAdded > 0) {
                        DetailRow("추가된 에너지", "${car.chargeEnergyAdded.fmt1()} kWh")
                    }
                    if (car.timeToFullCharge > 0) {
                        val hours = car.timeToFullCharge.toInt()
                        val minutes = ((car.timeToFullCharge - hours) * 60).roundToInt()
                        val timeStr = if (hours > 0) "${hours}시간 ${minutes}분" else "${minutes}분"
                        DetailRow("완충 예상", timeStr)
                    }
                }
            }
        }

        // 온도 / 공조
        item {
            DetailCard("온도 / 공조") {
                DetailRow("실내 온도", car.insideTemp.toDisplayTemp())
                DetailRow("실외 온도", car.outsideTemp.toDisplayTemp())
                DetailRow("공조", if (car.isClimateOn) "켜짐" else "꺼짐")
                if (car.isPreconditioning) {
                    DetailRow("프리컨디셔닝", "진행 중")
                }
            }
        }

        // 타이어 공기압
        if (car.tpmsFl > 0) {
            item {
                DetailCard("타이어 공기압") {
                    DetailRow("앞 좌", "${car.tpmsFl.fmt1()} bar")
                    DetailRow("앞 우", "${car.tpmsFr.fmt1()} bar")
                    DetailRow("뒤 좌", "${car.tpmsRl.fmt1()} bar")
                    DetailRow("뒤 우", "${car.tpmsRr.fmt1()} bar")
                }
            }
        }

        // 차량 상태
        item {
            DetailCard("차량") {
                DetailRow("잠금", if (car.isLocked) "잠김" else "해제")
                DetailRow("감시 모드", if (car.sentryMode) "켜짐" else "꺼짐")
                DetailRow("창문", if (car.windowsOpen) "열림" else "닫힘")
                DetailRow("문", if (car.doorsOpen) "열림" else "닫힘")
                DetailRow("트렁크", if (car.trunkOpen) "열림" else "닫힘")
                DetailRow("프렁크", if (car.frunkOpen) "열림" else "닫힘")
            }
        }

        item { Spacer(Modifier.height(100.dp)) }
    }
}

@Composable
private fun DetailCard(title: String, content: @Composable () -> Unit) {
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
            color = ChargingBlue,
        )
        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
        content()
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = ChargingBlue,
        )
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

    var showFullMap by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1A1A2E)),
    ) {
        Box {
            LeafletMapView(
                lat = mapLat,
                lng = mapLng,
                interactive = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
            )
            IconButton(
                onClick = { showFullMap = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(36.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.6f),
                    contentColor = Color.White,
                ),
            ) {
                Icon(
                    imageVector = Icons.Filled.Fullscreen,
                    contentDescription = "지도 확대",
                    modifier = Modifier.size(20.dp),
                )
            }
        }
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

    if (showFullMap) {
        Dialog(
            onDismissRequest = { showFullMap = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            var showCarInfo by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
            ) {
                LeafletMapView(
                    lat = mapLat,
                    lng = mapLng,
                    interactive = true,
                    modifier = Modifier.fillMaxSize(),
                )

                // 상단 버튼 영역
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconButton(
                        onClick = { showCarInfo = !showCarInfo },
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (showCarInfo) TeslaRed else Color.Black.copy(alpha = 0.6f),
                            contentColor = Color.White,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DirectionsCar,
                            contentDescription = "차량 정보",
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    IconButton(
                        onClick = { showFullMap = false },
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.6f),
                            contentColor = Color.White,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "닫기",
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                // 차량 정보 오버레이 카드
                AnimatedVisibility(
                    visible = showCarInfo,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                ) {
                    MapCarInfoCard(car, headingStr)
                }
            }
        }
    }
}

@Composable
private fun MapCarInfoCard(car: CarState, headingStr: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg.copy(alpha = 0.92f))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 차량명 + 상태
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = car.displayName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Text(
                text = car.state.replaceFirstChar { it.uppercase() },
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = when (car.state) {
                    "online" -> Color(0xFF34C759)
                    "driving" -> Color(0xFF00C7FF)
                    "charging" -> Color(0xFFFFCC00)
                    else -> TextSecondary
                },
            )
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

        // 배터리 + 충전
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            MapInfoItem("배터리", "${car.batteryLevel}%")
            MapInfoItem("예상 주행", car.estBatteryRangeKm.toDisplayKm())
            MapInfoItem("충전 상한", "${car.chargeLimitSoc}%")
        }

        // 온도
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            MapInfoItem("실내", car.insideTemp.toDisplayTemp())
            MapInfoItem("실외", car.outsideTemp.toDisplayTemp())
            MapInfoItem("공조", if (car.isClimateOn) "켜짐" else "꺼짐")
        }

        // 위치 + 상태
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            MapInfoItem("고도", "${car.elevation}m")
            MapInfoItem("방위", "$headingStr ${car.heading}°")
            MapInfoItem("잠금", if (car.isLocked) "잠김" else "열림")
        }

        // 주행거리
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            MapInfoItem("주행거리", car.odometer.toDisplayKm())
            MapInfoItem("센트리", if (car.sentryMode) "켜짐" else "꺼짐")
            MapInfoItem("속도", car.speed.toDouble().toDisplaySpeed())
        }
    }
}

@Composable
private fun MapInfoItem(label: String, value: String) {
    Column(
        modifier = Modifier.width(100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, fontSize = 11.sp, color = TextSecondary)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}

@Composable
private fun DailySummaryCard(daily: DailySummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // 주행 카드
        Column(
            modifier = Modifier
                .weight(1f)
                .background(CardBg, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Canvas(modifier = Modifier.size(6.dp)) { drawCircle(color = ChargingBlue) }
                Text("오늘 주행", fontSize = 11.sp, color = TextSecondary)
            }
            Text(
                "${daily.driveDistanceKm.fmt1()} km",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = ChargingBlue,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("${daily.driveCount}회", fontSize = 11.sp, color = TextSecondary)
                Text(formatMinutes(daily.driveDurationMin), fontSize = 11.sp, color = TextSecondary)
            }
        }

        // 충전 카드
        Column(
            modifier = Modifier
                .weight(1f)
                .background(CardBg, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Canvas(modifier = Modifier.size(6.dp)) { drawCircle(color = BatteryGreen) }
                Text("오늘 충전", fontSize = 11.sp, color = TextSecondary)
            }
            Text(
                "${daily.chargeEnergyKwh.fmt1()} kWh",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = BatteryGreen,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("${daily.chargeCount}회", fontSize = 11.sp, color = TextSecondary)
                Text(formatMinutes(daily.chargeDurationMin), fontSize = 11.sp, color = TextSecondary)
            }
        }
    }
}

private fun formatMinutes(min: Int): String {
    val h = min / 60
    val m = min % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

@Composable
private fun StatusGrid(car: CarState, onSoftwareClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusCard(
                title = "주행거리",
                value = car.odometer.toDisplayKm(),
                subtitle = "",
                modifier = Modifier.weight(1f),
            )
            StatusCard(
                title = "소프트웨어",
                value = car.softwareVersion.stripBuildHash().ifEmpty { "-" },
                subtitle = "상세 보기 ›",
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSoftwareClick() },
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
        Spacer(Modifier.height(2.dp))
        Text(
            text = subtitle.ifEmpty { " " },
            fontSize = 12.sp,
            color = TextSecondary,
        )
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
            quadraticTo(bodyRight, bodyTop, bodyRight, bodyTop + frontR)
            lineTo(bodyRight, bodyBottom - rearR)
            quadraticTo(bodyRight, bodyBottom, bodyRight - rearR, bodyBottom)
            lineTo(bodyLeft + rearR, bodyBottom)
            quadraticTo(bodyLeft, bodyBottom, bodyLeft, bodyBottom - rearR)
            lineTo(bodyLeft, bodyTop + frontR)
            quadraticTo(bodyLeft, bodyTop, bodyLeft + frontR, bodyTop)
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
private fun BatteryGaugeCompact(batteryLevel: Int, chargeLimitSoc: Int, chargingState: String, estRangeKm: Double = 0.0) {
    val isCharging = chargingState.lowercase() == "charging"
    val batteryColor = when {
        isCharging -> ChargingBlue
        batteryLevel >= 50 -> BatteryGreen
        batteryLevel >= 20 -> BatteryYellow
        else -> BatteryRed
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 10.dp.toPx()
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
            Text("$batteryLevel%", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            if (estRangeKm > 0) {
                Text(estRangeKm.toDisplayKm(), color = TextSecondary, fontSize = 11.sp)
            }
            if (isCharging) Text("⚡ 충전 중", color = ChargingBlue, fontSize = 11.sp)
        }
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
                Text(estRangeKm.toDisplayKm(), color = TextSecondary, fontSize = 13.sp)
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
        "asleep", "sleeping", "suspended" -> "절전 모드" to Color(0xFF5E5CE6)
        "offline" -> "오프라인" to BatteryRed
        "updating" -> "업데이트 중" to BatteryYellow
        "start" -> "시동 중" to BatteryYellow
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

@Composable
private fun SoftwareUpdatesScreen(
    currentVersion: String,
    updates: List<UpdateDto>,
    onBack: () -> Unit,
) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

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
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = CardBg,
                        contentColor = TextPrimary,
                    ),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "닫기", modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "소프트웨어 업데이트 내역",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
            }
        }

        // 현재 버전
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBg, RoundedCornerShape(20.dp))
                    .padding(20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("현재 버전", fontSize = 14.sp, color = TextSecondary)
                    Text(
                        currentVersion.stripBuildHash().ifEmpty { "-" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BatteryGreen,
                    )
                }
            }
        }

        if (updates.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("업데이트 내역이 없습니다", fontSize = 14.sp, color = TextSecondary)
                }
            }
        } else {
            // 업데이트 목록
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardBg, RoundedCornerShape(20.dp))
                        .padding(20.dp),
                ) {
                    updates.forEachIndexed { index, update ->
                        val daysFromPrev = if (index < updates.lastIndex) {
                            daysBetween(updates[index + 1].startDate, update.startDate)
                        } else null
                        UpdateRow(
                            update = update,
                            isCurrent = index == 0 && update.version?.stripBuildHash() == currentVersion.stripBuildHash(),
                            daysFromPrev = daysFromPrev,
                        )
                        if (index < updates.lastIndex) {
                            HorizontalDivider(
                                color = Color(0xFF2C2C2E),
                                modifier = Modifier.padding(vertical = 2.dp),
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(100.dp)) }
    }
}

@Composable
private fun UpdateRow(update: UpdateDto, isCurrent: Boolean, daysFromPrev: Int?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = update.version?.stripBuildHash() ?: "-",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                )
                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .background(BatteryGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text("현재", fontSize = 10.sp, color = BatteryGreen, fontWeight = FontWeight.Medium)
                    }
                }
                if (daysFromPrev != null) {
                    Box(
                        modifier = Modifier
                            .background(ChargingBlue.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "${daysFromPrev}일 만에",
                            fontSize = 10.sp,
                            color = ChargingBlue,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            val dateText = formatUpdateDate(update.startDate)
            if (dateText.isNotEmpty()) {
                Text(dateText, fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}

private fun formatUpdateDate(dateStr: String?): String {
    if (dateStr.isNullOrEmpty()) return ""
    // ISO format: "2024-03-15T10:30:00Z" → "2024-03-15 10:30"
    return try {
        val parts = dateStr.split("T")
        if (parts.size >= 2) {
            val date = parts[0]
            val time = parts[1].substringBefore("Z").substringBefore("+").take(5)
            "$date $time"
        } else {
            dateStr.take(16)
        }
    } catch (_: Exception) {
        dateStr.take(16)
    }
}

private fun daysBetween(fromDate: String?, toDate: String?): Int? {
    if (fromDate.isNullOrEmpty() || toDate.isNullOrEmpty()) return null
    return try {
        val from = kotlinx.datetime.LocalDate.parse(fromDate.substringBefore("T"))
        val to = kotlinx.datetime.LocalDate.parse(toDate.substringBefore("T"))
        (to.toEpochDays() - from.toEpochDays()).toInt()
    } catch (_: Exception) {
        null
    }
}
