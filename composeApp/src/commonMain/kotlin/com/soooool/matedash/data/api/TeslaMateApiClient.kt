package com.soooool.matedash.data.api

import com.soooool.matedash.data.model.ApiConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class TeslaMateApiClient {
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    suspend fun getCarStatus(config: ApiConfig): CarStatusDto {
        val response: ApiResponse = httpClient.get("${config.baseUrl}/api/v1/cars/${config.carId}/status") {
            if (config.apiToken.isNotBlank()) header(HttpHeaders.Authorization, "Bearer ${config.apiToken}")
        }.body()
        return response.data?.status ?: throw Exception("응답 데이터 없음")
    }

    suspend fun getCars(config: ApiConfig): List<CarListItemDto> {
        return try {
            httpClient.get("${config.baseUrl}/api/v1/cars") {
                if (config.apiToken.isNotBlank()) header(HttpHeaders.Authorization, "Bearer ${config.apiToken}")
            }.body()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getDrives(config: ApiConfig, limit: Int = 20): List<DriveDto> {
        val response: DrivesResponse = httpClient.get(
            "${config.baseUrl}/api/v1/cars/${config.carId}/drives?limit=$limit"
        ) {
            if (config.apiToken.isNotBlank()) header(HttpHeaders.Authorization, "Bearer ${config.apiToken}")
        }.body()
        return response.data?.drives ?: emptyList()
    }

    suspend fun getCharges(config: ApiConfig, limit: Int = 20): List<ChargeDto> {
        val response: ChargesResponse = httpClient.get(
            "${config.baseUrl}/api/v1/cars/${config.carId}/charges?limit=$limit"
        ) {
            if (config.apiToken.isNotBlank()) header(HttpHeaders.Authorization, "Bearer ${config.apiToken}")
        }.body()
        return response.data?.charges ?: emptyList()
    }

    fun close() = httpClient.close()
}

@kotlinx.serialization.Serializable
data class CarListItemDto(
    val id: Int? = null,
    val name: String? = null,
    val model: String? = null,
)
