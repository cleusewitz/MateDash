package com.soooool.matedash.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import com.soooool.matedash.data.persistence.clearApiConfig

@Composable
internal fun ConnectionSettingsScreen(onBack: () -> Unit, onDisconnect: () -> Unit) {
    val config = ServiceLocator.currentConfig

    SettingsDetailScaffold(title = "TeslaMate 연결", onBack = onBack) {
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

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                ServiceLocator.repository.stopPolling()
                ServiceLocator.repository.stopMqtt()
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
    }
}
