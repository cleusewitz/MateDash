package com.soooool.matedash.data.persistence

import android.content.Context
import com.soooool.matedash.data.model.AppSettings
import com.soooool.matedash.data.model.DistanceUnit
import com.soooool.matedash.data.model.TemperatureUnit

private fun settingsPrefs() =
    try {
        Class.forName("com.soooool.matedash.data.persistence.ConfigPersistence_androidKt")
            .let { null } // use appContext from ConfigPersistence
    } catch (_: Exception) { null }

private fun prefs() =
    try {
        val field = Class.forName("com.soooool.matedash.data.persistence.ConfigPersistence_androidKt")
        null
    } catch (_: Exception) { null }

private fun getPrefs(): android.content.SharedPreferences? {
    return try {
        val ctx = Class.forName("com.soooool.matedash.data.persistence.ConfigPersistence_androidKt")
            .getDeclaredField("appContext")
            .apply { isAccessible = true }
            .get(null) as? Context
        ctx?.getSharedPreferences("matedash_settings", Context.MODE_PRIVATE)
    } catch (_: Exception) { null }
}

actual fun saveAppSettings(settings: AppSettings) {
    getPrefs()?.edit()
        ?.putBoolean("live_activity_enabled", settings.liveActivityEnabled)
        ?.putBoolean("live_activity_charging", settings.liveActivityChargingEnabled)
        ?.putBoolean("live_activity_driving", settings.liveActivityDrivingEnabled)
        ?.putBoolean("exclude_supercharger", settings.excludeSupercharger)
        ?.putString("distance_unit", settings.distanceUnit.name)
        ?.putString("temperature_unit", settings.temperatureUnit.name)
        ?.putInt("poll_interval", settings.pollIntervalSeconds)
        ?.putBoolean("map_enabled", settings.mapEnabled)
        ?.apply()
}

actual fun loadAppSettings(): AppSettings {
    val p = getPrefs() ?: return AppSettings()
    return AppSettings(
        liveActivityEnabled = p.getBoolean("live_activity_enabled", true),
        liveActivityChargingEnabled = p.getBoolean("live_activity_charging", true),
        liveActivityDrivingEnabled = p.getBoolean("live_activity_driving", true),
        excludeSupercharger = p.getBoolean("exclude_supercharger", true),
        distanceUnit = try { DistanceUnit.valueOf(p.getString("distance_unit", "KM")!!) } catch (_: Exception) { DistanceUnit.KM },
        temperatureUnit = try { TemperatureUnit.valueOf(p.getString("temperature_unit", "CELSIUS")!!) } catch (_: Exception) { TemperatureUnit.CELSIUS },
        pollIntervalSeconds = p.getInt("poll_interval", 30),
        mapEnabled = p.getBoolean("map_enabled", true),
    )
}
