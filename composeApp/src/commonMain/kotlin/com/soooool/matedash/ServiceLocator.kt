package com.soooool.matedash

import com.soooool.matedash.data.api.GrafanaClient
import com.soooool.matedash.data.api.ItunesSearchClient
import com.soooool.matedash.data.api.TeslaApiConfig
import com.soooool.matedash.data.api.TeslaFleetApiClient
import com.soooool.matedash.data.api.TeslaMateApiClient
import com.soooool.matedash.data.model.ApiConfig
import com.soooool.matedash.data.model.AppSettings
import com.soooool.matedash.data.media.TeslaFullVehiclePoller
import com.soooool.matedash.data.media.TeslaMediaPoller
import com.soooool.matedash.data.mqtt.MqttService
import com.soooool.matedash.data.mqtt.createMqttService
import com.soooool.matedash.data.persistence.clearTeslaApiConfig as clearTeslaApiConfigStorage
import com.soooool.matedash.data.persistence.loadApiConfig
import com.soooool.matedash.data.persistence.loadAppSettings
import com.soooool.matedash.data.persistence.loadTeslaApiConfig
import com.soooool.matedash.data.persistence.saveAppSettings
import com.soooool.matedash.data.persistence.saveTeslaApiConfig
import com.soooool.matedash.data.repository.TeslaMateRepository
import com.soooool.matedash.data.repository.TeslaVehicleRepository
import com.soooool.matedash.data.repository.VehicleDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ServiceLocator {
    val apiClient by lazy { TeslaMateApiClient() }
    val repository by lazy { TeslaMateRepository(apiClient) }

    private val _currentConfigFlow = MutableStateFlow<ApiConfig?>(null)
    val currentConfigFlow: StateFlow<ApiConfig?> = _currentConfigFlow.asStateFlow()
    var currentConfig: ApiConfig?
        get() = _currentConfigFlow.value
        set(value) { _currentConfigFlow.value = value }

    val grafanaClient by lazy { GrafanaClient() }
    val teslaApiClient by lazy { TeslaFleetApiClient() }
    val itunesSearchClient by lazy { ItunesSearchClient() }
    val vehicleDataSource: VehicleDataSource by lazy { TeslaVehicleRepository(teslaApiClient) }
    val mqttService: MqttService by lazy { createMqttService() }
    val mediaPoller: TeslaMediaPoller by lazy { TeslaMediaPoller(teslaApiClient, repository) }
    val fullVehiclePoller: TeslaFullVehiclePoller by lazy { TeslaFullVehiclePoller(teslaApiClient, repository) }

    /** Tesla access token이 만료(401)됐을 때 refresh_token으로 새 토큰 발급 후 teslaApiConfig 갱신.
     *  성공 시 true (호출자가 재시도 가능). */
    suspend fun refreshTeslaToken(): Boolean {
        val cfg = teslaApiConfig ?: return false
        if (cfg.refreshToken.isBlank() || cfg.clientId.isBlank()) return false
        return try {
            val newToken = teslaApiClient.refreshAccessToken(cfg.clientId, cfg.refreshToken)
            teslaApiConfig = cfg.copy(
                accessToken = newToken.accessToken,
                refreshToken = newToken.refreshToken.ifBlank { cfg.refreshToken },
            )
            println("[MateDash] Tesla token 갱신 성공")
            true
        } catch (e: Exception) {
            println("[MateDash] Tesla token 갱신 실패: ${e.message}")
            false
        }
    }

    fun startMediaPolling() {
        val cfg = teslaApiConfig ?: return
        mediaPoller.start(cfg)
    }

    fun stopMediaPolling() {
        mediaPoller.stop()
    }

    /** TeslaMate 미연결 + Tesla Fleet API만 있을 때 호출 — 풀 vehicle_data 폴링 시작 */
    fun startFullVehiclePolling() {
        val cfg = teslaApiConfig ?: return
        fullVehiclePoller.start(cfg)
    }

    fun stopFullVehiclePolling() {
        fullVehiclePoller.stop()
    }

    /** 클러스터 표시 등 빠른 갱신 필요 시점 — 풀 폴러를 5초 주기로 단축 */
    fun setFullVehicleFastMode(enabled: Boolean) {
        fullVehiclePoller.setFastMode(enabled)
    }

    fun applyMqttSettings() {
        val s = appSettings
        val config = currentConfig
        if (s.mqttEnabled && s.mqttHost.isNotBlank() && config != null) {
            repository.startMqtt(
                service = mqttService,
                host = s.mqttHost,
                port = s.mqttPort,
                carId = config.carId,
                username = s.mqttUsername,
                password = s.mqttPassword,
            )
        } else {
            repository.stopMqtt()
        }
    }

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

    val clusterVisible = MutableStateFlow(false)

    /** TeslaMate 재연결 시 증가. 주행/충전 ViewModel이 옵저빙해서 캐시 무효화 후 reload. */
    val historyDataEpoch = MutableStateFlow(0L)
    fun invalidateHistoryData() { historyDataEpoch.value = historyDataEpoch.value + 1 }

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
