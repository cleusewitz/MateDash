package com.soooool.matedash.data.model

data class MqttConfig(
    val host: String,
    val port: Int = 1883,
    val carId: Int = 1,
    val username: String = "",
    val password: String = "",
)
