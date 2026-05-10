package com.soooool.matedash.data.media

import com.soooool.matedash.ServiceLocator
import com.soooool.matedash.data.api.TeslaApiConfig
import com.soooool.matedash.data.api.TeslaFleetApiClient
import com.soooool.matedash.data.api.TeslaVehicleData
import com.soooool.matedash.data.repository.TeslaMateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Tesla Fleet API의 vehicle_data.vehicle_state.media_info를 폴링해서
 * TeslaMateRepository의 CarState에 미디어 필드를 병합한다.
 *
 * TeslaMate가 media MQTT 토픽을 publish하지 않으므로 Fleet API로 직접 가져옴.
 *
 * 클러스터 화면이 표시되는 동안만 동작 (배터리/요금 절약).
 */
class TeslaMediaPoller(
    private val client: TeslaFleetApiClient,
    private val repository: TeslaMateRepository,
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var pollingJob: Job? = null
    private val intervalMs = 5_000L // 5초 — 음악/내비 빠른 갱신
    // 단발 빈 응답에 깜빡이지 않도록 N회 연속 빈 응답일 때만 클리어
    private var consecutiveBlanks: Int = 0
    private val blankClearThreshold = 2

    fun start(config: TeslaApiConfig) {
        if (config.accessToken.isBlank() || config.vehicleId == 0L) return
        stop()
        consecutiveBlanks = 0
        pollingJob = scope.launch {
            while (isActive) {
                tick(config)
                delay(intervalMs)
            }
        }
    }

    fun stop() {
        pollingJob?.cancel()
        pollingJob = null
        consecutiveBlanks = 0
    }

    private suspend fun tick(config: TeslaApiConfig) {
        // 차가 online일 때만 폴링 (asleep 상태에서 vehicle_data 호출 시 깨우는 부작용 방지)
        val carState = repository.carState.value
        if (carState.state.lowercase() != "online") return
        val data = try {
            fetchWithRefresh(config)
        } catch (e: Exception) {
            println("[MateDash] TeslaMediaPoller: tick error=${e.message}")
            return
        }
        val info = data.vehicleState?.mediaInfo
        if (info != null && info.nowPlayingTitle.isNotBlank()) {
            consecutiveBlanks = 0
            // duration/elapsed로만 판정 — title이 있어도 일시정지 상태면 elapsed가 안 움직임
            val isPlaying = info.nowPlayingDuration > 0 &&
                info.nowPlayingElapsed in 1..(info.nowPlayingDuration - 1)
            repository.updateMediaInfo(
                title = info.nowPlayingTitle,
                artist = info.nowPlayingArtist,
                album = info.nowPlayingAlbum,
                source = info.nowPlayingSource,
                isPlaying = isPlaying,
            )
        } else {
            // 빈 응답이 N회 연속이면 클리어 — 단발 깜빡임 방지하면서 정지/일시정지 반영
            consecutiveBlanks++
            val cur = repository.carState.value
            if (consecutiveBlanks >= blankClearThreshold && cur.mediaTitle.isNotBlank()) {
                repository.updateMediaInfo(title = "", artist = "", album = "", source = "", isPlaying = false)
            }
        }

        // 같은 응답에서 active_route 정보도 갱신 — TeslaMate MQTT의 느린 폴링 보강 (5초 보장)
        repository.updateActiveRouteFromFleet(data.driveState)
    }

    /** 401 받으면 refresh_token으로 새 토큰 발급 후 한 번만 재시도. */
    private suspend fun fetchWithRefresh(config: TeslaApiConfig): TeslaVehicleData {
        return try {
            client.getVehicleData(config)
        } catch (e: Exception) {
            if (e.message?.contains("401") != true) throw e
            if (!ServiceLocator.refreshTeslaToken()) throw e
            val fresh = ServiceLocator.teslaApiConfig ?: throw e
            client.getVehicleData(fresh)
        }
    }
}
