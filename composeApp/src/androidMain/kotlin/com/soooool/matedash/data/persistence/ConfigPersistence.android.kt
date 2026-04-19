package com.soooool.matedash.data.persistence

import android.content.Context
import android.content.SharedPreferences
import com.soooool.matedash.data.api.TeslaApiConfig
import com.soooool.matedash.data.model.ApiConfig

private var appContext: Context? = null

fun initPersistence(context: Context) {
    appContext = context.applicationContext
}

private fun prefs(): SharedPreferences? =
    appContext?.getSharedPreferences("matedash_prefs", Context.MODE_PRIVATE)

actual fun saveApiConfig(config: ApiConfig) {
    prefs()?.edit()
        ?.putString("api_base_url", config.baseUrl)
        ?.putString("api_token", config.apiToken)
        ?.putInt("api_car_id", config.carId)
        ?.apply()
}

actual fun loadApiConfig(): ApiConfig? {
    val p = prefs() ?: return null
    val baseUrl = p.getString("api_base_url", null) ?: return null
    if (baseUrl.isBlank()) return null
    return ApiConfig(
        baseUrl = baseUrl,
        apiToken = p.getString("api_token", "") ?: "",
        carId = p.getInt("api_car_id", 1),
    )
}

actual fun clearApiConfig() {
    prefs()?.edit()
        ?.remove("api_base_url")
        ?.remove("api_token")
        ?.remove("api_car_id")
        ?.apply()
}

actual fun saveTeslaApiConfig(config: TeslaApiConfig) {
    prefs()?.edit()
        ?.putString("tesla_base_url", config.baseUrl)
        ?.putString("tesla_access_token", config.accessToken)
        ?.putString("tesla_refresh_token", config.refreshToken)
        ?.putString("tesla_client_id", config.clientId)
        ?.putLong("tesla_vehicle_id", config.vehicleId)
        ?.apply()
}

actual fun loadTeslaApiConfig(): TeslaApiConfig? {
    val p = prefs() ?: return null
    val token = p.getString("tesla_access_token", null) ?: return null
    if (token.isBlank()) return null
    return TeslaApiConfig(
        baseUrl = p.getString("tesla_base_url", "https://fleet-api.prd.na.vn.cloud.tesla.com") ?: "https://fleet-api.prd.na.vn.cloud.tesla.com",
        accessToken = token,
        refreshToken = p.getString("tesla_refresh_token", "") ?: "",
        clientId = p.getString("tesla_client_id", "") ?: "",
        vehicleId = p.getLong("tesla_vehicle_id", 0L),
    )
}

actual fun clearTeslaApiConfig() {
    prefs()?.edit()
        ?.remove("tesla_base_url")
        ?.remove("tesla_access_token")
        ?.remove("tesla_refresh_token")
        ?.remove("tesla_client_id")
        ?.remove("tesla_vehicle_id")
        ?.apply()
}
