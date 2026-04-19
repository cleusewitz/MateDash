package com.soooool.matedash.data.persistence

import com.soooool.matedash.data.model.AppSettings
import com.soooool.matedash.data.model.DistanceUnit
import com.soooool.matedash.data.model.TemperatureUnit
import platform.Foundation.NSUserDefaults

private const val SUITE_NAME = "group.com.soooool.matedash"

private fun defaults(): NSUserDefaults? = NSUserDefaults(suiteName = SUITE_NAME)

actual fun saveAppSettings(settings: AppSettings) {
    val d = defaults() ?: return
    d.setBool(settings.liveActivityEnabled, forKey = "settings_la_enabled")
    d.setBool(settings.liveActivityChargingEnabled, forKey = "settings_la_charging")
    d.setBool(settings.liveActivityDrivingEnabled, forKey = "settings_la_driving")
    d.setBool(settings.excludeSupercharger, forKey = "settings_exclude_sc")
    d.setObject(settings.distanceUnit.name, forKey = "settings_distance_unit")
    d.setObject(settings.temperatureUnit.name, forKey = "settings_temperature_unit")
    d.setInteger(settings.pollIntervalSeconds.toLong(), forKey = "settings_poll_interval")
    d.setBool(settings.mapEnabled, forKey = "settings_map_enabled")
    d.synchronize()
}

actual fun loadAppSettings(): AppSettings {
    val d = defaults() ?: return AppSettings()
    // 첫 실행 체크: 키가 없으면 기본값 반환
    if (d.objectForKey("settings_la_enabled") == null) return AppSettings()
    return AppSettings(
        liveActivityEnabled = d.boolForKey("settings_la_enabled"),
        liveActivityChargingEnabled = d.boolForKey("settings_la_charging"),
        liveActivityDrivingEnabled = d.boolForKey("settings_la_driving"),
        excludeSupercharger = d.boolForKey("settings_exclude_sc"),
        distanceUnit = try { DistanceUnit.valueOf(d.stringForKey("settings_distance_unit") ?: "KM") } catch (_: Exception) { DistanceUnit.KM },
        temperatureUnit = try { TemperatureUnit.valueOf(d.stringForKey("settings_temperature_unit") ?: "CELSIUS") } catch (_: Exception) { TemperatureUnit.CELSIUS },
        pollIntervalSeconds = d.integerForKey("settings_poll_interval").toInt().let { if (it == 0) 30 else it },
        mapEnabled = d.boolForKey("settings_map_enabled"),
    )
}
