package com.soooool.matedash.data.persistence

import com.soooool.matedash.data.api.TeslaApiConfig
import com.soooool.matedash.data.model.ApiConfig
import platform.Foundation.NSUserDefaults

private const val SUITE_NAME = "group.com.soooool.matedash"
private const val KEY_BASE_URL = "api_base_url"
private const val KEY_API_TOKEN = "api_token"
private const val KEY_CAR_ID = "api_car_id"

private fun sharedDefaults(): NSUserDefaults? =
    NSUserDefaults(suiteName = SUITE_NAME)

actual fun saveApiConfig(config: ApiConfig) {
    val defaults = sharedDefaults() ?: return
    defaults.setObject(config.baseUrl, forKey = KEY_BASE_URL)
    defaults.setObject(config.apiToken, forKey = KEY_API_TOKEN)
    defaults.setInteger(config.carId.toLong(), forKey = KEY_CAR_ID)
    defaults.synchronize()
}

actual fun loadApiConfig(): ApiConfig? {
    val defaults = sharedDefaults() ?: return null
    val baseUrl = defaults.stringForKey(KEY_BASE_URL) ?: return null
    if (baseUrl.isBlank()) return null
    val apiToken = defaults.stringForKey(KEY_API_TOKEN) ?: ""
    val carId = defaults.integerForKey(KEY_CAR_ID).toInt().let { if (it == 0) 1 else it }
    return ApiConfig(baseUrl = baseUrl, apiToken = apiToken, carId = carId)
}

actual fun clearApiConfig() {
    val defaults = sharedDefaults() ?: return
    defaults.removeObjectForKey(KEY_BASE_URL)
    defaults.removeObjectForKey(KEY_API_TOKEN)
    defaults.removeObjectForKey(KEY_CAR_ID)
    defaults.synchronize()
}

actual fun saveTeslaApiConfig(config: TeslaApiConfig) {
    val defaults = sharedDefaults() ?: return
    defaults.setObject(config.baseUrl, forKey = "tesla_base_url")
    defaults.setObject(config.accessToken, forKey = "tesla_access_token")
    defaults.setObject(config.refreshToken, forKey = "tesla_refresh_token")
    defaults.setObject(config.clientId, forKey = "tesla_client_id")
    defaults.setInteger(config.vehicleId, forKey = "tesla_vehicle_id")
    defaults.synchronize()
}

actual fun loadTeslaApiConfig(): TeslaApiConfig? {
    val defaults = sharedDefaults() ?: return null
    val token = defaults.stringForKey("tesla_access_token") ?: return null
    if (token.isBlank()) return null
    return TeslaApiConfig(
        baseUrl = defaults.stringForKey("tesla_base_url") ?: "https://fleet-api.prd.na.vn.cloud.tesla.com",
        accessToken = token,
        refreshToken = defaults.stringForKey("tesla_refresh_token") ?: "",
        clientId = defaults.stringForKey("tesla_client_id") ?: "",
        vehicleId = defaults.integerForKey("tesla_vehicle_id"),
    )
}

actual fun clearTeslaApiConfig() {
    val defaults = sharedDefaults() ?: return
    defaults.removeObjectForKey("tesla_base_url")
    defaults.removeObjectForKey("tesla_access_token")
    defaults.removeObjectForKey("tesla_refresh_token")
    defaults.removeObjectForKey("tesla_client_id")
    defaults.removeObjectForKey("tesla_vehicle_id")
    defaults.synchronize()
}
