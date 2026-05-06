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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soooool.matedash.ServiceLocator
import com.soooool.matedash.data.persistence.LiveActivityDebug
import com.soooool.matedash.data.persistence.readLiveActivityDebug
import com.soooool.matedash.data.persistence.startTestLiveActivity
import com.soooool.matedash.data.persistence.startTestDrivingLiveActivity
import com.soooool.matedash.data.persistence.stopTestLiveActivity

@Composable
internal fun LiveActivitySettingsScreen(onBack: () -> Unit) {
    val settings by ServiceLocator.settingsFlow.collectAsState()

    SettingsDetailScaffold(title = "Live Activity", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg, RoundedCornerShape(20.dp))
                .padding(20.dp),
        ) {
            SettingSwitch(
                label = "Live Activity",
                description = "충전/주행 시 잠금화면과 Dynamic Island에 표시",
                checked = settings.liveActivityEnabled,
                onCheckedChange = { ServiceLocator.appSettings = settings.copy(liveActivityEnabled = it) },
            )

            Spacer(Modifier.height(16.dp))

            SettingSwitch(
                label = "슈퍼차저 제외",
                description = "슈퍼차저 충전 시 Live Activity 비활성 (Tesla 앱 사용)",
                checked = settings.excludeSupercharger,
                enabled = settings.liveActivityEnabled,
                onCheckedChange = { ServiceLocator.appSettings = settings.copy(excludeSupercharger = it) },
            )
        }

        Spacer(Modifier.height(20.dp))
        SectionTitle("테스트")
        Spacer(Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg, RoundedCornerShape(20.dp))
                .padding(20.dp),
        ) {
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
        }

        Spacer(Modifier.height(20.dp))
        SectionTitle("디버그")
        Spacer(Modifier.height(8.dp))

        LiveActivityDebugPanel()
    }
}

@Composable
private fun LiveActivityDebugPanel() {
    var debug by remember { mutableStateOf<LiveActivityDebug>(readLiveActivityDebug()) }

    Column(
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
