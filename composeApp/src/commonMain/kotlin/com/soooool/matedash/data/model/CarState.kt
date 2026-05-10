package com.soooool.matedash.data.model

data class CarState(
    // 기본
    val displayName: String = "Tesla",
    val state: String = "unknown",
    val odometer: Double = 0.0,
    val softwareVersion: String = "",
    // 배터리
    val batteryLevel: Int = 0,
    val usableBatteryLevel: Int = 0,
    val estBatteryRangeKm: Double = 0.0,
    val ratedBatteryRangeKm: Double = 0.0,
    val chargeLimitSoc: Int = 80,
    // 충전
    val isPluggedIn: Boolean = false,
    val chargingState: String = "",
    val chargerPower: Int = 0,
    val timeToFullCharge: Double = 0.0,
    val chargePortDoorOpen: Boolean = false,
    val chargerVoltage: Int = 0,
    val chargeEnergyAdded: Double = 0.0,
    // 주행
    val speed: Int = 0,
    val shiftState: String = "",
    val heading: Int = 0,
    val elevation: Int = 0,
    val power: Int = 0,
    // 기후
    val isClimateOn: Boolean = false,
    val insideTemp: Double = 0.0,
    val outsideTemp: Double = 0.0,
    val isPreconditioning: Boolean = false,
    // 상태
    val isLocked: Boolean = true,
    val sentryMode: Boolean = false,
    val windowsOpen: Boolean = false,
    val doorsOpen: Boolean = false,
    val trunkOpen: Boolean = false,
    val frunkOpen: Boolean = false,
    val isUserPresent: Boolean = false,
    // 위치
    val geofence: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    // 타이어 압력 (bar)
    val tpmsFl: Double = 0.0,
    val tpmsFr: Double = 0.0,
    val tpmsRl: Double = 0.0,
    val tpmsRr: Double = 0.0,
    // 미디어 (TeslaMate가 차량 vehicle_state에서 가져와 publish)
    val mediaTitle: String = "",
    val mediaArtist: String = "",
    val mediaAlbum: String = "",
    val mediaPlaylist: String = "",
    val mediaStatus: String = "", // Playing / Paused / Stopped
    val mediaArtworkUrl: String = "", // iTunes Search API에서 가져온 앨범 아트 URL
    // 내비게이션 (Tesla 차량 active route)
    val activeRouteDestination: String = "",
    val activeRouteMilesToArrival: Double = 0.0,
    val activeRouteMinutesToArrival: Int = 0,
    val activeRouteEnergyAtArrival: Int = 0, // 도착 시 예상 배터리 %
    val activeRouteTrafficMinutesDelay: Int = 0,
)
