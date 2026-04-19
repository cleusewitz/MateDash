package com.soooool.matedash.data.persistence

import com.soooool.matedash.data.model.CarState

expect fun updateLiveActivityState(carState: CarState)

/** 테스트용 충전 Live Activity 시작 */
expect fun startTestLiveActivity()

/** 테스트용 주행 Live Activity 시작 */
expect fun startTestDrivingLiveActivity()

/** 테스트용 Live Activity 종료 */
expect fun stopTestLiveActivity()
