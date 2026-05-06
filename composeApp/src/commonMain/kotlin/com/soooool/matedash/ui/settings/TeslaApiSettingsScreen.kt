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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soooool.matedash.ServiceLocator
import com.soooool.matedash.data.api.TeslaApiConfig
import com.soooool.matedash.data.api.TeslaOAuth
import kotlinx.coroutines.launch

@Composable
internal fun TeslaApiSettingsScreen(onBack: () -> Unit) {
    val existingConfig by ServiceLocator.teslaConfigFlow.collectAsState()
    val isLoggedIn = existingConfig != null && existingConfig!!.accessToken.isNotBlank()
    val scope = rememberCoroutineScope()

    var baseUrl by remember { mutableStateOf(existingConfig?.baseUrl ?: "https://fleet-api.prd.na.vn.cloud.tesla.com") }
    var statusMsg by remember { mutableStateOf<String?>(null) }
    var isConnecting by remember { mutableStateOf(false) }

    SettingsDetailScaffold(title = "Tesla Fleet API", onBack = onBack) {
        if (isLoggedIn) {
            LoggedInSection(
                onLogout = {
                    ServiceLocator.teslaApiConfig = null
                    statusMsg = null
                },
            )
        } else {
            LoginSection(
                baseUrl = baseUrl,
                onBaseUrlChange = { baseUrl = it },
                isConnecting = isConnecting,
                onConnect = { code, verifier ->
                    scope.launch {
                        isConnecting = true
                        statusMsg = "토큰 교환 중..."
                        try {
                            val token = ServiceLocator.teslaApiClient.exchangeCodeForToken(
                                clientId = TeslaOAuth.CLIENT_ID,
                                code = code,
                                codeVerifier = verifier,
                            )
                            val config = TeslaApiConfig(
                                baseUrl = baseUrl,
                                accessToken = token.accessToken,
                                refreshToken = token.refreshToken,
                                clientId = TeslaOAuth.CLIENT_ID,
                            )
                            statusMsg = "리전 등록 중..."
                            ServiceLocator.teslaApiClient.registerPartnerAccount(config)
                            val vehicles = ServiceLocator.teslaApiClient.getVehicles(config)
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
                            statusMsg = "실패: ${e.message}"
                        }
                        isConnecting = false
                    }
                },
                onStatusUpdate = { statusMsg = it },
            )
        }

        // VIN 기반 가상 키 등록
        Spacer(Modifier.height(20.dp))
        SectionTitle("가상 키 등록")
        Spacer(Modifier.height(8.dp))
        VirtualKeySection()

        statusMsg?.let { msg ->
            Spacer(Modifier.height(12.dp))
            Text(
                text = msg,
                fontSize = 12.sp,
                color = when {
                    msg.contains("성공") -> BatteryGreen
                    msg.contains("실패") -> TeslaRed
                    else -> TextSecondary
                },
            )
        }
    }
}

@Composable
private fun LoggedInSection(onLogout: () -> Unit) {
    val config = ServiceLocator.teslaConfigFlow.collectAsState().value!!

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
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
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E), contentColor = TextSecondary),
        ) {
            Text("연결 해제", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun LoginSection(
    baseUrl: String,
    onBaseUrlChange: (String) -> Unit,
    isConnecting: Boolean,
    onConnect: (code: String, verifier: String) -> Unit,
    onStatusUpdate: (String?) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    var authCode by remember { mutableStateOf("") }
    var showCodeInput by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("API Endpoint (Region)", fontSize = 12.sp, color = TextSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RegionChip("NA", "https://fleet-api.prd.na.vn.cloud.tesla.com", baseUrl) { onBaseUrlChange(it) }
            RegionChip("EU", "https://fleet-api.prd.eu.vn.cloud.tesla.com", baseUrl) { onBaseUrlChange(it) }
            RegionChip("CN", "https://fleet-api.prd.cn.vn.cloud.tesla.cn", baseUrl) { onBaseUrlChange(it) }
        }

        if (!showCodeInput) {
            Button(
                onClick = {
                    val authUrl = TeslaOAuth.buildAuthUrl(TeslaOAuth.CLIENT_ID)
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
                    val code = if (authCode.contains("code=")) {
                        authCode.substringAfter("code=").substringBefore("&")
                    } else {
                        authCode.trim()
                    }
                    val verifier = TeslaOAuth.getCodeVerifier()
                    if (code.isBlank() || verifier == null) {
                        onStatusUpdate("인증 코드를 확인해주세요")
                        return@Button
                    }
                    onConnect(code, verifier)
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

            Button(
                onClick = { showCodeInput = false; authCode = ""; onStatusUpdate(null) },
                modifier = Modifier.fillMaxWidth().height(36.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E), contentColor = TextSecondary),
            ) {
                Text("다시 시도", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun VirtualKeySection() {
    val uriHandler = LocalUriHandler.current
    var vin by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Tesla 앱에서 가상 키를 승인하면 차량 커맨드를 직접 전송할 수 있습니다.",
            fontSize = 12.sp,
            color = TextSecondary,
        )

        TeslaTextField(
            label = "차량 VIN",
            value = vin,
            onValueChange = { vin = it.trim().uppercase() },
        )

        Button(
            onClick = {
                val deepLink = if (vin.isNotBlank()) {
                    "https://tesla.com/_ak/cleusewitz.github.io?vin=$vin"
                } else {
                    "https://tesla.com/_ak/cleusewitz.github.io"
                }
                uriHandler.openUri(deepLink)
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TeslaRed.copy(alpha = 0.2f),
                contentColor = TeslaRed,
            ),
        ) {
            Text(
                if (vin.isBlank()) "가상 키 등록 (차량 선택)" else "가상 키 등록",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Text(
            "Tesla 앱이 열리면 차주가 승인해야 합니다. VIN을 비워두면 Tesla 앱에서 차량을 선택합니다.",
            fontSize = 11.sp,
            color = TextSecondary,
        )
    }
}
