package com.soooool.matedash.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChargesResponse(
    val data: ChargesData? = null,
)

@Serializable
data class ChargesData(
    val charges: List<ChargeDto> = emptyList(),
)

@Serializable
data class ChargeDto(
    @SerialName("charge_id") val chargeId: Int? = null,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    @SerialName("address") val address: String? = null,
    @SerialName("charge_energy_added") val chargeEnergyAdded: Double? = null,
    @SerialName("charge_energy_used") val chargeEnergyUsed: Double? = null,
    @SerialName("cost") val cost: Double? = null,
    @SerialName("duration_min") val durationMin: Int? = null,
    @SerialName("duration_str") val durationStr: String? = null,
    @SerialName("battery_details") val batteryDetails: ChargeBatteryDetails? = null,
    @SerialName("outside_temp_avg") val outsideTempAvg: Double? = null,
    @SerialName("odometer") val odometer: Double? = null,
)

@Serializable
data class ChargeBatteryDetails(
    @SerialName("start_battery_level") val startBatteryLevel: Int? = null,
    @SerialName("end_battery_level") val endBatteryLevel: Int? = null,
)
