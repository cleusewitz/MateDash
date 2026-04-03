package com.soooool.matedash.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import matedash.composeapp.generated.resources.Res
import matedash.composeapp.generated.resources.pretendard_bold
import matedash.composeapp.generated.resources.pretendard_medium
import matedash.composeapp.generated.resources.pretendard_regular
import matedash.composeapp.generated.resources.pretendard_semibold
import org.jetbrains.compose.resources.Font

@Composable
fun pretendardFontFamily() = FontFamily(
    Font(Res.font.pretendard_regular, FontWeight.Normal),
    Font(Res.font.pretendard_medium, FontWeight.Medium),
    Font(Res.font.pretendard_semibold, FontWeight.SemiBold),
    Font(Res.font.pretendard_bold, FontWeight.Bold),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val fonts = pretendardFontFamily()
    val typography = Typography(
        displayLarge  = TextStyle(fontFamily = fonts, fontWeight = FontWeight.Bold,   fontSize = 57.sp),
        displayMedium = TextStyle(fontFamily = fonts, fontWeight = FontWeight.Bold,   fontSize = 45.sp),
        displaySmall  = TextStyle(fontFamily = fonts, fontWeight = FontWeight.Bold,   fontSize = 36.sp),
        headlineLarge = TextStyle(fontFamily = fonts, fontWeight = FontWeight.Bold,   fontSize = 32.sp),
        headlineMedium= TextStyle(fontFamily = fonts, fontWeight = FontWeight.SemiBold, fontSize = 28.sp),
        headlineSmall = TextStyle(fontFamily = fonts, fontWeight = FontWeight.SemiBold, fontSize = 24.sp),
        titleLarge    = TextStyle(fontFamily = fonts, fontWeight = FontWeight.Bold,   fontSize = 22.sp),
        titleMedium   = TextStyle(fontFamily = fonts, fontWeight = FontWeight.Medium, fontSize = 16.sp),
        titleSmall    = TextStyle(fontFamily = fonts, fontWeight = FontWeight.Medium, fontSize = 14.sp),
        bodyLarge     = TextStyle(fontFamily = fonts, fontWeight = FontWeight.Normal, fontSize = 16.sp),
        bodyMedium    = TextStyle(fontFamily = fonts, fontWeight = FontWeight.Normal, fontSize = 14.sp),
        bodySmall     = TextStyle(fontFamily = fonts, fontWeight = FontWeight.Normal, fontSize = 12.sp),
        labelLarge    = TextStyle(fontFamily = fonts, fontWeight = FontWeight.Medium, fontSize = 14.sp),
        labelMedium   = TextStyle(fontFamily = fonts, fontWeight = FontWeight.Medium, fontSize = 12.sp),
        labelSmall    = TextStyle(fontFamily = fonts, fontWeight = FontWeight.Medium, fontSize = 11.sp),
    )
    MaterialTheme(
        colorScheme = darkColorScheme(),
        typography = typography,
        content = content,
    )
}
