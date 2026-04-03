package com.soooool.matedash.data.api

import com.soooool.matedash.data.model.CarState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── 루트 응답 래퍼 ──────────────────────────────────────────
@Serializable
data class ApiResponse(
    val data: ApiData? = null,
)

@Serializable
data class ApiData(
    val status: CarStatusDto? = null,
)

// ── 상태 루트 ────────────────────────────────────────────────
@Serializable
data class CarStatusDto(
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("state") val state: String? = null,
    @SerialName("odometer") val odometer: Double? = null,
    @SerialName("car_status") val carStatus: CarStatusDetails? = null,
    @SerialName("climate_details") val climateDetails: ClimateDetails? = null,
    @SerialName("battery_details") val batteryDetails: BatteryDetails? = null,
    @SerialName("charging_details") val chargingDetails: ChargingDetails? = null,
    @SerialName("driving_details") val drivingDetails: DrivingDetails? = null,
    @SerialName("car_geodata") val carGeodata: CarGeodata? = null,
    @SerialName("car_versions") val carVersions: CarVersions? = null,
    @SerialName("tpms_details") val tpmsDetails: TpmsDetails? = null,
)

@Serializable
data class CarStatusDetails(
    @SerialName("locked") val locked: Boolean? = null,
    @SerialName("sentry_mode") val sentryMode: Boolean? = null,
    @SerialName("windows_open") val windowsOpen: Boolean? = null,
    @SerialName("doors_open") val doorsOpen: Boolean? = null,
    @SerialName("trunk_open") val trunkOpen: Boolean? = null,
    @SerialName("frunk_open") val frunkOpen: Boolean? = null,
    @SerialName("is_user_present") val isUserPresent: Boolean? = null,
)

@Serializable
data class ClimateDetails(
    @SerialName("is_climate_on") val isClimateOn: Boolean? = null,
    @SerialName("inside_temp") val insideTemp: Double? = null,
    @SerialName("outside_temp") val outsideTemp: Double? = null,
    @SerialName("is_preconditioning") val isPreconditioning: Boolean? = null,
)

@Serializable
data class BatteryDetails(
    @SerialName("battery_level") val batteryLevel: Int? = null,
    @SerialName("usable_battery_level") val usableBatteryLevel: Int? = null,
    @SerialName("est_battery_range") val estBatteryRange: Double? = null,
    @SerialName("rated_battery_range") val ratedBatteryRange: Double? = null,
    @SerialName("ideal_battery_range") val idealBatteryRange: Double? = null,
)

@Serializable
data class ChargingDetails(
    @SerialName("plugged_in") val pluggedIn: Boolean? = null,
    @SerialName("charging_state") val chargingState: String? = null,
    @SerialName("charge_limit_soc") val chargeLimitSoc: Int? = null,
    @SerialName("charger_power") val chargerPower: Int? = null,
    @SerialName("charger_voltage") val chargerVoltage: Int? = null,
    @SerialName("time_to_full_charge") val timeToFullCharge: Double? = null,
    @SerialName("charge_port_door_open") val chargePortDoorOpen: Boolean? = null,
    @SerialName("charge_energy_added") val chargeEnergyAdded: Double? = null,
)

@Serializable
data class DrivingDetails(
    @SerialName("speed") val speed: Int? = null,
    @SerialName("shift_state") val shiftState: String? = null,
    @SerialName("heading") val heading: Int? = null,
    @SerialName("elevation") val elevation: Int? = null,
    @SerialName("power") val power: Int? = null,
)

@Serializable
data class CarGeodata(
    @SerialName("geofence") val geofence: String? = null,
    @SerialName("latitude") val latitude: Double? = null,
    @SerialName("longitude") val longitude: Double? = null,
)

@Serializable
data class CarVersions(
    @SerialName("version") val version: String? = null,
    @SerialName("update_available") val updateAvailable: Boolean? = null,
    @SerialName("update_version") val updateVersion: String? = null,
)

@Serializable
data class TpmsDetails(
    @SerialName("tpms_pressure_fl") val pressureFl: Double? = null,
    @SerialName("tpms_pressure_fr") val pressureFr: Double? = null,
    @SerialName("tpms_pressure_rl") val pressureRl: Double? = null,
    @SerialName("tpms_pressure_rr") val pressureRr: Double? = null,
    @SerialName("tpms_soft_warning_fl") val warningFl: Boolean? = null,
    @SerialName("tpms_soft_warning_fr") val warningFr: Boolean? = null,
    @SerialName("tpms_soft_warning_rl") val warningRl: Boolean? = null,
    @SerialName("tpms_soft_warning_rr") val warningRr: Boolean? = null,
)

// ── 매핑 ─────────────────────────────────────────────────────
fun CarStatusDto.toCarState(): CarState = CarState(
    displayName = displayName ?: "Tesla",
    state = state ?: "unknown",
    odometer = odometer ?: 0.0,
    softwareVersion = carVersions?.version ?: "",
    batteryLevel = batteryDetails?.batteryLevel ?: 0,
    usableBatteryLevel = batteryDetails?.usableBatteryLevel ?: 0,
    estBatteryRangeKm = batteryDetails?.estBatteryRange ?: 0.0,
    ratedBatteryRangeKm = batteryDetails?.ratedBatteryRange ?: 0.0,
    chargeLimitSoc = chargingDetails?.chargeLimitSoc ?: 80,
    isPluggedIn = chargingDetails?.pluggedIn ?: false,
    chargingState = chargingDetails?.chargingState ?: "",
    chargerPower = chargingDetails?.chargerPower ?: 0,
    chargerVoltage = chargingDetails?.chargerVoltage ?: 0,
    timeToFullCharge = chargingDetails?.timeToFullCharge ?: 0.0,
    chargePortDoorOpen = chargingDetails?.chargePortDoorOpen ?: false,
    chargeEnergyAdded = chargingDetails?.chargeEnergyAdded ?: 0.0,
    speed = drivingDetails?.speed ?: 0,
    shiftState = drivingDetails?.shiftState ?: "",
    heading = drivingDetails?.heading ?: 0,
    elevation = drivingDetails?.elevation ?: 0,
    power = drivingDetails?.power ?: 0,
    isClimateOn = climateDetails?.isClimateOn ?: false,
    insideTemp = climateDetails?.insideTemp ?: 0.0,
    outsideTemp = climateDetails?.outsideTemp ?: 0.0,
    isPreconditioning = climateDetails?.isPreconditioning ?: false,
    isLocked = carStatus?.locked ?: true,
    sentryMode = carStatus?.sentryMode ?: false,
    windowsOpen = carStatus?.windowsOpen ?: false,
    doorsOpen = carStatus?.doorsOpen ?: false,
    trunkOpen = carStatus?.trunkOpen ?: false,
    frunkOpen = carStatus?.frunkOpen ?: false,
    isUserPresent = carStatus?.isUserPresent ?: false,
    geofence = carGeodata?.geofence ?: "",
    latitude = carGeodata?.latitude ?: 0.0,
    longitude = carGeodata?.longitude ?: 0.0,
    tpmsFl = tpmsDetails?.pressureFl ?: 0.0,
    tpmsFr = tpmsDetails?.pressureFr ?: 0.0,
    tpmsRl = tpmsDetails?.pressureRl ?: 0.0,
    tpmsRr = tpmsDetails?.pressureRr ?: 0.0,
)
