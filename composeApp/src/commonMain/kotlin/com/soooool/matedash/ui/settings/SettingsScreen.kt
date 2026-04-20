package com.soooool.matedash.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalUriHandler
import com.soooool.matedash.ServiceLocator
import com.soooool.matedash.getPlatform
import com.soooool.matedash.data.api.TeslaApiConfig
import com.soooool.matedash.data.api.TeslaOAuth
import com.soooool.matedash.data.persistence.LiveActivityDebug
import com.soooool.matedash.data.persistence.clearApiConfig
import com.soooool.matedash.data.persistence.readLiveActivityDebug
import com.soooool.matedash.data.persistence.startTestLiveActivity
import com.soooool.matedash.data.persistence.startTestDrivingLiveActivity
import com.soooool.matedash.data.persistence.stopTestLiveActivity
import com.soooool.matedash.data.share.GeocodeResult
import com.soooool.matedash.data.share.PlaceGeocoder
import com.soooool.matedash.data.share.PlaceHit
import com.soooool.matedash.data.share.TMapShareInfo
import com.soooool.matedash.data.share.clearSharedText
import com.soooool.matedash.data.share.parseSharedPlace
import com.soooool.matedash.data.share.parseTMapShare
import com.soooool.matedash.data.share.readShareExtensionLog
import com.soooool.matedash.data.share.readSharedText
import com.soooool.matedash.data.share.writeTestSharedText
import kotlinx.coroutines.launch

private val DarkBg = Color(0xFF0B0B0B)
private val CardBg = Color(0xFF1A1A1A)
private val TeslaRed = Color(0xFFE31937)
private val ChargingBlue = Color(0xFF00C7FF)
private val BatteryGreen = Color(0xFF34C759)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFF8E8E93)

@Composable
fun SettingsScreen(onDisconnect: () -> Unit) {
    val config = ServiceLocator.currentConfig
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var sharedText by remember { mutableStateOf(readSharedText()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 16.dp)
            .padding(top = statusBarTop)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(24.dp))

        Text(
            text = "설정",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
        )

        // TeslaMate 연결 정보
        Spacer(Modifier.height(24.dp))
        SectionTitle("TeslaMate 연결")
        Spacer(Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg, RoundedCornerShape(20.dp))
                .padding(20.dp),
        ) {
            SettingRow(label = "연결 서버", value = config?.baseUrl ?: "-")
            Spacer(Modifier.height(16.dp))
            SettingRow(label = "차량 ID", value = config?.carId?.toString() ?: "-")
        }

        // 기능 설정
        Spacer(Modifier.height(28.dp))
        SectionTitle("기능 설정")
        Spacer(Modifier.height(8.dp))
        FeatureSettingsCard()

        // 공유 수신
        if (getPlatform().name.lowercase().contains("ios")) {
            Spacer(Modifier.height(28.dp))
            SectionTitle("공유 수신")
            Spacer(Modifier.height(8.dp))
            SharedTextCard(
                sharedText = sharedText,
                onRefresh = { sharedText = readSharedText() },
            )
        }

        // TMap 링크 좌표 추출 (확인용)
        Spacer(Modifier.height(28.dp))
        SectionTitle("TMap 링크 좌표 추출 (확인용)")
        Spacer(Modifier.height(8.dp))
        TMapLinkCard(
            sharedText = sharedText,
            onRefreshShared = { sharedText = readSharedText() },
        )

        // 좌표 → 차량 네비 전송 테스트
        Spacer(Modifier.height(28.dp))
        SectionTitle("차량 네비 전송 테스트")
        Spacer(Modifier.height(8.dp))
        NavSendTestCard()

        // Tesla API 설정
        Spacer(Modifier.height(28.dp))
        SectionTitle("Tesla Fleet API")
        Spacer(Modifier.height(8.dp))
        TeslaApiSettingsCard()

        // 연결 해제
        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                ServiceLocator.repository.stopPolling()
                ServiceLocator.currentConfig = null
                clearApiConfig()
                onDisconnect()
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TeslaRed,
                contentColor = Color.White,
            ),
        ) {
            Text("연결 해제", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(100.dp))
    }
}

@Composable
private fun FeatureSettingsCard() {
    val settings by ServiceLocator.settingsFlow.collectAsState()
    val isIos = getPlatform().name.lowercase().contains("ios")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(20.dp))
            .padding(20.dp),
    ) {
        // Live Activity 설정은 iOS 전용
        if (isIos) {
            SettingSwitch(
                label = "Live Activity",
                description = "충전/주행 시 잠금화면과 Dynamic Island에 표시",
                checked = settings.liveActivityEnabled,
                onCheckedChange = { ServiceLocator.appSettings = settings.copy(liveActivityEnabled = it) },
            )

            Spacer(Modifier.height(16.dp))

            SettingSwitch(
                label = "충전 Live Activity",
                description = "충전 중 실시간 상태 표시",
                checked = settings.liveActivityChargingEnabled,
                enabled = settings.liveActivityEnabled,
                onCheckedChange = { ServiceLocator.appSettings = settings.copy(liveActivityChargingEnabled = it) },
            )

            Spacer(Modifier.height(16.dp))

            SettingSwitch(
                label = "주행 Live Activity",
                description = "주행 중 속도, 배터리 표시",
                checked = settings.liveActivityDrivingEnabled,
                enabled = settings.liveActivityEnabled,
                onCheckedChange = { ServiceLocator.appSettings = settings.copy(liveActivityDrivingEnabled = it) },
            )

            Spacer(Modifier.height(16.dp))

            SettingSwitch(
                label = "슈퍼차저 제외",
                description = "슈퍼차저 충전 시 Live Activity 비활성 (Tesla 앱 사용)",
                checked = settings.excludeSupercharger,
                enabled = settings.liveActivityEnabled && settings.liveActivityChargingEnabled,
                onCheckedChange = { ServiceLocator.appSettings = settings.copy(excludeSupercharger = it) },
            )

            Spacer(Modifier.height(16.dp))

            // Live Activity 테스트 버튼
            Text("테스트", fontSize = 12.sp, color = TextSecondary)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { startTestLiveActivity() },
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BatteryGreen.copy(alpha = 0.2f),
                        contentColor = BatteryGreen,
                    ),
                ) {
                    Text("충전 테스트", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Button(
                    onClick = { startTestDrivingLiveActivity() },
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ChargingBlue.copy(alpha = 0.2f),
                        contentColor = ChargingBlue,
                    ),
                ) {
                    Text("주행 테스트", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Button(
                    onClick = { stopTestLiveActivity() },
                    modifier = Modifier.weight(1f).height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TeslaRed.copy(alpha = 0.2f),
                        contentColor = TeslaRed,
                    ),
                ) {
                    Text("종료", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(16.dp))

            LiveActivityDebugPanel()

            Spacer(Modifier.height(16.dp))
        }

        SettingSwitch(
            label = "지도 표시",
            description = "대시보드에 현재 위치 지도 표시",
            checked = settings.mapEnabled,
            onCheckedChange = { ServiceLocator.appSettings = settings.copy(mapEnabled = it) },
        )

        Spacer(Modifier.height(20.dp))

        // 갱신 주기
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
    }
}

@Composable
private fun SettingSwitch(
    label: String,
    description: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                fontSize = 15.sp,
                color = if (enabled) TextPrimary else TextSecondary,
                fontWeight = FontWeight.Medium,
            )
            Text(
                description,
                fontSize = 11.sp,
                color = TextSecondary,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = BatteryGreen,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = Color(0xFF2C2C2E),
            ),
        )
    }
}

@Composable
private fun TeslaApiSettingsCard() {
    val existingConfig by ServiceLocator.teslaConfigFlow.collectAsState()
    val isLoggedIn = existingConfig != null && existingConfig!!.accessToken.isNotBlank()
    val scope = rememberCoroutineScope()

    var baseUrl by remember { mutableStateOf(existingConfig?.baseUrl ?: "https://fleet-api.prd.na.vn.cloud.tesla.com") }
    // existingConfig이 변경될 때 baseUrl 갱신은 불필요 (초기값만 사용)
    var accessToken by remember { mutableStateOf("") }
    var refreshToken by remember { mutableStateOf("") }
    var statusMsg by remember { mutableStateOf<String?>(null) }
    var isConnecting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (isLoggedIn) {
            val config = existingConfig!!
            // 연결 상태
            Column {
                Text("연결됨", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = BatteryGreen)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Token: ...${config.accessToken.takeLast(8)}",
                    fontSize = 12.sp,
                    color = TextSecondary,
                )
                if (config.vehicleId != 0L) {
                    Text("Vehicle ID: ${config.vehicleId}", fontSize = 12.sp, color = TextSecondary)
                }
            }

            Button(
                onClick = {
                    ServiceLocator.teslaApiConfig = null
                    accessToken = ""
                    statusMsg = null
                },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E), contentColor = TextSecondary),
            ) {
                Text("연결 해제", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        } else {
            // OAuth 로그인 플로우
            val uriHandler = LocalUriHandler.current
            var authCode by remember { mutableStateOf("") }
            var showCodeInput by remember { mutableStateOf(false) }

            Text("API Endpoint (Region)", fontSize = 12.sp, color = TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RegionChip("NA", "https://fleet-api.prd.na.vn.cloud.tesla.com", baseUrl) { baseUrl = it }
                RegionChip("EU", "https://fleet-api.prd.eu.vn.cloud.tesla.com", baseUrl) { baseUrl = it }
                RegionChip("CN", "https://fleet-api.prd.cn.vn.cloud.tesla.cn", baseUrl) { baseUrl = it }
            }

            if (!showCodeInput) {
                // 1단계: Tesla 로그인 버튼
                Button(
                    onClick = {
                        val authUrl = TeslaOAuth.buildAuthUrl(TeslaOAuth.CLIENT_ID)
                        println("[MateDash] Opening Tesla OAuth: $authUrl")
                        uriHandler.openUri(authUrl)
                        showCodeInput = true
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ChargingBlue, contentColor = Color.White),
                ) {
                    Text("Tesla 계정으로 로그인", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }

                Text(
                    "브라우저에서 Tesla 로그인 후 리다이렉트된 URL을 붙여넣어주세요",
                    fontSize = 11.sp,
                    color = TextSecondary,
                )
            } else {
                // 2단계: 콜백 URL 또는 코드 입력
                Text(
                    "로그인 후 리다이렉트된 URL 전체를 붙여넣어주세요",
                    fontSize = 12.sp,
                    color = TextSecondary,
                )

                TeslaTextField(
                    label = "콜백 URL 또는 인증 코드",
                    value = authCode,
                    onValueChange = { authCode = it },
                )

                Button(
                    onClick = {
                        // URL에서 code 파라미터 추출 또는 코드 직접 사용
                        val code = if (authCode.contains("code=")) {
                            authCode.substringAfter("code=").substringBefore("&")
                        } else {
                            authCode.trim()
                        }
                        val verifier = TeslaOAuth.getCodeVerifier()
                        if (code.isBlank() || verifier == null) {
                            statusMsg = "인증 코드를 확인해주세요"
                            return@Button
                        }
                        scope.launch {
                            isConnecting = true
                            statusMsg = "토큰 교환 중..."
                            try {
                                val token = ServiceLocator.teslaApiClient.exchangeCodeForToken(
                                    clientId = TeslaOAuth.CLIENT_ID,
                                    code = code,
                                    codeVerifier = verifier,
                                )
                                println("[MateDash] OAuth 토큰 획득 성공")
                                val config = TeslaApiConfig(
                                    baseUrl = baseUrl,
                                    accessToken = token.accessToken,
                                    refreshToken = token.refreshToken,
                                    clientId = TeslaOAuth.CLIENT_ID,
                                )
                                // 리전 등록 (첫 사용 시 필요)
                                statusMsg = "리전 등록 중..."
                                ServiceLocator.teslaApiClient.registerPartnerAccount(config)
                                val vehicles = ServiceLocator.teslaApiClient.getVehicles(config)
                                println("[MateDash] 차량 ${vehicles.size}대 확인")
                                // 차량이 1대면 자동 선택
                                val finalConfig = if (vehicles.size == 1) {
                                    config.copy(vehicleId = vehicles.first().id)
                                } else {
                                    config
                                }
                                ServiceLocator.teslaApiConfig = finalConfig
                                statusMsg = if (vehicles.size == 1) {
                                    "연결 성공! (${vehicles.first().displayName})"
                                } else {
                                    "연결 성공! (${vehicles.size}대 - 차량 탭에서 선택)"
                                }
                            } catch (e: Exception) {
                                println("[MateDash] OAuth error: ${e.message}")
                                statusMsg = "실패: ${e.message}"
                            }
                            isConnecting = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ChargingBlue, contentColor = Color.White),
                    enabled = authCode.isNotBlank() && !isConnecting,
                ) {
                    Text(
                        if (isConnecting) "연결 중..." else "연결",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                // 다시 시도 버튼
                Button(
                    onClick = { showCodeInput = false; authCode = ""; statusMsg = null },
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E), contentColor = TextSecondary),
                ) {
                    Text("다시 시도", fontSize = 13.sp)
                }
            }
        }

        if (statusMsg != null) {
            Text(
                text = statusMsg!!,
                fontSize = 12.sp,
                color = if (statusMsg!!.contains("성공")) BatteryGreen
                       else if (statusMsg!!.contains("실패")) TeslaRed
                       else TextSecondary,
            )
        }
    }
}

@Composable
private fun RegionChip(label: String, url: String, selected: String, onSelect: (String) -> Unit) {
    val isSelected = selected == url
    Button(
        onClick = { onSelect(url) },
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) ChargingBlue.copy(alpha = 0.2f) else Color(0xFF2C2C2E),
            contentColor = if (isSelected) ChargingBlue else TextSecondary,
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 4.dp),
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TeslaTextField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
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

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary,
    )
}

@Composable
private fun SettingRow(label: String, value: String) {
    Column {
        Text(label, fontSize = 12.sp, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 15.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun LiveActivityDebugPanel() {
    var debug by remember { mutableStateOf<LiveActivityDebug>(readLiveActivityDebug()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF111418), RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("LA 디버그 스냅샷", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            Button(
                onClick = { debug = readLiveActivityDebug() },
                modifier = Modifier.height(28.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ChargingBlue.copy(alpha = 0.2f),
                    contentColor = ChargingBlue,
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp),
            ) {
                Text("새로고침", fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(Modifier.height(10.dp))

        DebugRow("마지막 갱신", debug.lastUpdate.ifBlank { "-" })
        DebugRow("state", debug.rawState.ifBlank { "-" })
        DebugRow("shift_state", debug.rawShiftState.ifBlank { "(null/공백)" })
        DebugRow("speed", "${debug.rawSpeed} km/h")
        DebugRow("power", "${debug.rawPower} kW")
        DebugRow("charging_state", debug.rawChargingState.ifBlank { "-" })
        DebugRow("isDriving 판정", if (debug.isDriving) "true" else "false", if (debug.isDriving) BatteryGreen else TextSecondary)
        DebugRow("isCharging 판정", if (debug.isCharging) "true" else "false", if (debug.isCharging) BatteryGreen else TextSecondary)

        Spacer(Modifier.height(8.dp))
        Text("Swift 로그 (마지막 액션)", fontSize = 11.sp, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        DebugRow("driving", debug.drivingLast.ifBlank { "-" })
        DebugRow("charging", debug.chargingLast.ifBlank { "-" })
    }
}

@Composable
private fun DebugRow(label: String, value: String, valueColor: Color = TextPrimary) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, fontSize = 11.sp, color = TextSecondary)
        Text(value, fontSize = 11.sp, color = valueColor, fontWeight = FontWeight.Medium)
    }
}

private const val NAVER_SAMPLE_TEXT = "[네이버지도]\n하나로마트 군자농협대부점\n경기 안산시 단원구 대부중앙로 142\nhttps://naver.me/I5caIqaF"

@Composable
private fun SharedTextCard(
    sharedText: String?,
    onRefresh: () -> Unit,
) {
    var logs by remember { mutableStateOf(readShareExtensionLog()) }
    val raw = sharedText

    fun refresh() {
        onRefresh()
        logs = readShareExtensionLog()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(20.dp))
            .padding(20.dp),
    ) {
        Text(
            "다른 앱에서 MateDash로 공유한 텍스트가 여기 표시됩니다",
            fontSize = 12.sp,
            color = TextSecondary,
        )

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { refresh() },
                modifier = Modifier.weight(1f).height(38.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ChargingBlue.copy(alpha = 0.2f),
                    contentColor = ChargingBlue,
                ),
            ) {
                Text("새로고침", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Button(
                onClick = {
                    writeTestSharedText(NAVER_SAMPLE_TEXT)
                    refresh()
                },
                modifier = Modifier.weight(1f).height(38.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BatteryGreen.copy(alpha = 0.2f),
                    contentColor = BatteryGreen,
                ),
            ) {
                Text("샘플 주입", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Button(
                onClick = {
                    clearSharedText()
                    refresh()
                },
                modifier = Modifier.weight(1f).height(38.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TeslaRed.copy(alpha = 0.2f),
                    contentColor = TeslaRed,
                ),
            ) {
                Text("초기화", fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }

        val current = raw
        if (current.isNullOrBlank()) {
            Spacer(Modifier.height(14.dp))
            Text("아직 받은 공유 없음", fontSize = 13.sp, color = TextSecondary)
        } else {
            val place = parseSharedPlace(current)

            Spacer(Modifier.height(14.dp))
            Text("원문", fontSize = 11.sp, color = TextSecondary)
            Spacer(Modifier.height(4.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF111418), RoundedCornerShape(10.dp))
                    .padding(10.dp),
            ) {
                Text(current, fontSize = 12.sp, color = TextPrimary)
            }

            if (place != null) {
                Spacer(Modifier.height(14.dp))
                Text("추출 결과", fontSize = 11.sp, color = TextSecondary)
                Spacer(Modifier.height(6.dp))
                DebugRow("출처", place.source.ifBlank { "-" })
                DebugRow("장소", place.name.ifBlank { "-" })
                DebugRow("주소", place.address.ifBlank { "-" })
                DebugRow("URL", place.url.ifBlank { "-" })
            }
        }

        Spacer(Modifier.height(14.dp))
        Text("Share Extension 로그 (최신 20줄)", fontSize = 11.sp, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111418), RoundedCornerShape(10.dp))
                .padding(10.dp),
        ) {
            if (logs.isEmpty()) {
                Text("아직 로그 없음 (공유시트에서 공유하면 기록됩니다)", fontSize = 11.sp, color = TextSecondary)
            } else {
                logs.forEach { line ->
                    Text(line, fontSize = 11.sp, color = TextPrimary)
                }
            }
        }
    }
}

private const val TMAP_SAMPLE_TEXT = "불꽃교회에 오후 11시 36분 도착 예정입니다 https://tmap.life/c80f8e32"

@Composable
private fun TMapLinkCard(
    sharedText: String?,
    onRefreshShared: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val geocoder = remember { PlaceGeocoder() }

    var input by remember { mutableStateOf(sharedText?.takeIf { it.isNotBlank() } ?: TMAP_SAMPLE_TEXT) }
    var lastAutoFilled by remember { mutableStateOf(sharedText) }
    var kakaoKey by remember { mutableStateOf("") }
    var googleKey by remember { mutableStateOf("") }
    var parsed by remember { mutableStateOf<TMapShareInfo?>(null) }
    var hits by remember { mutableStateOf<List<PlaceHit>>(emptyList()) }
    var status by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(sharedText) {
        if (!sharedText.isNullOrBlank() && sharedText != lastAutoFilled) {
            input = sharedText
            lastAutoFilled = sharedText
            parsed = null
            hits = emptyList()
            status = "공유 원문을 불러왔습니다"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(20.dp))
            .padding(20.dp),
    ) {
        Text(
            "TMap 단축링크 + 목적지명 메시지를 붙여넣으면 지명으로 좌표를 역추출합니다.\n" +
                "Kakao REST 키를 넣으면 국내 POI 정확도가 올라가고, 비워두면 OpenStreetMap(Nominatim)으로 검색합니다.",
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

        OutlinedTextField(
            value = kakaoKey,
            onValueChange = { kakaoKey = it.trim() },
            label = { Text("Kakao REST API 키 (선택)", fontSize = 12.sp) },
            singleLine = true,
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

        OutlinedTextField(
            value = googleKey,
            onValueChange = { googleKey = it.trim() },
            label = { Text("Google Places API 키 (선택)", fontSize = 12.sp) },
            singleLine = true,
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
            Button(
                onClick = {
                    onRefreshShared()
                    val shared = readSharedText()?.takeIf { it.isNotBlank() }
                    if (shared != null) {
                        input = shared
                        lastAutoFilled = shared
                        status = "공유 원문을 불러왔습니다"
                    } else {
                        input = TMAP_SAMPLE_TEXT
                        status = "공유 원문이 없어 샘플을 넣었습니다"
                    }
                    parsed = null
                    hits = emptyList()
                },
                modifier = Modifier.weight(1f).height(40.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2C2C2E),
                    contentColor = TextSecondary,
                ),
            ) {
                Text("공유 원문", fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
                                sendStatus = idx to if (res.result) "전송 완료 ✓" else "실패: ${res.reason}"
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
                            if (!teslaReady) "Tesla 로그인 후 사용"
                            else if (sendingIdx == idx) "전송 중..."
                            else "차량 네비로 전송",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    sendStatus?.takeIf { it.first == idx }?.let { (_, msg) ->
                        Spacer(Modifier.height(4.dp))
                        Text(
                            msg,
                            fontSize = 10.sp,
                            color = if (msg.startsWith("실패")) TeslaRed
                                    else if (msg.contains("완료")) BatteryGreen
                                    else TextSecondary,
                        )
                    }
                }
            }
        }

        status?.let { msg ->
            Spacer(Modifier.height(10.dp))
            Text(
                msg,
                fontSize = 11.sp,
                color = if (msg.startsWith("실패")) TeslaRed
                        else if (msg.contains("건") || msg.contains("성공")) BatteryGreen
                        else TextSecondary,
            )
        }
    }
}

private fun formatCoord(v: Double): String {
    val scaled = (v * 1_000_000).toLong().toDouble() / 1_000_000
    return scaled.toString()
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
            "임의의 위도/경도를 차량 네비게이션으로 바로 전송합니다. (Tesla Fleet API: navigation_gps_request)",
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
                    if (lat == null || lng == null) {
                        status = "위도/경도 형식 오류"
                        return@Button
                    }
                    scope.launch {
                        sending = true
                        status = "차량 깨우는 중..."
                        runCatching { ServiceLocator.teslaApiClient.wakeUp(cfg) }
                        status = "좌표 전송 중..."
                        val res = ServiceLocator.teslaApiClient.sendNavigationGps(cfg, lat, lng)
                        status = if (res.result) "전송 완료 ✓ ($lat, $lng)" else "실패: ${res.reason}"
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
                    if (!teslaReady) "Tesla 로그인 필요"
                    else if (sending) "전송 중..."
                    else "네비로 전송 (GPS)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Button(
                onClick = {
                    val cfg = teslaConfig ?: return@Button
                    val lat = latText.toDoubleOrNull()
                    val lng = lngText.toDoubleOrNull()
                    if (lat == null || lng == null) {
                        status = "위도/경도 형식 오류"
                        return@Button
                    }
                    scope.launch {
                        sending = true
                        status = "차량 깨우는 중..."
                        runCatching { ServiceLocator.teslaApiClient.wakeUp(cfg) }
                        status = "공유 전송 중..."
                        val text = "https://maps.google.com/?q=$lat,$lng"
                        val res = ServiceLocator.teslaApiClient.shareToVehicle(cfg, text, subject = "테스트 목적지")
                        status = if (res.result) "공유 완료 ✓" else "공유 실패: ${res.reason}"
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
                    if (!teslaReady) "Tesla 로그인 필요"
                    else if (sending) "전송 중..."
                    else "공유 (share)",
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
                color = if (msg.startsWith("실패") || msg.contains("오류")) TeslaRed
                        else if (msg.contains("완료")) BatteryGreen
                        else TextSecondary,
            )
        }

        if (teslaReady) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Vehicle ID: ${teslaConfig!!.vehicleId}",
                fontSize = 10.sp,
                color = TextSecondary,
            )
        }
    }
}
