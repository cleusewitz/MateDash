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
    private val intervalMs = 30_000L // 30초

    fun start(config: TeslaApiConfig) {
        if (config.accessToken.isBlank() || config.vehicleId == 0L) return
        stop()
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
        // 제목이 비어있으면 업데이트 자체 skip — 응답에 미디어가 없을 때마다 클리어돼서
        // NowPlayingCard가 깜빡거리는 문제 방지. (실제 음악이 멈춘 경우는 mediaStatus로 판단)
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
