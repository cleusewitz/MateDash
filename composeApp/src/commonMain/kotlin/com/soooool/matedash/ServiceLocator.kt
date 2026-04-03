package com.soooool.matedash

import com.soooool.matedash.data.api.TeslaMateApiClient
import com.soooool.matedash.data.model.ApiConfig
import com.soooool.matedash.data.repository.TeslaMateRepository

object ServiceLocator {
    val apiClient by lazy { TeslaMateApiClient() }
    val repository by lazy { TeslaMateRepository(apiClient) }
    var currentConfig: ApiConfig? = null
}
