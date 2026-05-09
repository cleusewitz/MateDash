package com.soooool.matedash.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DarkBg = Color(0xFF0B0B0B)
private val CardBg = Color(0xFF1A1A1A)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFF8E8E93)

/**
 * 주행/충전 이력처럼 TeslaMate가 반드시 필요한 화면에서 표시.
 * Tesla Fleet API 단독 모드 사용자에게 안내.
 */
@Composable
fun TeslaMateRequiredScreen(featureName: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardBg, RoundedCornerShape(20.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Filled.Cloud,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Text(
                text = "TeslaMate 연결이 필요합니다",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "$featureName 정보는 TeslaMate가 자체 데이터베이스에 저장한 과거 기록에서 가져옵니다.\n\nTesla Fleet API는 현재 상태만 제공하므로 이 화면은 표시되지 않습니다.",
                color = TextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "설정 → 연결 해제 → TeslaMate 탭에서 서버 정보 입력",
                color = Color(0xFF48484A),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
