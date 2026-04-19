package com.soooool.matedash.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdatesResponse(
    val data: UpdatesData? = null,
)

@Serializable
data class UpdatesData(
    val updates: List<UpdateDto> = emptyList(),
)

@Serializable
data class UpdateDto(
    @SerialName("update_id") val updateId: Int? = null,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    @SerialName("version") val version: String? = null,
)
