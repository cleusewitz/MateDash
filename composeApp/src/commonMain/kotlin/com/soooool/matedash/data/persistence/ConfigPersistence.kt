package com.soooool.matedash.data.persistence

import com.soooool.matedash.data.api.TeslaApiConfig
import com.soooool.matedash.data.model.ApiConfig

expect fun saveApiConfig(config: ApiConfig)

expect fun loadApiConfig(): ApiConfig?

expect fun clearApiConfig()

expect fun saveTeslaApiConfig(config: TeslaApiConfig)

expect fun loadTeslaApiConfig(): TeslaApiConfig?

expect fun clearTeslaApiConfig()
