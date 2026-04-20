package com.soooool.matedash.data.persistence

import com.soooool.matedash.data.model.CarState

actual fun updateLiveActivityState(carState: CarState) {
    // Android: no-op (Live Activity는 iOS 전용)
}

actual fun startTestLiveActivity() {
    // Android: no-op
}

actual fun startTestDrivingLiveActivity() {
    // Android: no-op (Live Activity는 iOS 전용)
}

actual fun stopTestLiveActivity() {
    // Android: no-op
}

actual fun readLiveActivityDebug(): LiveActivityDebug = LiveActivityDebug()
