package com.soooool.matedash.data.persistence

import com.soooool.matedash.data.model.CarState
import platform.Foundation.NSUserDefaults

private const val SUITE_NAME = "group.com.soooool.matedash"
private const val KEY_LA_BATTERY = "la_battery_level"
private const val KEY_LA_CHARGER_POWER = "la_charger_power"
private const val KEY_LA_CHARGE_LIMIT = "la_charge_limit_soc"
private const val KEY_LA_TIME_FULL = "la_time_to_full"
private const val KEY_LA_CHARGING_STATE = "la_charging_state"
private const val KEY_LA_ENERGY_ADDED = "la_energy_added"
private const val KEY_LA_DISPLAY_NAME = "la_display_name"
private const val KEY_LA_UPDATED = "la_updated"
private const val KEY_LA_CHARGER_VOLTAGE = "la_charger_voltage"
private const val KEY_LA_GEOFENCE = "la_geofence"

// 주행 Live Activity 키
private const val KEY_LA_DRIVING_STATE = "la_driving_state"
private const val KEY_LA_DRIVING_SPEED = "la_driving_speed"
private const val KEY_LA_DRIVING_BATTERY = "la_driving_battery"
private const val KEY_LA_DRIVING_DISTANCE = "la_driving_distance"
private const val KEY_LA_DRIVING_POWER = "la_driving_power"
private const val KEY_LA_DRIVING_DURATION = "la_driving_duration_min"
private const val KEY_LA_DRIVING_START_ADDR = "la_driving_start_address"

// Debug 키
private const val KEY_DBG_UPDATED = "la_dbg_updated"
private const val KEY_DBG_STATE = "la_dbg_state"
private const val KEY_DBG_SHIFT = "la_dbg_shift"
private const val KEY_DBG_SPEED = "la_dbg_speed"
private const val KEY_DBG_POWER = "la_dbg_power"
private const val KEY_DBG_CHARGING = "la_dbg_charging"
private const val KEY_DBG_IS_DRIVING = "la_dbg_is_driving"
private const val KEY_DBG_IS_CHARGING = "la_dbg_is_charging"
private const val KEY_DBG_DRIVING_LAST = "la_dbg_driving_last"
private const val KEY_DBG_CHARGING_LAST = "la_dbg_charging_last"

/** 테스트: 충전 Live Activity */
actual fun startTestLiveActivity() {
    startTestChargingLiveActivity()
}

fun startTestChargingLiveActivity() {
    val defaults = NSUserDefaults(suiteName = SUITE_NAME) ?: return

    defaults.setInteger(72, forKey = KEY_LA_BATTERY)
    defaults.setInteger(11, forKey = KEY_LA_CHARGER_POWER)
    defaults.setInteger(90, forKey = KEY_LA_CHARGE_LIMIT)
    defaults.setDouble(1.5, forKey = KEY_LA_TIME_FULL)
    defaults.setObject("Charging", forKey = KEY_LA_CHARGING_STATE)
    defaults.setDouble(18.7, forKey = KEY_LA_ENERGY_ADDED)
    defaults.setObject("MateDash Test", forKey = KEY_LA_DISPLAY_NAME)
    defaults.setInteger(220, forKey = KEY_LA_CHARGER_VOLTAGE)
    defaults.setObject("Home", forKey = KEY_LA_GEOFENCE)
    val now = platform.Foundation.NSDate()
    defaults.setDouble(now.timeIntervalSinceReferenceDate, forKey = KEY_LA_UPDATED)
    defaults.synchronize()

    platform.Foundation.NSNotificationCenter.defaultCenter.postNotificationName(
        "com.soooool.matedash.forceCheckLiveActivity",
        `object` = null,
    )
}

/** 테스트: 주행 Live Activity */
actual fun startTestDrivingLiveActivity() {
    val defaults = NSUserDefaults(suiteName = SUITE_NAME) ?: return

    defaults.setObject("MateDash Test", forKey = KEY_LA_DISPLAY_NAME)
    defaults.setObject("Driving", forKey = KEY_LA_DRIVING_STATE)
    defaults.setInteger(82, forKey = KEY_LA_DRIVING_SPEED)
    defaults.setInteger(75, forKey = KEY_LA_DRIVING_BATTERY)
    defaults.setDouble(23.4, forKey = KEY_LA_DRIVING_DISTANCE)
    defaults.setInteger(-18, forKey = KEY_LA_DRIVING_POWER)
    defaults.setInteger(35, forKey = KEY_LA_DRIVING_DURATION)
    defaults.setObject("집", forKey = KEY_LA_DRIVING_START_ADDR)
    val now = platform.Foundation.NSDate()
    defaults.setDouble(now.timeIntervalSinceReferenceDate, forKey = KEY_LA_UPDATED)
    defaults.synchronize()

    platform.Foundation.NSNotificationCenter.defaultCenter.postNotificationName(
        "com.soooool.matedash.forceCheckLiveActivity",
        `object` = null,
    )
}

actual fun stopTestLiveActivity() {
    val defaults = NSUserDefaults(suiteName = SUITE_NAME) ?: return

    // 충전/주행 상태 모두 초기화
    defaults.setObject("", forKey = KEY_LA_CHARGING_STATE)
    defaults.setObject("", forKey = "la_driving_state")
    defaults.synchronize()

    // 즉시 모든 Live Activity 제거
    platform.Foundation.NSNotificationCenter.defaultCenter.postNotificationName(
        "com.soooool.matedash.forceEndAllLiveActivities",
        `object` = null,
    )
}

actual fun updateLiveActivityState(carState: CarState) {
    val defaults = NSUserDefaults(suiteName = SUITE_NAME) ?: return

    // 슈퍼차저 판별: 전압 300V 이상이거나 geofence에 "supercharger" 포함
    val isSupercharger = carState.chargerVoltage > 300 ||
        carState.geofence.lowercase().contains("supercharger")

    // 충전 상태 기록 (슈퍼차저면 빈 문자열로 → Live Activity 비활성)
    val chargingState = if (isSupercharger) "" else carState.chargingState

    defaults.setInteger(carState.batteryLevel.toLong(), forKey = KEY_LA_BATTERY)
    defaults.setInteger(carState.chargerPower.toLong(), forKey = KEY_LA_CHARGER_POWER)
    defaults.setInteger(carState.chargeLimitSoc.toLong(), forKey = KEY_LA_CHARGE_LIMIT)
    defaults.setDouble(carState.timeToFullCharge, forKey = KEY_LA_TIME_FULL)
    defaults.setObject(chargingState, forKey = KEY_LA_CHARGING_STATE)
    defaults.setDouble(carState.chargeEnergyAdded, forKey = KEY_LA_ENERGY_ADDED)
    defaults.setObject(carState.displayName, forKey = KEY_LA_DISPLAY_NAME)
    defaults.setInteger(carState.chargerVoltage.toLong(), forKey = KEY_LA_CHARGER_VOLTAGE)
    defaults.setObject(carState.geofence, forKey = KEY_LA_GEOFENCE)

    // 주행 데이터 (shiftState가 D/R이면 주행 중)
    val isDriving = carState.shiftState.uppercase().let { it == "D" || it == "R" }
    defaults.setObject(if (isDriving) "Driving" else "", forKey = KEY_LA_DRIVING_STATE)
    defaults.setInteger(carState.speed.toLong(), forKey = KEY_LA_DRIVING_SPEED)
    defaults.setInteger(carState.batteryLevel.toLong(), forKey = KEY_LA_DRIVING_BATTERY)
    defaults.setInteger(carState.power.toLong(), forKey = KEY_LA_DRIVING_POWER)
    defaults.setObject(carState.geofence, forKey = KEY_LA_DRIVING_START_ADDR)

    val isCharging = chargingState.lowercase() == "charging"

    // Debug 스냅샷
    defaults.setObject(carState.state, forKey = KEY_DBG_STATE)
    defaults.setObject(carState.shiftState, forKey = KEY_DBG_SHIFT)
    defaults.setInteger(carState.speed.toLong(), forKey = KEY_DBG_SPEED)
    defaults.setInteger(carState.power.toLong(), forKey = KEY_DBG_POWER)
    defaults.setObject(carState.chargingState, forKey = KEY_DBG_CHARGING)
    defaults.setBool(isDriving, forKey = KEY_DBG_IS_DRIVING)
    defaults.setBool(isCharging, forKey = KEY_DBG_IS_CHARGING)
    defaults.setObject(formatNow(), forKey = KEY_DBG_UPDATED)

    val now = platform.Foundation.NSDate()
    defaults.setDouble(now.timeIntervalSinceReferenceDate, forKey = KEY_LA_UPDATED)
    defaults.synchronize()
}

private fun formatNow(): String {
    val formatter = platform.Foundation.NSDateFormatter()
    formatter.dateFormat = "HH:mm:ss"
    return formatter.stringFromDate(platform.Foundation.NSDate())
}

actual fun readLiveActivityDebug(): LiveActivityDebug {
    val defaults = NSUserDefaults(suiteName = SUITE_NAME) ?: return LiveActivityDebug()
    return LiveActivityDebug(
        lastUpdate = defaults.stringForKey(KEY_DBG_UPDATED) ?: "",
        rawState = defaults.stringForKey(KEY_DBG_STATE) ?: "",
        rawShiftState = defaults.stringForKey(KEY_DBG_SHIFT) ?: "",
        rawSpeed = defaults.integerForKey(KEY_DBG_SPEED).toInt(),
        rawPower = defaults.integerForKey(KEY_DBG_POWER).toInt(),
        rawChargingState = defaults.stringForKey(KEY_DBG_CHARGING) ?: "",
        isDriving = defaults.boolForKey(KEY_DBG_IS_DRIVING),
        isCharging = defaults.boolForKey(KEY_DBG_IS_CHARGING),
        drivingLast = defaults.stringForKey(KEY_DBG_DRIVING_LAST) ?: "",
        chargingLast = defaults.stringForKey(KEY_DBG_CHARGING_LAST) ?: "",
    )
}
