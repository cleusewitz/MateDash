package com.soooool.matedash.data.mqtt

import com.soooool.matedash.data.model.CarState

fun CarState.applyTeslaMateTopic(attr: String, raw: String): CarState {
    // 미디어 토픽은 빈 페이로드도 의미 있음(재생 중지 시 clear)
    val isMedia = attr.startsWith("media_")
    if (raw.isEmpty() && !isMedia) return this
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
        else -> this
    }
}
