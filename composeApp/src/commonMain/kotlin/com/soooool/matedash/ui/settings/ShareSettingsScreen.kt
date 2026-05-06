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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soooool.matedash.data.share.clearSharedText
import com.soooool.matedash.data.share.parseSharedPlace
import com.soooool.matedash.data.share.readShareExtensionLog
import com.soooool.matedash.data.share.readSharedText
import com.soooool.matedash.data.share.writeTestSharedText

private const val NAVER_SAMPLE_TEXT = "[네이버지도]\n하나로마트 군자농협대부점\n경기 안산시 단원구 대부중앙로 142\nhttps://naver.me/I5caIqaF"

@Composable
internal fun ShareSettingsScreen(onBack: () -> Unit) {
    var sharedText by remember { mutableStateOf(readSharedText()) }
    var logs by remember { mutableStateOf(readShareExtensionLog()) }

    fun refresh() {
        sharedText = readSharedText()
        logs = readShareExtensionLog()
    }

    SettingsDetailScaffold(title = "공유 확장", onBack = onBack) {
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

            val raw = sharedText
            if (raw.isNullOrBlank()) {
                Spacer(Modifier.height(14.dp))
                Text("아직 받은 공유 없음", fontSize = 13.sp, color = TextSecondary)
            } else {
                val place = parseSharedPlace(raw)

                Spacer(Modifier.height(14.dp))
                Text("원문", fontSize = 11.sp, color = TextSecondary)
                Spacer(Modifier.height(4.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF111418), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                ) {
                    Text(raw, fontSize = 12.sp, color = TextPrimary)
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
        }

        Spacer(Modifier.height(20.dp))
        SectionTitle("Share Extension 로그")
        Spacer(Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg, RoundedCornerShape(20.dp))
                .padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("최신 20줄", fontSize = 11.sp, color = TextSecondary)
                Button(
                    onClick = { refresh() },
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
            Spacer(Modifier.height(8.dp))
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
}
