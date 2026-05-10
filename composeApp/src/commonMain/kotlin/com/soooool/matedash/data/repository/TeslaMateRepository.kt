package com.soooool.matedash.data.repository

import com.soooool.matedash.data.api.TeslaMateApiClient
import com.soooool.matedash.data.api.TeslaVehicleData
import com.soooool.matedash.data.api.toCarState
import com.soooool.matedash.data.model.ApiConfig
import com.soooool.matedash.data.model.CarState
import com.soooool.matedash.data.model.MqttConfig
import com.soooool.matedash.data.mqtt.MqttConnectionState
import com.soooool.matedash.data.mqtt.MqttService
import com.soooool.matedash.data.mqtt.applyTeslaMateTopic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.soooool.matedash.data.persistence.updateLiveActivityState
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

enum class ApiConnectionState { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

class TeslaMateRepository(private val apiClient: TeslaMateApiClient) {

    private val _carState = MutableStateFlow(CarState())
    val carState: StateFlow<CarState> = _carState.asStateFlow()

    private val _connectionState = MutableStateFlow(ApiConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ApiConnectionState> = _connectionState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var pollingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    private val _fastPollingRequested = MutableStateFlow(false)

    private var mqttService: MqttService? = null
    private val _mqttState = MutableStateFlow(MqttConnectionState.DISCONNECTED)
    val mqttState: StateFlow<MqttConnectionState> = _mqttState.asStateFlow()
    private val _mqttError = MutableStateFlow<String?>(null)
    val mqttError: StateFlow<String?> = _mqttError.asStateFlow()

    // 내비 클리어 디바운스 — TeslaMate가 가끔 transient "no route" 신호를 보내도 즉시 클리어 안 함.
    // 30초 동안 유효한 destination 신호가 없을 때만 실제 클리어 → 사용자가 명시적으로 취소한 경우 반영.
    private var pendingClearActiveRouteJob: Job? = null
    private val activeRouteClearDelayMs = 30_000L

    fun requestFastPolling(enabled: Boolean) {
        _fastPollingRequested.value = enabled
    }

    /** Tesla Fleet API의 vehicle_data 응답을 CarState 전체에 병합 (TeslaMate 미연결 모드용).
     * Fleet API는 distance/speed를 항상 imperial(mile, mph)로 반환하므로 km/(km/h)로 변환. */
    fun updateFromFleetVehicleData(data: TeslaVehicleData) {
        val cur = _carState.value
        val cs = data.chargeState
        val cl = data.climateState
        val ds = data.driveState
        val vs = data.vehicleState
        val mileToKm = 1.609344
        _carState.value = cur.copy(
            displayName = data.displayName.ifBlank { cur.displayName },
            state = data.state.ifBlank { cur.state },
            odometer = vs?.odometer?.let { it * mileToKm } ?: cur.odometer,
            softwareVersion = vs?.carVersion?.ifBlank { cur.softwareVersion } ?: cur.softwareVersion,
            batteryLevel = cs?.batteryLevel ?: cur.batteryLevel,
            usableBatteryLevel = cs?.usableBatteryLevel ?: cur.usableBatteryLevel,
            estBatteryRangeKm = cs?.estBatteryRange?.let { it * mileToKm } ?: cur.estBatteryRangeKm,
            ratedBatteryRangeKm = cs?.batteryRange?.let { it * mileToKm } ?: cur.ratedBatteryRangeKm,
            chargeLimitSoc = cs?.chargeLimitSoc ?: cur.chargeLimitSoc,
            isPluggedIn = cs?.chargingState?.lowercase() != "disconnected" && cs?.chargingState?.isNotBlank() == true,
            chargingState = cs?.chargingState ?: cur.chargingState,
            chargerPower = cs?.chargerPower ?: cur.chargerPower,
            timeToFullCharge = cs?.timeToFullCharge ?: cur.timeToFullCharge,
            chargePortDoorOpen = cs?.chargePortDoorOpen ?: cur.chargePortDoorOpen,
            chargerVoltage = cs?.chargerVoltage ?: cur.chargerVoltage,
            chargeEnergyAdded = cs?.chargeEnergyAdded ?: cur.chargeEnergyAdded,
            speed = ds?.speed?.let { (it * mileToKm).toInt() } ?: cur.speed,
            shiftState = ds?.shiftState ?: cur.shiftState,
            heading = ds?.heading ?: cur.heading,
            power = ds?.power ?: cur.power,
            isClimateOn = cl?.isClimateOn ?: cur.isClimateOn,
            insideTemp = cl?.insideTemp ?: cur.insideTemp,
            outsideTemp = cl?.outsideTemp ?: cur.outsideTemp,
            isPreconditioning = cl?.isPreconditioning ?: cur.isPreconditioning,
            isLocked = vs?.locked ?: cur.isLocked,
            sentryMode = vs?.sentryMode ?: cur.sentryMode,
            latitude = ds?.latitude ?: cur.latitude,
            longitude = ds?.longitude ?: cur.longitude,
        )
        _connectionState.value = ApiConnectionState.CONNECTED
    }

    /** Fleet API의 drive_state.active_route_* 필드를 CarState에 병합.
     *  TeslaMate MQTT의 폴링이 느릴 때 Fleet API 직접 폴링으로 보강 (5초 보장). */
    fun updateActiveRouteFromFleet(ds: com.soooool.matedash.data.api.TeslaDriveState?) {
        if (ds == null) return
        val dest = ds.activeRouteDestination
        if (!dest.isNullOrBlank()) {
            // 유효한 destination — 즉시 적용 + 클리어 예약 취소
            _carState.value = _carState.value.copy(
                activeRouteDestination = dest,
                activeRouteMilesToArrival = ds.activeRouteMilesToArrival ?: 0.0,
                activeRouteMinutesToArrival = ds.activeRouteMinutesToArrival?.toInt() ?: 0,
                activeRouteEnergyAtArrival = ds.activeRouteEnergyAtArrival ?: 0,
                activeRouteTrafficMinutesDelay = ds.activeRouteTrafficMinutesDelay?.toInt() ?: 0,
            )
            pendingClearActiveRouteJob?.cancel()
            pendingClearActiveRouteJob = null
        } else if (_carState.value.activeRouteDestination.isNotBlank()) {
            // Fleet API에서도 destination이 비어있고 우리는 표시 중인 destination이 있음 → 사용자 취소 가능성
            // 디바운스 예약 (이미 잡혀있으면 갱신 안 함, MQTT 쪽이 먼저 잡았을 수 있음)
            scheduleActiveRouteClearIfNeeded()
        }
    }

    private fun scheduleActiveRouteClearIfNeeded() {
        if (pendingClearActiveRouteJob?.isActive == true) return
        pendingClearActiveRouteJob = scope.launch {
            kotlinx.coroutines.delay(activeRouteClearDelayMs)
            if (_carState.value.activeRouteDestination.isNotBlank()) {
                _carState.value = _carState.value.copy(
                    activeRouteDestination = "",
                    activeRouteMilesToArrival = 0.0,
                    activeRouteMinutesToArrival = 0,
                    activeRouteEnergyAtArrival = 0,
                    activeRouteTrafficMinutesDelay = 0,
                )
                println("[MateDash] active_route 디바운스 클리어 (${activeRouteClearDelayMs / 1000}초간 신호 없음)")
            }
        }
    }

    /** 외부 미디어 소스(Tesla Fleet API 등)에서 가져온 재생 정보를 CarState에 병합.
     *  곡이 바뀌면 iTunes Search로 앨범 아트 URL을 백그라운드에서 채움. */
    fun updateMediaInfo(title: String, artist: String, album: String, source: String, isPlaying: Boolean) {
        val cur = _carState.value
        val isNewSong = cur.mediaTitle != title || cur.mediaArtist != artist
        _carState.value = cur.copy(
            mediaTitle = title,
            mediaArtist = artist,
            mediaAlbum = album,
            mediaPlaylist = source,
            mediaStatus = if (isPlaying) "Playing" else if (title.isBlank()) "" else "Paused",
            // 새 곡이면 일단 아트 URL 비움 (낡은 이미지 표시 방지). iTunes 검색 결과 도착 시 채워짐.
            mediaArtworkUrl = if (isNewSong) "" else cur.mediaArtworkUrl,
        )
        if (isNewSong && title.isNotBlank()) {
            println("[MateDash] iTunes 검색 시작: title='$title' artist='$artist'")
            scope.launch {
                val url = com.soooool.matedash.ServiceLocator.itunesSearchClient.findArtworkUrl(title, artist)
                val now = _carState.value
                if (url.isBlank()) {
                    println("[MateDash] iTunes 검색 결과 없음")
                } else if (now.mediaTitle != title || now.mediaArtist != artist) {
                    println("[MateDash] iTunes 결과 도착했으나 그 사이 곡 바뀜 — 무시")
                } else {
                    println("[MateDash] iTunes artworkUrl 적용: $url")
                    _carState.value = now.copy(mediaArtworkUrl = url)
                }
            }
        }
    }

    fun startMqtt(service: MqttService, host: String, port: Int, carId: Int, username: String, password: String) {
        stopMqtt()
        if (host.isBlank()) {
            _mqttError.value = "호스트가 비어있습니다"
            return
        }
        mqttService = service
        _mqttError.value = null
        _mqttState.value = MqttConnectionState.CONNECTING
        scope.launch {
            service.connectionState.collect { _mqttState.value = it }
        }
        service.connect(
            config = MqttConfig(host = host, port = port, carId = carId, username = username, password = password),
            onConnected = {
                service.subscribe("teslamate/cars/$carId/+") { topic, payload ->
                    val attr = topic.substringAfterLast('/')
                    _carState.value = _carState.value.applyTeslaMateTopic(attr, payload)
                    handleActiveRouteDebounce(attr, payload)
                }
            },
            onError = { msg ->
                _mqttError.value = msg
                _mqttState.value = MqttConnectionState.ERROR
            },
        )
    }

    fun stopMqtt() {
        mqttService?.disconnect()
        mqttService = null
        pendingClearActiveRouteJob?.cancel()
        pendingClearActiveRouteJob = null
        _mqttState.value = MqttConnectionState.DISCONNECTED
    }

    /**
     * active_route 토픽을 받을 때마다 호출. 유효한 destination이면 클리어 예약 취소,
     * 클리어 신호(error/nil)면 30초 뒤 클리어 예약. 30초 안에 유효 신호 오면 자동 취소됨.
     */
    private fun handleActiveRouteDebounce(attr: String, payload: String) {
        if (attr != "active_route" && attr != "active_route_destination") return
        val isValidSignal = when (attr) {
            "active_route_destination" -> payload.isNotBlank() && !payload.equals("nil", ignoreCase = true)
            "active_route" -> isActiveRouteJsonValid(payload)
            else -> false
        }
        if (isValidSignal) {
            pendingClearActiveRouteJob?.cancel()
            pendingClearActiveRouteJob = null
        } else {
            scheduleActiveRouteClearIfNeeded()
        }
    }

    private fun isActiveRouteJsonValid(json: String): Boolean {
        if (json.isBlank()) return false
        return try {
            val obj = kotlinx.serialization.json.Json.parseToJsonElement(json).jsonObject
            val error = obj["error"]?.jsonPrimitive?.contentOrNull
            if (!error.isNullOrBlank()) return false
            val dest = obj["destination"]?.jsonPrimitive?.contentOrNull ?: ""
            dest.isNotBlank()
        } catch (_: Exception) {
            false
        }
    }

    fun startPolling(config: ApiConfig, pollIntervalMs: Long = 30_000L) {
        stopPolling()
        _connectionState.value = ApiConnectionState.CONNECTING

        pollingJob = scope.launch {
            while (isActive) {
                try {
                    val dto = apiClient.getCarStatus(config)
                    val rest = dto.toCarState()
                    // REST API는 active_route/media 정보를 안 주므로 기존(MQTT/Fleet API에서 채워진) 값 보존.
                    // 그대로 _carState.value = rest 하면 매 5/30초마다 미디어/내비가 빈 값으로 클리어 → 깜빡임.
                    val cur = _carState.value
                    val merged = rest.copy(
                        mediaTitle = cur.mediaTitle,
                        mediaArtist = cur.mediaArtist,
                        mediaAlbum = cur.mediaAlbum,
                        mediaPlaylist = cur.mediaPlaylist,
                        mediaStatus = cur.mediaStatus,
                        mediaArtworkUrl = cur.mediaArtworkUrl,
                        activeRouteDestination = cur.activeRouteDestination,
                        activeRouteMilesToArrival = cur.activeRouteMilesToArrival,
                        activeRouteMinutesToArrival = cur.activeRouteMinutesToArrival,
                        activeRouteEnergyAtArrival = cur.activeRouteEnergyAtArrival,
                        activeRouteTrafficMinutesDelay = cur.activeRouteTrafficMinutesDelay,
                    )
                    _carState.value = merged
                    _connectionState.value = ApiConnectionState.CONNECTED
                    _errorMessage.value = null
                    updateLiveActivityState(merged)
                } catch (e: Exception) {
                    _connectionState.value = ApiConnectionState.ERROR
                    _errorMessage.value = e.message ?: "데이터 수신 실패"
                }
                val activeStates = listOf("driving", "charging")
                val interval = when {
                    _fastPollingRequested.value -> 5_000L
                    _carState.value.state.lowercase() in activeStates -> 5_000L
                    else -> pollIntervalMs
                }
                delay(interval)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        _connectionState.value = ApiConnectionState.DISCONNECTED
    }
}
