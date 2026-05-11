package com.soooool.matedash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.soooool.matedash.R
import com.soooool.matedash.data.persistence.initPersistence

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash 테마(@style/Theme.MateDash.Splash)에서 일반 테마로 전환 — Compose가
        // 첫 프레임 그릴 때까지 windowBackground 스플래시 드로어블이 보임.
        setTheme(R.style.Theme_MateDash)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        initPersistence(this)
        println("[MateDash] initPersistence done")

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}