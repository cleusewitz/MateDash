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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soooool.matedash.ServiceLocator
import com.soooool.matedash.data.mqtt.MqttConnectionState

@Composable
internal fun MqttSettingsScreen(onBack: () -> Unit) {
    val settings by ServiceLocator.settingsFlow.collectAsState()
    val mqttState by ServiceLocator.repository.mqttState.collectAsState()
    val mqttError by ServiceLocator.repository.mqttError.collectAsState()

    var host by remember { mutableStateOf(settings.mqttHost) }
    var port by remember { mutableStateOf(settings.mqttPort.toString()) }
    var username by remember { mutableStateOf(settings.mqttUsername) }
    var password by remember { mutableStateOf(settings.mqttPassword) }
    var enabled by remember { mutableStateOf(settings.mqttEnabled) }

    SettingsDetailScaffold(title = "MQTT (실시간 갱신)", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg, RoundedCornerShape(20.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingSwitch(
                label = "MQTT 활성화",
                description = "TeslaMate 브로커에서 변경 즉시 푸시 수신 (1초 이하 지연)",
                checked = enabled,
                onCheckedChange = { enabled = it },
            )

            TeslaTextField(
                label = "Broker Host (예: 192.168.0.10)",
                value = host,
                onValueChange = { host = it.trim() },
            )
            TeslaTextField(
                label = "Port (기본 1883)",
                value = port,
                onValueChange = { port = it.filter { c -> c.isDigit() }.take(5) },
            )
            TeslaTextField(
                label = "Username (선택)",
                value = username,
                onValueChange = { username = it },
            )
            TeslaTextField(
                label = "Password (선택)",
                value = password,
                onValueChange = { password = it },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("상태", fontSize = 12.sp, color = TextSecondary)
                Text(
                    text = mqttState.label(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = mqttState.color(),
                )
            }
            mqttError?.let {
                Text(it, fontSize = 11.sp, color = TeslaRed)
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                val portInt = port.toIntOrNull() ?: 1883
                ServiceLocator.appSettings = settings.copy(
                    mqttEnabled = enabled,
                    mqttHost = host,
                    mqttPort = portInt,
                    mqttUsername = username,
                    mqttPassword = password,
                )
                ServiceLocator.applyMqttSettings()
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ChargingBlue,
                contentColor = Color.White,
            ),
        ) {
            Text(
                if (enabled) "저장 및 연결" else "저장 (비활성)",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private fun MqttConnectionState.label(): String = when (this) {
    MqttConnectionState.DISCONNECTED -> "연결 안 됨"
    MqttConnectionState.CONNECTING -> "연결 중..."
    MqttConnectionState.CONNECTED -> "연결됨"
    MqttConnectionState.RECONNECTING -> "재연결 중..."
    MqttConnectionState.ERROR -> "오류"
}

private fun MqttConnectionState.color(): Color = when (this) {
    MqttConnectionState.CONNECTED -> BatteryGreen
    MqttConnectionState.CONNECTING, MqttConnectionState.RECONNECTING -> ChargingBlue
    MqttConnectionState.ERROR -> TeslaRed
    MqttConnectionState.DISCONNECTED -> TextSecondary
}
