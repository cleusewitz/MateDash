package com.soooool.matedash.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DrivesResponse(
    val data: DrivesData? = null,
)

@Serializable
data class DrivesData(
    val drives: List<DriveDto> = emptyList(),
)

@Serializable
data class DriveDto(
    @SerialName("drive_id") val driveId: Int? = null,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    @SerialName("start_address") val startAddress: String? = null,
    @SerialName("end_address") val endAddress: String? = null,
    @SerialName("odometer_details") val odometerDetails: DriveOdometerDetails? = null,
    @SerialName("duration_min") val durationMin: Int? = null,
    @SerialName("duration_str") val durationStr: String? = null,
    @SerialName("speed_max") val speedMax: Int? = null,
    @SerialName("speed_avg") val speedAvg: Double? = null,
    @SerialName("battery_details") val batteryDetails: DriveBatteryDetails? = null,
    @SerialName("outside_temp_avg") val outsideTempAvg: Double? = null,
    @SerialName("energy_consumed_net") val energyConsumedNet: Double? = null,
)

@Serializable
data class DriveOdometerDetails(
    @SerialName("odometer_distance") val odometerDistance: Double? = null,
)

@Serializable
data class DriveBatteryDetails(
    @SerialName("start_battery_level") val startBatteryLevel: Int? = null,
    @SerialName("end_battery_level") val endBatteryLevel: Int? = null,
)
