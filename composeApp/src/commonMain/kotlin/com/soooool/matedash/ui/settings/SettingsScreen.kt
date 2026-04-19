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
import com.soooool.matedash.data.persistence.clearApiConfig
import com.soooool.matedash.data.persistence.startTestLiveActivity
import com.soooool.matedash.data.persistence.startTestDrivingLiveActivity
import com.soooool.matedash.data.persistence.stopTestLiveActivity
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
