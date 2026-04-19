package com.soooool.matedash

import com.soooool.matedash.data.api.TeslaApiConfig
import com.soooool.matedash.data.api.TeslaFleetApiClient
import com.soooool.matedash.data.api.TeslaMateApiClient
import com.soooool.matedash.data.model.ApiConfig
import com.soooool.matedash.data.model.AppSettings
import com.soooool.matedash.data.persistence.clearTeslaApiConfig as clearTeslaApiConfigStorage
import com.soooool.matedash.data.persistence.loadApiConfig
import com.soooool.matedash.data.persistence.loadAppSettings
import com.soooool.matedash.data.persistence.loadTeslaApiConfig
import com.soooool.matedash.data.persistence.saveAppSettings
import com.soooool.matedash.data.persistence.saveTeslaApiConfig
import com.soooool.matedash.data.repository.TeslaMateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ServiceLocator {
    val apiClient by lazy { TeslaMateApiClient() }
    val repository by lazy { TeslaMateRepository(apiClient) }
    var currentConfig: ApiConfig? = null

    val teslaApiClient by lazy { TeslaFleetApiClient() }
    private var _teslaApiConfig: TeslaApiConfig? = null
    private val _teslaConfigFlow = MutableStateFlow<TeslaApiConfig?>(null)
    val teslaConfigFlow: StateFlow<TeslaApiConfig?> = _teslaConfigFlow.asStateFlow()

    var teslaApiConfig: TeslaApiConfig?
        get() = _teslaApiConfig
        set(value) {
            println("[MateDash] teslaApiConfig set: hasToken=${value?.accessToken?.isNotBlank()}, vehicleId=${value?.vehicleId}")
            _teslaApiConfig = value
            _teslaConfigFlow.value = value
            if (value != null) saveTeslaApiConfig(value)
            else clearTeslaApiConfigStorage()
        }

    private val _settingsFlow = MutableStateFlow(AppSettings())
    val settingsFlow: StateFlow<AppSettings> = _settingsFlow.asStateFlow()

    var appSettings: AppSettings
        get() = _settingsFlow.value
        set(value) {
            _settingsFlow.value = value
            saveAppSettings(value)
        }

    fun loadSavedConfigs() {
        _settingsFlow.value = loadAppSettings()
        // TeslaMate config
        val savedApiConfig = loadApiConfig()
        if (savedApiConfig != null) {
            currentConfig = savedApiConfig
            println("[MateDash] loadSavedConfigs: TeslaMate loaded, baseUrl=${savedApiConfig.baseUrl}")
        }
        // Tesla Fleet API config
        _teslaApiConfig = loadTeslaApiConfig()
        _teslaConfigFlow.value = _teslaApiConfig
        println("[MateDash] loadSavedConfigs: Tesla loaded=${_teslaApiConfig != null}, vehicleId=${_teslaApiConfig?.vehicleId}")
    }
}
