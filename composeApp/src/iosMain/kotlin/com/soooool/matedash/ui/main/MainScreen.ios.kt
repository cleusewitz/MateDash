package com.soooool.matedash.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.interop.UIKitViewController
import androidx.compose.ui.window.ComposeUIViewController
import com.soooool.matedash.ui.charging.ChargingScreen
import com.soooool.matedash.ui.dashboard.DashboardScreen
import com.soooool.matedash.ui.driving.DrivingScreen
import com.soooool.matedash.ui.settings.SettingsScreen
import com.soooool.matedash.ui.tesla.TeslaScreen
import com.soooool.matedash.ui.theme.AppTheme
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIColor
import platform.UIKit.UIImage
import platform.UIKit.UITabBarAppearance
import platform.UIKit.UITabBarController
import platform.UIKit.UITabBarItem
import platform.UIKit.UIViewController
import platform.UIKit.addChildViewController
import platform.UIKit.didMoveToParentViewController
import platform.UIKit.tabBarItem

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun MainScreen(onDisconnect: () -> Unit) {
    UIKitViewController(
        factory = { createTabBarController(onDisconnect) },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun SafeAreaContent(content: @Composable () -> Unit) {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    AppTheme {
        Box(
            Modifier.fillMaxSize()
        ) {
            content()

            // 스테이터스 바 그라데이션 오버레이
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(statusBarHeight + 20.dp)
                    .align(Alignment.TopStart)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0B0B0B),
                                Color(0xCC0B0B0B),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )

            // 하단 탭바 영역 그라데이션 오버레이
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .align(Alignment.BottomStart)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xCC0B0B0B),
                                Color(0xFF0B0B0B),
                            ),
                        ),
                    ),
            )
        }
    }
}

/**
 * 탭 선택 시에만 ComposeUIViewController를 생성하는 컨테이너.
 * 처음에는 빈 UIViewController, 탭 선택 시 Compose VC를 child로 추가.
 */
@OptIn(ExperimentalForeignApi::class)
private class LazyComposeContainerVC(
    private val composeFactory: () -> UIViewController,
) : UIViewController(nibName = null, bundle = null) {
    private var composed = false

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)
        if (!composed) {
            composed = true
            val composeVC = composeFactory()
            addChildViewController(composeVC)
            composeVC.view.setFrame(view.bounds)
            composeVC.view.setAutoresizingMask(
                platform.UIKit.UIViewAutoresizingFlexibleWidth or platform.UIKit.UIViewAutoresizingFlexibleHeight
            )
            view.addSubview(composeVC.view)
            composeVC.didMoveToParentViewController(this)
        }
    }

    override fun viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor(red = 0.043, green = 0.043, blue = 0.043, alpha = 1.0)
    }
}

private fun createTabBarController(onDisconnect: () -> Unit): UITabBarController {
    val tabBarController = UITabBarController()

    // 대시보드만 즉시 생성 (첫 화면), 나머지는 지연 생성
    val dashboardContainer = LazyComposeContainerVC {
        ComposeUIViewController { SafeAreaContent { DashboardScreen() } }
    }
    dashboardContainer.tabBarItem = UITabBarItem(
        title = "대시보드",
        image = UIImage.systemImageNamed("gauge.with.dots.needle.33percent"),
        tag = 0,
    )

    val drivingContainer = LazyComposeContainerVC {
        ComposeUIViewController { SafeAreaContent { DrivingScreen() } }
    }
    drivingContainer.tabBarItem = UITabBarItem(
        title = "주행",
        image = UIImage.systemImageNamed("car.fill"),
        tag = 1,
    )

    val chargingContainer = LazyComposeContainerVC {
        ComposeUIViewController { SafeAreaContent { ChargingScreen() } }
    }
    chargingContainer.tabBarItem = UITabBarItem(
        title = "충전",
        image = UIImage.systemImageNamed("bolt.fill"),
        tag = 2,
    )

    val teslaContainer = LazyComposeContainerVC {
        ComposeUIViewController { SafeAreaContent { TeslaScreen() } }
    }
    teslaContainer.tabBarItem = UITabBarItem(
        title = "Tesla",
        image = UIImage.systemImageNamed("bolt.car.fill"),
        tag = 3,
    )

    val settingsContainer = LazyComposeContainerVC {
        ComposeUIViewController { SafeAreaContent { SettingsScreen(onDisconnect = onDisconnect) } }
    }
    settingsContainer.tabBarItem = UITabBarItem(
        title = "설정",
        image = UIImage.systemImageNamed("gearshape.fill"),
        tag = 4,
    )

    tabBarController.viewControllers = listOf(
        dashboardContainer, drivingContainer, chargingContainer, teslaContainer, settingsContainer,
    ).map { it as UIViewController }

    // Dark translucent tab bar with blur (system default blur)
    val appearance = UITabBarAppearance()
    appearance.configureWithDefaultBackground()
    appearance.backgroundColor = UIColor(
        red = 0.043, green = 0.043, blue = 0.043, alpha = 0.2,
    )
    tabBarController.tabBar.standardAppearance = appearance
    tabBarController.tabBar.scrollEdgeAppearance = appearance
    tabBarController.tabBar.tintColor = UIColor(
        red = 0.89, green = 0.098, blue = 0.216, alpha = 1.0,
    )
    tabBarController.tabBar.unselectedItemTintColor = UIColor(
        red = 0.557, green = 0.557, blue = 0.576, alpha = 1.0,
    )

    return tabBarController
}
