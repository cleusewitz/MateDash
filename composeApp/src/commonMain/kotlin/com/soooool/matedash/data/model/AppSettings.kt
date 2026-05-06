package com.soooool.matedash.data.model

data class AppSettings(
    // Live Activity
    val liveActivityEnabled: Boolean = true,
    val excludeSupercharger: Boolean = true,
    // 표시 설정
    val distanceUnit: DistanceUnit = DistanceUnit.KM,
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    // 데이터
    val pollIntervalSeconds: Int = 30,
    // 지도
    val mapEnabled: Boolean = true,
    // Grafana
    val grafanaUrl: String = "",
    val grafanaApiKey: String = "",
    val grafanaUser: String = "",
    val grafanaPassword: String = "",
    // MQTT (실시간 갱신, TeslaMate 브로커 직결)
    val mqttEnabled: Boolean = false,
    val mqttHost: String = "",
    val mqttPort: Int = 1883,
    val mqttUsername: String = "",
    val mqttPassword: String = "",
)

enum class DistanceUnit(val label: String) {
    KM("km"), MILES("miles")
}

enum class TemperatureUnit(val label: String) {
    CELSIUS("°C"), FAHRENHEIT("°F")
}
