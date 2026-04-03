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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

private val DarkBg = Color(0xFF0D0D0D)
private val CardBg = Color(0xFF1C1C1E)
private val TeslaRed = Color(0xFFE31937)
private val TextSecondary = Color(0xFF8E8E93)
private val FieldBorder = Color(0xFF3A3A3C)

@Composable
fun ConnectionScreen(onConnected: () -> Unit) {
    val vm = viewModel { ConnectionViewModel() }
    val state by vm.uiState.collectAsState()

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
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))

        Text(
            text = "MateDash",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Text(
            text = "TeslaMate API Dashboard",
            fontSize = 14.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 4.dp),
        )

        Spacer(Modifier.height(48.dp))

        Text(
            text = "서버 설정",
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )

        ApiTextField(
            value = state.host,
            onValueChange = vm::onHostChange,
            label = "호스트",
            placeholder = "soooool.synology.me",
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
                label = "차량 ID",
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
            placeholder = "설정한 경우에만 입력",
            isPassword = true,
        )

        Spacer(Modifier.height(8.dp))
        Text(
            text = "API_TOKEN 미설정 시 비워두세요.",
            color = TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(32.dp))

        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage!!,
                color = TeslaRed,
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            )
        }

        Button(
            onClick = { vm.connect(onConnected) },
            enabled = !state.isConnecting,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TeslaRed,
                contentColor = Color.White,
                disabledContainerColor = TeslaRed.copy(alpha = 0.5f),
            ),
        ) {
            if (state.isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )
            } else {
                Text("연결", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider(color = FieldBorder)
        Spacer(Modifier.height(16.dp))

        Text(
            text = "연결 URL 예시:\nhttp://soooool.synology.me:9999/api/v1/cars/1/status",
            color = Color(0xFF48484A),
            fontSize = 12.sp,
            lineHeight = 18.sp,
        )
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
