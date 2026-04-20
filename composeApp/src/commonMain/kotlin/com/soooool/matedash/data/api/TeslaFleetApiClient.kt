package com.soooool.matedash.data.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import io.ktor.serialization.kotlinx.json.json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

data class TeslaApiConfig(
    val baseUrl: String = "https://fleet-api.prd.na.vn.cloud.tesla.com",  // Owner API deprecated → Fleet API only
    val accessToken: String = "",
    val refreshToken: String = "",
    val clientId: String = "",
    val vehicleId: Long = 0,
)

@Serializable
data class TeslaVehicle(
    val id: Long = 0,
    @SerialName("vehicle_id") val vehicleId: Long = 0,
    val vin: String = "",
    @SerialName("display_name") val displayName: String = "",
    val state: String = "",
)

@Serializable
data class TeslaVehiclesResponse(
    val response: List<TeslaVehicle> = emptyList(),
    val count: Int = 0,
)

@Serializable
data class TeslaCommandResponse(
    val response: TeslaCommandResult? = null,
)

@Serializable
data class TeslaCommandResult(
    val result: Boolean = false,
    val reason: String = "",
)

@Serializable
data class TeslaVehicleDataResponse(
    val response: TeslaVehicleData? = null,
)

@Serializable
data class TeslaVehicleData(
    val id: Long = 0,
    val state: String = "",
    @SerialName("display_name") val displayName: String = "",
    @SerialName("charge_state") val chargeState: TeslaChargeState? = null,
    @SerialName("climate_state") val climateState: TeslaClimateState? = null,
    @SerialName("drive_state") val driveState: TeslaDriveState? = null,
    @SerialName("vehicle_state") val vehicleState: TeslaVehicleState? = null,
)

@Serializable
data class TeslaChargeState(
    @SerialName("battery_level") val batteryLevel: Int = 0,
    @SerialName("usable_battery_level") val usableBatteryLevel: Int = 0,
    @SerialName("battery_range") val batteryRange: Double = 0.0,
    @SerialName("est_battery_range") val estBatteryRange: Double = 0.0,
    @SerialName("charge_limit_soc") val chargeLimitSoc: Int = 0,
    @SerialName("charging_state") val chargingState: String = "",
    @SerialName("charger_power") val chargerPower: Int = 0,
    @SerialName("charge_rate") val chargeRate: Double = 0.0,
    @SerialName("charger_voltage") val chargerVoltage: Int = 0,
    @SerialName("charge_energy_added") val chargeEnergyAdded: Double = 0.0,
    @SerialName("time_to_full_charge") val timeToFullCharge: Double = 0.0,
    @SerialName("charge_port_door_open") val chargePortDoorOpen: Boolean = false,
    @SerialName("charge_port_latch") val chargePortLatch: String = "",
)

@Serializable
data class TeslaClimateState(
    @SerialName("inside_temp") val insideTemp: Double? = null,
    @SerialName("outside_temp") val outsideTemp: Double? = null,
    @SerialName("is_climate_on") val isClimateOn: Boolean = false,
    @SerialName("is_preconditioning") val isPreconditioning: Boolean = false,
    @SerialName("driver_temp_setting") val driverTempSetting: Double = 0.0,
)

@Serializable
data class TeslaDriveState(
    val speed: Int? = null,
    val heading: Int = 0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    @SerialName("shift_state") val shiftState: String? = null,
    val power: Int = 0,
)

@Serializable
data class TeslaVehicleState(
    val locked: Boolean = true,
    @SerialName("sentry_mode") val sentryMode: Boolean = false,
    @SerialName("odometer") val odometer: Double = 0.0,
    @SerialName("car_version") val carVersion: String = "",
    val fd_window: Int = 0,
    val fp_window: Int = 0,
    val rd_window: Int = 0,
    val rp_window: Int = 0,
    val df: Int = 0,
    val dr: Int = 0,
    val pf: Int = 0,
    val pr: Int = 0,
    val ft: Int = 0,
    val rt: Int = 0,
)

@Serializable
data class TeslaNavGpsRequest(
    val lat: Double,
    val lon: Double,
    val order: Int = 1,
)

@Serializable
data class TeslaShareRequest(
    val type: String = "share_ext_content_raw",
    val locale: String = "ko-KR",
    @SerialName("timestamp_ms") val timestampMs: Long = 0,
    val value: Map<String, String> = emptyMap(),
)

@Serializable
data class TeslaTokenResponse(
    @SerialName("access_token") val accessToken: String = "",
    @SerialName("refresh_token") val refreshToken: String = "",
    @SerialName("token_type") val tokenType: String = "",
    @SerialName("expires_in") val expiresIn: Long = 0,
)

object TeslaOAuth {
    private const val AUTH_URL = "https://auth.tesla.com/oauth2/v3/authorize"
    const val TOKEN_URL = "https://auth.tesla.com/oauth2/v3/token"
    const val REDIRECT_URI = "https://cleusewitz.github.io/callback"
    const val CLIENT_ID = "18fe6729-fc21-4548-bc97-7709b4bea740"

    private var codeVerifier: String? = null

    @OptIn(ExperimentalEncodingApi::class)
    fun generateCodeVerifier(): String {
        val bytes = Random.nextBytes(32)
        val verifier = Base64.UrlSafe.encode(bytes).trimEnd('=')
        codeVerifier = verifier
        return verifier
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun sha256Base64Url(input: String): String {
        val hash = com.soooool.matedash.data.crypto.sha256(input.encodeToByteArray())
        return Base64.UrlSafe.encode(hash).trimEnd('=')
    }

    fun buildAuthUrl(clientId: String): String {
        val verifier = generateCodeVerifier()
        val challenge = sha256Base64Url(verifier)
        return "$AUTH_URL?" +
            "client_id=$clientId" +
            "&redirect_uri=$REDIRECT_URI" +
            "&response_type=code" +
            "&scope=openid+vehicle_device_data+vehicle_cmds+vehicle_charging_cmds" +
            "&code_challenge=$challenge" +
            "&code_challenge_method=S256" +
            "&state=matedash"
    }

    fun getCodeVerifier(): String? = codeVerifier
}

class TeslaFleetApiClient {
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 30_000
        }
        install(io.ktor.client.plugins.DefaultRequest) {
            headers.append(HttpHeaders.UserAgent, "TeslaApp/4.32.6")
            headers.append("x-tesla-user-agent", "TeslaApp/4.32.6")
        }
    }

    private fun authHeader(config: TeslaApiConfig) =
        "Bearer ${config.accessToken}"

    suspend fun getVehicles(config: TeslaApiConfig): List<TeslaVehicle> {
        val url = "${config.baseUrl}/api/1/vehicles"
        println("[MateDash] getVehicles: url=$url, token=...${config.accessToken.takeLast(8)}")
        try {
            val httpResponse = httpClient.get(url) {
                header(HttpHeaders.Authorization, authHeader(config))
            }
            println("[MateDash] getVehicles: status=${httpResponse.status}")
            if (!httpResponse.status.isSuccess()) {
                val errorBody = httpResponse.bodyAsText()
                println("[MateDash] getVehicles: errorBody=$errorBody")
                throw Exception("차량 목록 조회 실패 (${httpResponse.status})")
            }
            val response: TeslaVehiclesResponse = httpResponse.body()
            println("[MateDash] getVehicles: count=${response.response.size}")
            return response.response
        } catch (e: Exception) {
            println("[MateDash] getVehicles: error=${e::class.simpleName}: ${e.message}")
            throw e
        }
    }

    suspend fun getVehicleData(config: TeslaApiConfig): TeslaVehicleData {
        val url = "${config.baseUrl}/api/1/vehicles/${config.vehicleId}/vehicle_data"
        println("[MateDash] getVehicleData: url=$url, vehicleId=${config.vehicleId}")
        try {
            val httpResponse = httpClient.get(url) {
                header(HttpHeaders.Authorization, authHeader(config))
            }
            println("[MateDash] getVehicleData: status=${httpResponse.status}")
            if (!httpResponse.status.isSuccess()) {
                val errorBody = httpResponse.bodyAsText()
                println("[MateDash] getVehicleData: errorBody=$errorBody")
                throw Exception("차량 데이터 조회 실패 (${httpResponse.status})")
            }
            val response: TeslaVehicleDataResponse = httpResponse.body()
            return response.response ?: throw Exception("Vehicle data not available")
        } catch (e: Exception) {
            println("[MateDash] getVehicleData: error=${e::class.simpleName}: ${e.message}")
            throw e
        }
    }

    suspend fun registerPartnerAccount(config: TeslaApiConfig): Boolean {
        val url = "${config.baseUrl}/api/1/partner_accounts"
        println("[MateDash] registerPartnerAccount: url=$url")
        try {
            val httpResponse = httpClient.submitForm(
                url = url,
                formParameters = parameters {
                    append("domain", "cleusewitz.github.io")
                },
            ) {
                header(HttpHeaders.Authorization, authHeader(config))
            }
            println("[MateDash] registerPartnerAccount: status=${httpResponse.status}")
            if (!httpResponse.status.isSuccess()) {
                val errorBody = httpResponse.bodyAsText()
                println("[MateDash] registerPartnerAccount: errorBody=$errorBody")
                // 이미 등록된 경우도 있으므로 에러를 무시할 수 있음
                return false
            }
            println("[MateDash] registerPartnerAccount: success")
            return true
        } catch (e: Exception) {
            println("[MateDash] registerPartnerAccount: error=${e.message}")
            return false
        }
    }

    suspend fun wakeUp(config: TeslaApiConfig): Boolean {
        val response: TeslaCommandResponse = httpClient.post(
            "${config.baseUrl}/api/1/vehicles/${config.vehicleId}/wake_up"
        ) {
            header(HttpHeaders.Authorization, authHeader(config))
        }.body()
        return response.response?.result ?: false
    }

    /** 차량 네비게이션에 GPS 좌표 전송 */
    suspend fun sendNavigationGps(
        config: TeslaApiConfig,
        latitude: Double,
        longitude: Double,
    ): TeslaCommandResult {
        val url = "${config.baseUrl}/api/1/vehicles/${config.vehicleId}/command/navigation_gps_request"
        println("[MateDash] navigation_gps_request: lat=$latitude, lon=$longitude")
        return try {
            val httpResponse = httpClient.post(url) {
                header(HttpHeaders.Authorization, authHeader(config))
                contentType(ContentType.Application.Json)
                setBody(TeslaNavGpsRequest(lat = latitude, lon = longitude))
            }
            println("[MateDash] navigation_gps_request: status=${httpResponse.status}")
            if (!httpResponse.status.isSuccess()) {
                val body = httpResponse.bodyAsText()
                println("[MateDash] navigation_gps_request: errorBody=$body")
                TeslaCommandResult(false, "${httpResponse.status}: $body")
            } else {
                val response: TeslaCommandResponse = httpResponse.body()
                response.response ?: TeslaCommandResult(true, "")
            }
        } catch (e: Exception) {
            println("[MateDash] navigation_gps_request: error=${e.message}")
            TeslaCommandResult(false, e.message ?: "error")
        }
    }

    /** 차량으로 텍스트(URL/주소) 공유 — 차량 내 파서가 주소/URL을 해석해 네비에 입력 */
    @OptIn(ExperimentalTime::class)
    suspend fun shareToVehicle(
        config: TeslaApiConfig,
        text: String,
        subject: String = "",
    ): TeslaCommandResult {
        val url = "${config.baseUrl}/api/1/vehicles/${config.vehicleId}/command/share"
        val now = Clock.System.now().toEpochMilliseconds()
        val req = TeslaShareRequest(
            timestampMs = now,
            value = buildMap {
                put("android.intent.extra.TEXT", text)
                if (subject.isNotBlank()) put("android.intent.extra.SUBJECT", subject)
            },
        )
        return try {
            val httpResponse = httpClient.post(url) {
                header(HttpHeaders.Authorization, authHeader(config))
                contentType(ContentType.Application.Json)
                setBody(req)
            }
            println("[MateDash] share: status=${httpResponse.status}")
            if (!httpResponse.status.isSuccess()) {
                val body = httpResponse.bodyAsText()
                println("[MateDash] share: errorBody=$body")
                TeslaCommandResult(false, "${httpResponse.status}: $body")
            } else {
                val response: TeslaCommandResponse = httpResponse.body()
                response.response ?: TeslaCommandResult(true, "")
            }
        } catch (e: Exception) {
            println("[MateDash] share: error=${e.message}")
            TeslaCommandResult(false, e.message ?: "error")
        }
    }

    suspend fun sendCommand(config: TeslaApiConfig, command: String): TeslaCommandResult {
        val response: TeslaCommandResponse = httpClient.post(
            "${config.baseUrl}/api/1/vehicles/${config.vehicleId}/command/$command"
        ) {
            header(HttpHeaders.Authorization, authHeader(config))
        }.body()
        return response.response ?: TeslaCommandResult(false, "No response")
    }

    suspend fun exchangeCodeForToken(
        clientId: String,
        code: String,
        codeVerifier: String,
    ): TeslaTokenResponse {
        println("[MateDash] exchangeCodeForToken: clientId=${clientId.take(8)}..., code=${code.take(8)}..., verifier=${codeVerifier.take(8)}...")
        try {
            val httpResponse = httpClient.submitForm(
                url = TeslaOAuth.TOKEN_URL,
                formParameters = parameters {
                    append("grant_type", "authorization_code")
                    append("client_id", clientId)
                    append("code", code)
                    append("code_verifier", codeVerifier)
                    append("redirect_uri", TeslaOAuth.REDIRECT_URI)
                },
            )
            println("[MateDash] exchangeCodeForToken: status=${httpResponse.status}")
            if (!httpResponse.status.isSuccess()) {
                val errorBody = httpResponse.bodyAsText()
                println("[MateDash] exchangeCodeForToken: errorBody=$errorBody")
                throw Exception("토큰 교환 실패 (${httpResponse.status}): $errorBody")
            }
            val token: TeslaTokenResponse = httpResponse.body()
            println("[MateDash] exchangeCodeForToken: hasAccessToken=${token.accessToken.isNotBlank()}, hasRefresh=${token.refreshToken.isNotBlank()}")
            if (token.accessToken.isBlank()) {
                throw Exception("토큰이 비어있습니다. 인증 코드를 다시 확인해주세요.")
            }
            return token
        } catch (e: Exception) {
            println("[MateDash] exchangeCodeForToken: error=${e::class.simpleName}: ${e.message}")
            throw e
        }
    }

    suspend fun refreshAccessToken(
        clientId: String,
        refreshToken: String,
    ): TeslaTokenResponse {
        println("[MateDash] refreshAccessToken: clientId=${clientId.take(8)}...")
        try {
            val httpResponse = httpClient.submitForm(
                url = TeslaOAuth.TOKEN_URL,
                formParameters = parameters {
                    append("grant_type", "refresh_token")
                    append("client_id", clientId)
                    append("refresh_token", refreshToken)
                },
            )
            println("[MateDash] refreshAccessToken: status=${httpResponse.status}")
            if (!httpResponse.status.isSuccess()) {
                val errorBody = httpResponse.bodyAsText()
                println("[MateDash] refreshAccessToken: errorBody=$errorBody")
                throw Exception("토큰 갱신 실패 (${httpResponse.status}): $errorBody")
            }
            val token: TeslaTokenResponse = httpResponse.body()
            return token
        } catch (e: Exception) {
            println("[MateDash] refreshAccessToken: error=${e::class.simpleName}: ${e.message}")
            throw e
        }
    }

    fun close() = httpClient.close()
}
