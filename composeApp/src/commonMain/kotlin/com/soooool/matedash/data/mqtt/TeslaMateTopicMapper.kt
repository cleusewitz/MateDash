package com.soooool.matedash.data.mqtt

import com.soooool.matedash.data.model.CarState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val routeJson = Json { ignoreUnknownKeys = true; isLenient = true }

fun CarState.applyTeslaMateTopic(attr: String, raw: String): CarState {
    // 미디어 토픽은 빈 페이로드도 의미 있음(재생 중지 시 clear)
    val isMedia = attr.startsWith("media_")
    val isRoute = attr == "active_route"
    if (raw.isEmpty() && !isMedia && !isRoute) return this
    val s = raw.trim()
    val b = s.equals("true", ignoreCase = true)
    val i = s.toIntOrNull()
    val d = s.toDoubleOrNull()
    return when (attr) {
        "display_name" -> copy(displayName = s)
        "state" -> copy(state = s)
        "version" -> copy(softwareVersion = s)
        "odometer" -> d?.let { copy(odometer = it) } ?: this
        "battery_level" -> i?.let { copy(batteryLevel = it) } ?: this
        "usable_battery_level" -> i?.let { copy(usableBatteryLevel = it) } ?: this
        "est_battery_range_km" -> d?.let { copy(estBatteryRangeKm = it) } ?: this
        "rated_battery_range_km" -> d?.let { copy(ratedBatteryRangeKm = it) } ?: this
        // ideal_battery_range_km — TeslaMate v3에서 publish됨. est가 0인 환경 대비 fallback으로 ratedBatteryRangeKm에 같이 채움.
        "ideal_battery_range_km" -> d?.let {
            if (ratedBatteryRangeKm <= 0.0) copy(ratedBatteryRangeKm = it) else this
        } ?: this
        "charge_limit_soc" -> i?.let { copy(chargeLimitSoc = it) } ?: this
        "plugged_in" -> copy(isPluggedIn = b)
        "charging_state" -> copy(chargingState = s)
        "charger_power" -> i?.let { copy(chargerPower = it) } ?: this
        "time_to_full_charge" -> d?.let { copy(timeToFullCharge = it) } ?: this
        "charge_port_door_open" -> copy(chargePortDoorOpen = b)
        "charger_voltage" -> i?.let { copy(chargerVoltage = it) } ?: this
        "charge_energy_added" -> d?.let { copy(chargeEnergyAdded = it) } ?: this
        "speed" -> i?.let { copy(speed = it) } ?: this
        "shift_state" -> copy(shiftState = s)
        "heading" -> i?.let { copy(heading = it) } ?: this
        "elevation" -> i?.let { copy(elevation = it) } ?: this
        "power" -> i?.let { copy(power = it) } ?: this
        "is_climate_on" -> copy(isClimateOn = b)
        "inside_temp" -> d?.let { copy(insideTemp = it) } ?: this
        "outside_temp" -> d?.let { copy(outsideTemp = it) } ?: this
        "is_preconditioning" -> copy(isPreconditioning = b)
        "locked" -> copy(isLocked = b)
        "sentry_mode" -> copy(sentryMode = b)
        "windows_open" -> copy(windowsOpen = b)
        "doors_open" -> copy(doorsOpen = b)
        "trunk_open" -> copy(trunkOpen = b)
        "frunk_open" -> copy(frunkOpen = b)
        "is_user_present" -> copy(isUserPresent = b)
        "geofence" -> copy(geofence = s)
        "latitude" -> d?.let { copy(latitude = it) } ?: this
        "longitude" -> d?.let { copy(longitude = it) } ?: this
        "tpms_pressure_fl" -> d?.let { copy(tpmsFl = it) } ?: this
        "tpms_pressure_fr" -> d?.let { copy(tpmsFr = it) } ?: this
        "tpms_pressure_rl" -> d?.let { copy(tpmsRl = it) } ?: this
        "tpms_pressure_rr" -> d?.let { copy(tpmsRr = it) } ?: this
        "media_title" -> copy(mediaTitle = s)
        "media_artist" -> copy(mediaArtist = s)
        "media_album" -> copy(mediaAlbum = s)
        "media_playlist" -> copy(mediaPlaylist = s)
        "media_status" -> copy(mediaStatus = s)
        // active_route — JSON 한 덩어리에 destination/도착시간/거리/배터리 등이 들어옴.
        // 주행 중 TeslaMate가 가끔 "No active route available" 에러 JSON을 publish하는 경우가 있어
        // 즉시 클리어하면 NavigationCard ↔ 일반 LeftPanel이 깜빡거림.
        // 보수적으로 처리: 유효한 destination일 때만 업데이트, 에러/nil은 모두 무시.
        // 명시적 클리어는 다른 destination이 들어오거나 앱 재시작 시점에만 발생.
        "active_route" -> parseActiveRoute(s) ?: this
        "active_route_destination" -> {
            val cleared = s.isBlank() || s.equals("nil", ignoreCase = true)
            if (cleared) this else copy(activeRouteDestination = s)
        }
        else -> this
    }
}

private fun CarState.parseActiveRoute(json: String): CarState? {
    if (json.isEmpty()) return null
    return try {
        val obj = routeJson.parseToJsonElement(json).jsonObject
        val error = obj["error"]?.jsonPrimitive?.contentOrNull
        if (!error.isNullOrBlank()) return null
        val dest = obj["destination"]?.jsonPrimitive?.contentOrNull ?: ""
        if (dest.isBlank()) return null
        copy(
            activeRouteDestination = dest,
            activeRouteMilesToArrival = obj["miles_to_arrival"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
            activeRouteMinutesToArrival = obj["minutes_to_arrival"]?.jsonPrimitive?.doubleOrNull?.toInt()
                ?: obj["minutes_to_arrival"]?.jsonPrimitive?.intOrNull ?: 0,
            activeRouteEnergyAtArrival = obj["energy_at_arrival"]?.jsonPrimitive?.intOrNull ?: 0,
            activeRouteTrafficMinutesDelay = obj["traffic_minutes_delay"]?.jsonPrimitive?.doubleOrNull?.toInt()
                ?: obj["traffic_minutes_delay"]?.jsonPrimitive?.intOrNull ?: 0,
        )
    } catch (e: Exception) {
        null
    }
}

private fun CarState.clearActiveRoute(): CarState = copy(
    activeRouteDestination = "",
    activeRouteMilesToArrival = 0.0,
    activeRouteMinutesToArrival = 0,
    activeRouteEnergyAtArrival = 0,
    activeRouteTrafficMinutesDelay = 0,
)
