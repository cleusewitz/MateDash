package com.soooool.matedash.data.model

data class AppSettings(
    // Live Activity
    val liveActivityEnabled: Boolean = true,
    val liveActivityChargingEnabled: Boolean = true,
    val liveActivityDrivingEnabled: Boolean = true,
    val excludeSupercharger: Boolean = true,
    // 표시 설정
    val distanceUnit: DistanceUnit = DistanceUnit.KM,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    // 데이터
    val pollIntervalSeconds: Int = 30,
    // 지도
    val mapEnabled: Boolean = true,
)

enum class DistanceUnit(val label: String) {
    KM("km"), MILES("miles")
}

enum class TemperatureUnit(val label: String) {
    CELSIUS("°C"), FAHRENHEIT("°F")
}
