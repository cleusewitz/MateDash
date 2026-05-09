package com.soooool.matedash.data.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class PositionPoint(val latitude: Double, val longitude: Double)

class GrafanaClient {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val httpClient = HttpClient {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
        }
    }

    private var cachedDatasourceUid: String? = null

    private fun authHeaders(
        apiKey: String?,
        user: String?,
        password: String?,
    ): Pair<String, String>? = when {
        !apiKey.isNullOrBlank() -> "Authorization" to "Bearer $apiKey"
        !user.isNullOrBlank() && !password.isNullOrBlank() -> {
            val credentials = "$user:$password"
            val encoded = kotlin.io.encoding.Base64.Default.encode(credentials.encodeToByteArray())
            "Authorization" to "Basic $encoded"
        }
        else -> null
    }

    @OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
    private suspend fun findPostgresDatasourceUid(
        grafanaUrl: String,
        apiKey: String?,
        user: String? = null,
        password: String? = null,
    ): String? {
        cachedDatasourceUid?.let { return it }
        try {
            val auth = authHeaders(apiKey, user, password)
            // 진단용: 실제 전송하는 자격증명 정보 (비밀번호는 길이만)
            val authDesc = when {
                !apiKey.isNullOrBlank() -> "ApiKey(len=${apiKey.length})"
                !user.isNullOrBlank() && !password.isNullOrBlank() -> "Basic(user='$user', pwLen=${password.length})"
                !user.isNullOrBlank() -> "user='$user'이지만 password 비어있음 → 인증 헤더 안 보냄"
                else -> "인증 정보 없음"
            }
            println("[MateDash] Grafana auth: $authDesc")
            println("[MateDash] Grafana: finding datasource at $grafanaUrl/api/datasources")
            val resp = httpClient.get("$grafanaUrl/api/datasources") {
                auth?.let { header(it.first, it.second) }
            }
            println("[MateDash] Grafana datasources status: ${resp.status}")
            if (!resp.status.isSuccess()) {
                println("[MateDash] Grafana datasources errorBody: ${resp.bodyAsText().take(200)}")
                return null
            }
            val body = resp.bodyAsText()
            val arr = json.parseToJsonElement(body).jsonArray
            for (ds in arr) {
                val obj = ds.jsonObject
                val type = obj["type"]?.jsonPrimitive?.content ?: ""
                if (type == "postgres" || type == "grafana-postgresql-datasource") {
                    val uid = obj["uid"]?.jsonPrimitive?.content
                    cachedDatasourceUid = uid
                    return uid
                }
            }
        } catch (e: Exception) {
            println("[MateDash] Grafana findDatasource error: ${e.message}")
        }
        return null
    }

    @OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
    suspend fun getDrivePositions(
        grafanaUrl: String,
        startDate: String,
        endDate: String,
        carId: Int,
        apiKey: String? = null,
        user: String? = null,
        password: String? = null,
    ): List<PositionPoint> {
        val uid = findPostgresDatasourceUid(grafanaUrl, apiKey, user, password)
            ?: throw Exception("Grafana datasource를 찾을 수 없습니다. URL: $grafanaUrl, 인증: ${if (!user.isNullOrBlank()) "Basic($user)" else if (!apiKey.isNullOrBlank()) "ApiKey" else "없음"}")
        val auth = authHeaders(apiKey, user, password)

        val sql = "SELECT latitude, longitude FROM positions " +
            "WHERE date >= '$startDate' AND date <= '$endDate' " +
            "AND car_id = $carId ORDER BY date"

        val requestBody = """
            {
              "queries": [{
                "refId": "A",
                "datasource": {"type": "postgres", "uid": "$uid"},
                "rawSql": "$sql",
                "format": "table"
              }],
              "from": "0",
              "to": "0"
            }
        """.trimIndent()

        try {
            println("[MateDash] Grafana SQL: $sql")
            val resp = httpClient.post("$grafanaUrl/api/ds/query") {
                auth?.let { header(it.first, it.second) }
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            println("[MateDash] Grafana query status: ${resp.status}")
            if (!resp.status.isSuccess()) {
                val errBody = resp.bodyAsText()
                println("[MateDash] Grafana query errorBody: ${errBody.take(400)}")
                throw Exception("Grafana 쿼리 실패 (${resp.status})")
            }
            val body = resp.bodyAsText()
            println("[MateDash] Grafana query body (first 600): ${body.take(600)}")
            return parsePositionsResponse(body)
        } catch (e: Exception) {
            throw e
        }
    }

    private fun parsePositionsResponse(body: String): List<PositionPoint> {
        try {
            val root = json.parseToJsonElement(body).jsonObject
            val results = root["results"]?.jsonObject ?: return emptyList()
            val a = results["A"]?.jsonObject ?: return emptyList()
            val frames = a["frames"]?.jsonArray ?: return emptyList()
            if (frames.isEmpty()) return emptyList()

            val frame = frames[0].jsonObject
            val data = frame["data"]?.jsonObject ?: return emptyList()
            val values = data["values"]?.jsonArray ?: return emptyList()
            if (values.size < 2) return emptyList()

            val lats = values[0].jsonArray
            val lngs = values[1].jsonArray
            val count = minOf(lats.size, lngs.size)

            return (0 until count).map { i ->
                PositionPoint(
                    latitude = lats[i].jsonPrimitive.double,
                    longitude = lngs[i].jsonPrimitive.double,
                )
            }
        } catch (e: Exception) {
            println("[MateDash] Grafana parse error: ${e.message}")
            return emptyList()
        }
    }

    fun close() = httpClient.close()
}
