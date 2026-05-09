package com.soooool.matedash.data.media

import com.soooool.matedash.data.api.TeslaApiConfig
import com.soooool.matedash.data.api.TeslaFleetApiClient
import com.soooool.matedash.data.repository.TeslaMateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Tesla Fleet API의 vehicle_data를 폴링해서 CarState 전체를 채운다.
 * TeslaMate 미연결 모드용 — TeslaMate가 있을 땐 사용 안 함 (MQTT 1초 갱신이 더 빠름).
 *
 * - 차량 활동 상태에 따라 폴링 주기 가변: driving/charging 시 30초, 그 외 60초
 * - asleep 상태에선 폴링 스킵 (vehicle_data 호출이 차량 깨우는 부작용 방지)
 */
class TeslaFullVehiclePoller(
    private val client: TeslaFleetApiClient,
    private val repository: TeslaMateRepository,
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var pollingJob: Job? = null

    fun start(config: TeslaApiConfig) {
        if (config.accessToken.isBlank() || config.vehicleId == 0L) return
        stop()
        pollingJob = scope.launch {
            while (isActive) {
                tick(config)
                val state = repository.carState.value.state.lowercase()
                val interval = if (state in listOf("driving", "charging")) 30_000L else 60_000L
                delay(interval)
            }
        }
    }

    fun stop() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private suspend fun tick(config: TeslaApiConfig) {
        try {
            val data = client.getVehicleData(config)
            println("[MateDash] TeslaFullVehiclePoller: parsed name='${data.displayName}' state='${data.state}' " +
                "vs=${data.vehicleState != null} cs=${data.chargeState != null} cl=${data.climateState != null} ds=${data.driveState != null}")
            data.vehicleState?.let {
                println("[MateDash]   vs.odometer=${it.odometer} version='${it.carVersion}' locked=${it.locked}")
            }
            data.chargeState?.let {
                println("[MateDash]   cs.battery=${it.batteryLevel}% range=${it.batteryRange}km charging='${it.chargingState}' power=${it.chargerPower}")
            }
            data.driveState?.let {
                println("[MateDash]   ds.lat=${it.latitude} lng=${it.longitude} speed=${it.speed} shift='${it.shiftState}'")
            }
            repository.updateFromFleetVehicleData(data)
            // 미디어도 같은 응답에서 같이 갱신
            val info = data.vehicleState?.mediaInfo
            if (info != null && info.nowPlayingTitle.isNotBlank()) {
                val isPlaying = info.nowPlayingDuration > 0 &&
                    info.nowPlayingElapsed in 1..(info.nowPlayingDuration - 1)
                repository.updateMediaInfo(
                    title = info.nowPlayingTitle,
                    artist = info.nowPlayingArtist,
                    album = info.nowPlayingAlbum,
                    source = info.nowPlayingSource,
                    isPlaying = isPlaying || info.nowPlayingTitle.isNotBlank(),
                )
            }
        } catch (e: Exception) {
            println("[MateDash] TeslaFullVehiclePoller: tick error=${e.message}")
        }
    }
}
