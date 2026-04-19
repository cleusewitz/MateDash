package com.soooool.matedash.data.persistence

import com.soooool.matedash.data.model.AppSettings

expect fun saveAppSettings(settings: AppSettings)
expect fun loadAppSettings(): AppSettings
