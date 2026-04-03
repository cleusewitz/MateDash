package com.soooool.matedash.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soooool.matedash.ServiceLocator

private val DarkBg = Color(0xFF0B0B0B)
private val CardBg = Color(0xFF1A1A1A)
private val TeslaRed = Color(0xFFE31937)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFF8E8E93)

@Composable
fun SettingsScreen(onDisconnect: () -> Unit) {
    val config = ServiceLocator.currentConfig

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(24.dp))

        Text(
            text = "설정",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
        )

        Spacer(Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg, RoundedCornerShape(20.dp))
                .padding(20.dp),
        ) {
            SettingRow(label = "연결 서버", value = config?.baseUrl ?: "-")
            Spacer(Modifier.height(16.dp))
            SettingRow(label = "차량 ID", value = config?.carId?.toString() ?: "-")
            Spacer(Modifier.height(16.dp))
            SettingRow(label = "갱신 주기", value = "30초")
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                ServiceLocator.repository.stopPolling()
                ServiceLocator.currentConfig = null
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
    }
}

@Composable
private fun SettingRow(label: String, value: String) {
    Column {
        Text(label, fontSize = 12.sp, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 15.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
    }
}
