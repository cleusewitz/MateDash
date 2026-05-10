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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soooool.matedash.ServiceLocator
import kotlin.math.roundToInt

@Composable
internal fun ClusterSettingsScreen(onBack: () -> Unit) {
    val settings by ServiceLocator.settingsFlow.collectAsState()

    SettingsDetailScaffold(title = "클러스터 설정", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg, RoundedCornerShape(20.dp))
                .padding(20.dp),
        ) {
            Text("글씨 크기", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "클러스터 화면의 모든 텍스트가 비례적으로 확대/축소됩니다.",
                color = TextSecondary,
                fontSize = 11.sp,
            )
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("작게", color = TextSecondary, fontSize = 11.sp)
                Text(
                    "${(settings.clusterFontScale * 100).roundToInt()}%",
                    color = ChargingBlue,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text("크게", color = TextSecondary, fontSize = 11.sp)
            }
            Spacer(Modifier.height(8.dp))
            Slider(
                value = settings.clusterFontScale,
                onValueChange = { v ->
                    // 0.05 단위 스냅
                    val snapped = ((v * 20).roundToInt() / 20.0f).coerceIn(0.8f, 1.5f)
                    if (snapped != settings.clusterFontScale) {
                        ServiceLocator.appSettings = settings.copy(clusterFontScale = snapped)
                    }
                },
                valueRange = 0.8f..1.5f,
                colors = SliderDefaults.colors(
                    thumbColor = ChargingBlue,
                    activeTrackColor = ChargingBlue,
                    inactiveTrackColor = Color(0xFF2C2C2E),
                ),
            )

            Spacer(Modifier.height(8.dp))
            // 미리보기 — 현재 scale 적용된 샘플 텍스트
            val baseSample = 20
            Text(
                "샘플: 60 km/h",
                color = Color.White,
                fontSize = (baseSample * settings.clusterFontScale).sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
