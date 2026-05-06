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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soooool.matedash.ServiceLocator
import com.soooool.matedash.data.share.GeocodeResult
import com.soooool.matedash.data.share.PlaceGeocoder
import com.soooool.matedash.data.share.PlaceHit
import com.soooool.matedash.data.share.TMapShareInfo
import com.soooool.matedash.data.share.parseTMapShare
import com.soooool.matedash.data.share.readSharedText
import kotlinx.coroutines.launch

private const val TMAP_SAMPLE_TEXT = "불꽃교회에 오후 11시 36분 도착 예정입니다 https://tmap.life/c80f8e32"

@Composable
internal fun NavigationSettingsScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val geocoder = remember { PlaceGeocoder() }
    val sharedText = readSharedText()

    var input by remember { mutableStateOf(sharedText?.takeIf { it.isNotBlank() } ?: TMAP_SAMPLE_TEXT) }
    var lastAutoFilled by remember { mutableStateOf(sharedText) }
    var kakaoKey by remember { mutableStateOf("") }
    var googleKey by remember { mutableStateOf("") }
    var parsed by remember { mutableStateOf<TMapShareInfo?>(null) }
    var hits by remember { mutableStateOf<List<PlaceHit>>(emptyList()) }
    var status by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(sharedText) {
        if (!sharedText.isNullOrBlank() && sharedText != lastAutoFilled) {
            input = sharedText
            lastAutoFilled = sharedText
            parsed = null
            hits = emptyList()
            status = "공유 원문을 불러왔습니다"
        }
    }

    SettingsDetailScaffold(title = "내비게이션", onBack = onBack) {
        // TMap 링크 좌표 추출
        SectionTitle("TMap 링크 좌표 추출")
        Spacer(Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg, RoundedCornerShape(20.dp))
                .padding(20.dp),
        ) {
            Text(
                "TMap 단축링크 + 목적지명 메시지를 붙여넣으면 지명으로 좌표를 역추출합니다.",
                fontSize = 11.sp,
                color = TextSecondary,
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("공유 메시지", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = ChargingBlue,
                    unfocusedBorderColor = Color(0xFF3A3A3C),
                    focusedLabelColor = ChargingBlue,
                    unfocusedLabelColor = TextSecondary,
                    cursorColor = ChargingBlue,
                ),
            )

            Spacer(Modifier.height(8.dp))

            TeslaTextField(label = "Kakao REST API 키 (선택)", value = kakaoKey, onValueChange = { kakaoKey = it.trim() })
            Spacer(Modifier.height(8.dp))
            TeslaTextField(label = "Google Places API 키 (선택)", value = googleKey, onValueChange = { googleKey = it.trim() })

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        parsed = parseTMapShare(input)
                        hits = emptyList()
                        status = if (parsed == null) "TMap 링크를 찾지 못했습니다" else null
                    },
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ChargingBlue.copy(alpha = 0.2f),
                        contentColor = ChargingBlue,
                    ),
                ) {
                    Text("목적지 추출", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Button(
                    onClick = {
                        val info = parsed ?: parseTMapShare(input).also { parsed = it }
                        val query = info?.destinationName
                        if (query.isNullOrBlank()) {
                            status = "목적지 이름이 없어 검색할 수 없습니다"
                            return@Button
                        }
                        scope.launch {
                            loading = true
                            status = "검색 중..."
                            val result = geocoder.search(
                                query = query,
                                kakaoRestKey = kakaoKey.ifBlank { null },
                                googleApiKey = googleKey.ifBlank { null },
                            )
                            when (result) {
                                is GeocodeResult.Success -> {
                                    hits = result.hits
                                    status = if (result.hits.isEmpty()) "일치하는 장소 없음" else "${result.hits.size}건"
                                }
                                is GeocodeResult.Failure -> {
                                    hits = emptyList()
                                    status = "실패: ${result.message}"
                                }
                            }
                            loading = false
                        }
                    },
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !loading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BatteryGreen.copy(alpha = 0.2f),
                        contentColor = BatteryGreen,
                    ),
                ) {
                    Text(if (loading) "검색 중..." else "좌표 검색", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }

            val info = parsed
            if (info != null) {
                Spacer(Modifier.height(14.dp))
                Text("파싱 결과", fontSize = 11.sp, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF111418), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                ) {
                    DebugRow("목적지", info.destinationName.ifBlank { "-" })
                    DebugRow("URL", info.shortUrl)
                }
            }

            if (hits.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text("후보 좌표", fontSize = 11.sp, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                PlaceHitsList(hits = hits)
            }

            status?.let { msg ->
                Spacer(Modifier.height(10.dp))
                Text(
                    msg,
                    fontSize = 11.sp,
                    color = when {
                        msg.startsWith("실패") -> TeslaRed
                        msg.contains("건") || msg.contains("성공") -> BatteryGreen
                        else -> TextSecondary
                    },
                )
            }
        }

        // 차량 네비 전송 테스트
        Spacer(Modifier.height(28.dp))
        SectionTitle("차량 네비 전송 테스트")
        Spacer(Modifier.height(8.dp))
        NavSendTestCard()
    }
}

@Composable
private fun PlaceHitsList(hits: List<PlaceHit>) {
    val scope = rememberCoroutineScope()
    val teslaConfig by ServiceLocator.teslaConfigFlow.collectAsState()
    var sendingIdx by remember { mutableStateOf(-1) }
    var sendStatus by remember { mutableStateOf<Pair<Int, String>?>(null) }
    val teslaReady = teslaConfig != null && teslaConfig!!.accessToken.isNotBlank() && teslaConfig!!.vehicleId != 0L

    hits.forEachIndexed { idx, hit ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .background(Color(0xFF111418), RoundedCornerShape(10.dp))
                .padding(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "${idx + 1}. ${hit.name.ifBlank { "(이름 없음)" }}",
                    fontSize = 12.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(hit.provider, fontSize = 10.sp, color = TextSecondary)
            }
            if (hit.address.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(hit.address, fontSize = 11.sp, color = TextSecondary)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "lat ${formatCoord(hit.latitude)}  ·  lng ${formatCoord(hit.longitude)}",
                fontSize = 11.sp,
                color = ChargingBlue,
            )
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = {
                    val cfg = teslaConfig ?: return@Button
                    scope.launch {
                        sendingIdx = idx
                        sendStatus = idx to "차량 깨우는 중..."
                        runCatching { ServiceLocator.teslaApiClient.wakeUp(cfg) }
                        sendStatus = idx to "좌표 전송 중..."
                        val res = ServiceLocator.teslaApiClient.sendNavigationGps(cfg, hit.latitude, hit.longitude)
                        sendStatus = idx to if (res.result) "전송 완료" else "실패: ${res.reason}"
                        sendingIdx = -1
                    }
                },
                enabled = teslaReady && sendingIdx == -1,
                modifier = Modifier.fillMaxWidth().height(34.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TeslaRed.copy(alpha = 0.2f),
                    contentColor = TeslaRed,
                    disabledContainerColor = Color(0xFF2C2C2E),
                    disabledContentColor = TextSecondary,
                ),
            ) {
                Text(
                    when {
                        !teslaReady -> "Tesla 로그인 후 사용"
                        sendingIdx == idx -> "전송 중..."
                        else -> "차량 네비로 전송"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            sendStatus?.takeIf { it.first == idx }?.let { (_, msg) ->
                Spacer(Modifier.height(4.dp))
                Text(
                    msg,
                    fontSize = 10.sp,
                    color = when {
                        msg.startsWith("실패") -> TeslaRed
                        msg.contains("완료") -> BatteryGreen
                        else -> TextSecondary
                    },
                )
            }
        }
    }
}

@Composable
private fun NavSendTestCard() {
    val scope = rememberCoroutineScope()
    val teslaConfig by ServiceLocator.teslaConfigFlow.collectAsState()
    val teslaReady = teslaConfig != null && teslaConfig!!.accessToken.isNotBlank() && teslaConfig!!.vehicleId != 0L

    var latText by remember { mutableStateOf("37.5311868") }
    var lngText by remember { mutableStateOf("126.6486763") }
    var sending by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(20.dp))
            .padding(20.dp),
    ) {
        Text(
            "임의의 위도/경도를 차량 네비게이션으로 바로 전송합니다.",
            fontSize = 11.sp,
            color = TextSecondary,
        )

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = latText,
                onValueChange = { latText = it.trim() },
                label = { Text("위도 (lat)", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = ChargingBlue,
                    unfocusedBorderColor = Color(0xFF3A3A3C),
                    focusedLabelColor = ChargingBlue,
                    unfocusedLabelColor = TextSecondary,
                    cursorColor = ChargingBlue,
                ),
            )
            OutlinedTextField(
                value = lngText,
                onValueChange = { lngText = it.trim() },
                label = { Text("경도 (lng)", fontSize = 11.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = ChargingBlue,
                    unfocusedBorderColor = Color(0xFF3A3A3C),
                    focusedLabelColor = ChargingBlue,
                    unfocusedLabelColor = TextSecondary,
                    cursorColor = ChargingBlue,
                ),
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val cfg = teslaConfig ?: return@Button
                    val lat = latText.toDoubleOrNull()
                    val lng = lngText.toDoubleOrNull()
                    if (lat == null || lng == null) { status = "위도/경도 형식 오류"; return@Button }
                    scope.launch {
                        sending = true
                        status = "차량 깨우는 중..."
                        runCatching { ServiceLocator.teslaApiClient.wakeUp(cfg) }
                        status = "좌표 전송 중..."
                        val res = ServiceLocator.teslaApiClient.sendNavigationGps(cfg, lat, lng)
                        status = if (res.result) "전송 완료 ($lat, $lng)" else "실패: ${res.reason}"
                        sending = false
                    }
                },
                enabled = teslaReady && !sending,
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TeslaRed.copy(alpha = 0.2f),
                    contentColor = TeslaRed,
                    disabledContainerColor = Color(0xFF2C2C2E),
                    disabledContentColor = TextSecondary,
                ),
            ) {
                Text(
                    when {
                        !teslaReady -> "Tesla 로그인 필요"
                        sending -> "전송 중..."
                        else -> "네비로 전송 (GPS)"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Button(
                onClick = {
                    val cfg = teslaConfig ?: return@Button
                    val lat = latText.toDoubleOrNull()
                    val lng = lngText.toDoubleOrNull()
                    if (lat == null || lng == null) { status = "위도/경도 형식 오류"; return@Button }
                    scope.launch {
                        sending = true
                        status = "차량 깨우는 중..."
                        runCatching { ServiceLocator.teslaApiClient.wakeUp(cfg) }
                        status = "공유 전송 중..."
                        val text = "https://maps.google.com/?q=$lat,$lng"
                        val res = ServiceLocator.teslaApiClient.shareToVehicle(cfg, text, subject = "테스트 목적지")
                        status = if (res.result) "공유 완료" else "공유 실패: ${res.reason}"
                        sending = false
                    }
                },
                enabled = teslaReady && !sending,
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ChargingBlue.copy(alpha = 0.2f),
                    contentColor = ChargingBlue,
                    disabledContainerColor = Color(0xFF2C2C2E),
                    disabledContentColor = TextSecondary,
                ),
            ) {
                Text(
                    when {
                        !teslaReady -> "Tesla 로그인 필요"
                        sending -> "전송 중..."
                        else -> "공유 (share)"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        status?.let { msg ->
            Spacer(Modifier.height(10.dp))
            Text(
                msg,
                fontSize = 12.sp,
                color = when {
                    msg.startsWith("실패") || msg.contains("오류") -> TeslaRed
                    msg.contains("완료") -> BatteryGreen
                    else -> TextSecondary
                },
            )
        }

        if (teslaReady) {
            Spacer(Modifier.height(8.dp))
            Text("Vehicle ID: ${teslaConfig!!.vehicleId}", fontSize = 10.sp, color = TextSecondary)
        }
    }
}

private fun formatCoord(v: Double): String {
    val scaled = (v * 1_000_000).toLong().toDouble() / 1_000_000
    return scaled.toString()
}
