package com.soooool.matedash.ui.util

import com.soooool.matedash.ServiceLocator
import com.soooool.matedash.data.model.DistanceUnit
import com.soooool.matedash.data.model.TemperatureUnit

fun Double.toDisplayTemp(): String {
    val settings = ServiceLocator.appSettings
    return if (settings.temperatureUnit == TemperatureUnit.FAHRENHEIT) {
        val f = this * 9.0 / 5.0 + 32.0
        "${f.fmt1()}°F"
    } else {
        "${this.fmt1()}°C"
    }
}

fun Double.toDisplayKm(): String {
    val settings = ServiceLocator.appSettings
    return if (settings.distanceUnit == DistanceUnit.MILES) {
        "${(this * 0.621371).fmt0Comma()} mi"
    } else {
        "${this.fmt0Comma()} km"
    }
}

fun Double.toDisplaySpeed(): String {
    val settings = ServiceLocator.appSettings
    return if (settings.distanceUnit == DistanceUnit.MILES) {
        "${(this * 0.621371).toInt()} mph"
    } else {
        "${this.toInt()} km/h"
    }
}

fun Double.toDisplayKmShort(): String {
    val settings = ServiceLocator.appSettings
    return if (settings.distanceUnit == DistanceUnit.MILES) {
        "${(this * 0.621371).fmt0Comma()} mi"
    } else {
        "${this.fmt0Comma()} km"
    }
}

private fun Double.fmt1(): String {
    val rounded = (this * 10).toLong() / 10.0
    return if (rounded == rounded.toLong().toDouble()) "${rounded.toLong()}.0" else "$rounded"
}

private fun Double.fmt0Comma(): String {
    val value = this.toLong()
    return value.toString().reversed().chunked(3).joinToString(",").reversed()
}
