package com.soooool.matedash.ui.connection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soooool.matedash.data.api.TeslaOAuth

private val DarkBg = Color(0xFF0D0D0D)
private val CardBg = Color(0xFF1C1C1E)
private val TeslaRed = Color(0xFFE31937)
private val ChargingBlue = Color(0xFF00C7FF)
private val BatteryGreen = Color(0xFF34C759)
private val TextSecondary = Color(0xFF8E8E93)
private val FieldBorder = Color(0xFF3A3A3C)

@Composable
fun ConnectionScreen(onConnected: () -> Unit) {
    val vm = viewModel { ConnectionViewModel() }
    val state by vm.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(state.errorMessage) {
        if (state.errorMessage != null) {
            kotlinx.coroutines.delay(5000)
            vm.clearError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .safeContentPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))

        Text("MateDash", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(
            text = "Tesla 차량 대시보드",
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 4.dp),
        )

        Spacer(Modifier.height(28.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = CardBg,
            contentColor = Color.White,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    androidx.compose.material3.HorizontalDivider(
                        modifier = Modifier
                            .tabIndicatorOffset(tabPositions[selectedTab])
                            .height(3.dp),
                        color = TeslaRed,
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("TeslaMate (전체 기능)", fontSize = 12.sp) },
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Tesla 직접 (Fleet API)", fontSize = 12.sp) },
            )
        }

        Spacer(Modifier.height(20.dp))

        when (selectedTab) {
            0 -> TeslaMateTab(vm, state, onConnected)
            1 -> TeslaDirectTab(vm, state, onConnected)
        }

        Spacer(Modifier.height(16.dp))
        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage!!,
                color = TeslaRed,
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        }
        if (state.statusMessage != null) {
            Text(
                text = state.statusMessage!!,
                color = if (state.statusMessage!!.contains("성공") || state.statusMessage!!.contains("연결됨")) BatteryGreen else TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun TeslaMateTab(
    vm: ConnectionViewModel,
    state: ConnectionUiState,
    onConnected: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "TeslaMate 서버",
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
        ApiTextField(
            value = state.host,
            onValueChange = vm::onHostChange,
            label = "호스트",
            placeholder = "예: 192.168.0.10 또는 my.synology.me",
            keyboardType = KeyboardType.Uri,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ApiTextField(
                value = state.port,
                onValueChange = vm::onPortChange,
                label = "포트",
                placeholder = "9999",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
            ApiTextField(
                value = state.carId,
                onValueChange = vm::onCarIdChange,
                label = "차량 ID (보통 1)",
                placeholder = "1",
                keyboardType = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(12.dp))
        ApiTextField(
            value = state.apiToken,
            onValueChange = vm::onApiTokenChange,
            label = "API 토큰 (선택)",
            placeholder = "TeslaMate 인증 켰을 때만",
            isPassword = true,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "TeslaMate 연결 시: 대시보드, 클러스터, 주행/충전 이력, MQTT 1초 갱신 모두 가능",
            color = TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))
        ConnectButton(
            text = "TeslaMate 연결",
            isConnecting = state.isConnecting,
            onClick = { vm.connectTeslaMate(onConnected) },
        )
    }
}

@Composable
private fun TeslaDirectTab(
    vm: ConnectionViewModel,
    state: ConnectionUiState,
    onConnected: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    Column(modifier = Modifier.fillMaxWidth()) {
        if (state.teslaConnected) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBg, RoundedCornerShape(12.dp))
                    .padding(16.dp),
            ) {
                Text("Tesla 계정 연결됨 ✅", color = BatteryGreen, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "대시보드 + 클러스터 + 차량 명령 사용 가능. 주행/충전 이력은 표시되지 않음.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("VIN (선택, 가상 키 등록용)", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            ApiTextField(
                value = state.teslaVin,
                onValueChange = vm::onTeslaVinChange,
                label = "차량 VIN (17자)",
                placeholder = "5YJSA1E26HF000337",
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val deepLink = if (state.teslaVin.isNotBlank()) {
                        "https://tesla.com/_ak/cleusewitz.github.io?vin=${state.teslaVin}"
                    } else {
                        "https://tesla.com/_ak/cleusewitz.github.io"
                    }
                    uriHandler.openUri(deepLink)
                },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TeslaRed.copy(alpha = 0.2f),
                    contentColor = TeslaRed,
                ),
            ) {
                Text(
                    if (state.teslaVin.isBlank()) "가상 키 등록 (Tesla 앱에서 차량 선택)" else "VIN으로 가상 키 등록",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(20.dp))
            ConnectButton(
                text = "이 설정으로 시작",
                isConnecting = state.isConnecting,
                onClick = { vm.proceedWithExistingTesla(onConnected) },
            )
        } else {
            Text("Tesla 계정 OAuth", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                RegionChip("NA", "https://fleet-api.prd.na.vn.cloud.tesla.com", state.teslaBaseUrl) { vm.onTeslaBaseUrlChange(it) }
                RegionChip("EU", "https://fleet-api.prd.eu.vn.cloud.tesla.com", state.teslaBaseUrl) { vm.onTeslaBaseUrlChange(it) }
                RegionChip("CN", "https://fleet-api.prd.cn.vn.cloud.tesla.cn", state.teslaBaseUrl) { vm.onTeslaBaseUrlChange(it) }
            }

            Spacer(Modifier.height(16.dp))

            if (!state.teslaShowCodeInput) {
                Button(
                    onClick = {
                        uriHandler.openUri(TeslaOAuth.buildAuthUrl(TeslaOAuth.CLIENT_ID))
                        vm.onTeslaShowCodeInputChange(true)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ChargingBlue, contentColor = Color.White),
                ) {
                    Text("Tesla 계정으로 로그인", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "브라우저에서 Tesla 로그인 → 리다이렉트된 URL을 붙여넣기",
                    color = TextSecondary,
                    fontSize = 11.sp,
                )
            } else {
                Text(
                    "로그인 후 리다이렉트된 URL 전체를 붙여넣어주세요",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                ApiTextField(
                    value = state.teslaAuthCode,
                    onValueChange = vm::onTeslaAuthCodeChange,
                    label = "콜백 URL 또는 인증 코드",
                    placeholder = "https://...?code=...",
                )
                Spacer(Modifier.height(12.dp))
                ConnectButton(
                    text = "Tesla Fleet API 연결",
                    isConnecting = state.isConnecting,
                    onClick = { vm.connectTeslaDirect(onConnected) },
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { vm.onTeslaShowCodeInputChange(false); vm.onTeslaAuthCodeChange("") },
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E), contentColor = TextSecondary),
                ) {
                    Text("다시 시도", fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "Tesla Fleet API 단독 사용: 현재 상태 표시 + 차량 명령 가능. 주행/충전 이력은 TeslaMate 연결이 추가로 필요합니다.",
                color = TextSecondary,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun ConnectButton(text: String, isConnecting: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !isConnecting,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = TeslaRed,
            contentColor = Color.White,
            disabledContainerColor = TeslaRed.copy(alpha = 0.5f),
        ),
    ) {
        if (isConnecting) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp,
            )
        } else {
            Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun RegionChip(label: String, value: String, selected: String, onSelect: (String) -> Unit) {
    val isSelected = selected == value
    Button(
        onClick = { onSelect(value) },
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
private fun ApiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        placeholder = { Text(placeholder, color = Color(0xFF48484A)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = TeslaRed,
            unfocusedBorderColor = FieldBorder,
            focusedLabelColor = TeslaRed,
            unfocusedLabelColor = TextSecondary,
            cursorColor = TeslaRed,
            focusedContainerColor = CardBg,
            unfocusedContainerColor = CardBg,
        ),
    )
}
