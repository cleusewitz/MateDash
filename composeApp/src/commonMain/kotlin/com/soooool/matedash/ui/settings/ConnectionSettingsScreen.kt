package com.soooool.matedash.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soooool.matedash.ServiceLocator
import com.soooool.matedash.data.model.ApiConfig
import com.soooool.matedash.data.mqtt.MqttConnectionState
import com.soooool.matedash.data.persistence.clearApiConfig
import com.soooool.matedash.data.persistence.saveApiConfig
import kotlinx.coroutines.launch

@Composable
internal fun ConnectionSettingsScreen(onBack: () -> Unit, onDisconnect: () -> Unit) {
    val cur = ServiceLocator.currentConfig
    val curSettings by ServiceLocator.settingsFlow.collectAsState()
    val mqttState by ServiceLocator.repository.mqttState.collectAsState()
    val scope = rememberCoroutineScope()

    val initialHost = cur?.baseUrl?.removePrefix("http://")?.removePrefix("https://")
        ?.substringBefore(':') ?: ""
    val initialPort = cur?.baseUrl?.substringAfterLast(':')?.takeIf { it.toIntOrNull() != null } ?: "9999"

    var host by remember { mutableStateOf(initialHost) }
    var port by remember { mutableStateOf(initialPort) }
    var carId by remember { mutableStateOf(cur?.carId?.toString() ?: "1") }
    var apiToken by remember { mutableStateOf(cur?.apiToken ?: "") }
    var mqttEnabled by remember { mutableStateOf(curSettings.mqttEnabled) }
    var mqttPort by remember { mutableStateOf(curSettings.mqttPort.toString()) }
    var mqttUsername by remember { mutableStateOf(curSettings.mqttUsername) }
    var mqttPassword by remember { mutableStateOf(curSettings.mqttPassword) }
    var isConnecting by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    SettingsDetailScaffold(title = "TeslaMate 연결", onBack = onBack) {
        // 현재 상태 카드
        if (cur != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBg, RoundedCornerShape(16.dp))
                    .padding(16.dp),
            ) {
                Text("현재 연결됨 ✅", color = BatteryGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(cur.baseUrl, color = TextSecondary, fontSize = 11.sp)
                if (mqttState == MqttConnectionState.CONNECTED) {
                    Text("MQTT broker 연결됨", color = BatteryGreen, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // 편집 폼
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg, RoundedCornerShape(20.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("TeslaMate 서버", color = TextSecondary, fontSize = 12.sp)
            EditField(value = host, onValueChange = { host = it.trim() }, label = "호스트", placeholder = "예: 192.168.0.10")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                EditField(
                    value = port,
                    onValueChange = { port = it.filter { c -> c.isDigit() }.take(5) },
                    label = "REST 포트",
                    placeholder = "9999",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
                EditField(
                    value = carId,
                    onValueChange = { carId = it.filter { c -> c.isDigit() }.take(3) },
                    label = "차량 ID",
                    placeholder = "1",
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
            }
            EditField(
                value = apiToken,
                onValueChange = { apiToken = it },
                label = "API 토큰 (선택)",
                placeholder = "TeslaMate 인증 켰을 때만",
                isPassword = true,
            )

            HorizontalDivider(color = Color(0xFF2C2C2E))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("MQTT 실시간 갱신", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("broker host = TeslaMate host. 1초 이하 푸시 갱신.", color = TextSecondary, fontSize = 11.sp)
                }
                Switch(
                    checked = mqttEnabled,
                    onCheckedChange = { mqttEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = BatteryGreen,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = Color(0xFF2C2C2E),
                    ),
                )
            }

            if (mqttEnabled) {
                EditField(
                    value = mqttPort,
                    onValueChange = { mqttPort = it.filter { c -> c.isDigit() }.take(5) },
                    label = "MQTT 포트",
                    placeholder = "1883",
                    keyboardType = KeyboardType.Number,
                )
                EditField(
                    value = mqttUsername,
                    onValueChange = { mqttUsername = it },
                    label = "MQTT Username (선택)",
                    placeholder = "broker 인증 시에만",
                )
                EditField(
                    value = mqttPassword,
                    onValueChange = { mqttPassword = it },
                    label = "MQTT Password (선택)",
                    placeholder = "broker 인증 시에만",
                    isPassword = true,
                )
            }
        }

        status?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                it,
                color = if (it.contains("성공") || it.contains("연결됨")) BatteryGreen else if (it.contains("실패")) TeslaRed else TextSecondary,
                fontSize = 12.sp,
            )
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                if (host.isBlank()) {
                    status = "호스트를 입력해주세요"
                    return@Button
                }
                isConnecting = true
                status = "연결 중..."
                val newConfig = ApiConfig(
                    baseUrl = "http://${host.trim()}:${port.trim()}",
                    apiToken = apiToken.trim(),
                    carId = carId.toIntOrNull() ?: 1,
                )
                scope.launch {
                    try {
                        ServiceLocator.repository.stopPolling()
                        ServiceLocator.repository.stopMqtt()
                        ServiceLocator.apiClient.getCarStatus(newConfig)
                        ServiceLocator.repository.startPolling(
                            newConfig,
                            ServiceLocator.appSettings.pollIntervalSeconds * 1000L,
                        )
                        ServiceLocator.currentConfig = newConfig
                        saveApiConfig(newConfig)
                        ServiceLocator.appSettings = ServiceLocator.appSettings.copy(
                            mqttEnabled = mqttEnabled,
                            mqttHost = host.trim(),
                            mqttPort = mqttPort.toIntOrNull() ?: 1883,
                            mqttUsername = mqttUsername,
                            mqttPassword = mqttPassword,
                        )
                        ServiceLocator.applyMqttSettings()
                        ServiceLocator.stopFullVehiclePolling()
                        // 주행/충전 ViewModel 캐시 무효화 → 자동 reload
                        ServiceLocator.invalidateHistoryData()
                        status = "저장 및 연결 성공"
                    } catch (e: Exception) {
                        status = "실패: ${e.message}"
                    } finally {
                        isConnecting = false
                    }
                }
            },
            enabled = !isConnecting,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ChargingBlue, contentColor = Color.White),
        ) {
            if (isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    if (cur != null) "저장 및 재연결" else "연결",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        if (cur != null) {
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    ServiceLocator.repository.stopPolling()
                    ServiceLocator.repository.stopMqtt()
                    ServiceLocator.currentConfig = null
                    clearApiConfig()
                    onDisconnect()
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TeslaRed,
                    contentColor = Color.White,
                ),
            ) {
                Text("연결 해제", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun EditField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        placeholder = { Text(placeholder, color = Color(0xFF48484A), fontSize = 13.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = ChargingBlue,
            unfocusedBorderColor = Color(0xFF3A3A3C),
            focusedLabelColor = ChargingBlue,
            unfocusedLabelColor = TextSecondary,
            cursorColor = ChargingBlue,
        ),
    )
}
