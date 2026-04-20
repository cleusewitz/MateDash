package com.soooool.matedash.data.share

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class PlaceHit(
    val provider: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
)

sealed class GeocodeResult {
    data class Success(val hits: List<PlaceHit>) : GeocodeResult()
    data class Failure(val message: String) : GeocodeResult()
}

@Serializable
private data class NominatimItem(
    @SerialName("display_name") val displayName: String = "",
    @SerialName("name") val name: String? = null,
    @SerialName("lat") val lat: String = "0",
    @SerialName("lon") val lon: String = "0",
)

@Serializable
private data class KakaoResponse(
    val documents: List<KakaoDoc> = emptyList(),
    val meta: KakaoMeta? = null,
)

@Serializable
private data class KakaoDoc(
    @SerialName("place_name") val placeName: String = "",
    @SerialName("address_name") val addressName: String = "",
    @SerialName("road_address_name") val roadAddressName: String = "",
    @SerialName("x") val x: String = "0",
    @SerialName("y") val y: String = "0",
)

@Serializable
private data class KakaoMeta(
    @SerialName("total_count") val totalCount: Int = 0,
)

@Serializable
private data class PhotonResponse(
    val features: List<PhotonFeature> = emptyList(),
)

@Serializable
private data class PhotonFeature(
    val properties: PhotonProps = PhotonProps(),
    val geometry: PhotonGeometry = PhotonGeometry(),
)

@Serializable
private data class PhotonProps(
    val name: String? = null,
    val street: String? = null,
    val housenumber: String? = null,
    val city: String? = null,
    val district: String? = null,
    val locality: String? = null,
    val country: String? = null,
    val postcode: String? = null,
    @SerialName("osm_value") val osmValue: String? = null,
)

@Serializable
private data class PhotonGeometry(
    val coordinates: List<Double> = emptyList(),
)

@Serializable
private data class GoogleTextSearchRequest(
    val textQuery: String,
    val languageCode: String = "ko",
    val regionCode: String = "kr",
    val maxResultCount: Int = 5,
)

@Serializable
private data class GoogleTextSearchResponse(
    val places: List<GooglePlace> = emptyList(),
)

@Serializable
private data class GooglePlace(
    val displayName: GoogleDisplayName? = null,
    val formattedAddress: String? = null,
    val location: GoogleLocation? = null,
)

@Serializable
private data class GoogleDisplayName(
    val text: String = "",
    val languageCode: String = "",
)

@Serializable
private data class GoogleLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
)

class PlaceGeocoder {
    private val http = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 10_000
            connectTimeoutMillis = 7_000
            socketTimeoutMillis = 10_000
        }
    }

    /** 체인: Kakao(키) → Google(키) → Nominatim → Photon. 각 제공자에서 쿼리를 점진적으로 축약하며 재시도. */
    suspend fun search(
        query: String,
        kakaoRestKey: String? = null,
        googleApiKey: String? = null,
    ): GeocodeResult {
        if (query.isBlank()) return GeocodeResult.Failure("검색어 없음")

        val variants = queryVariants(query)
        val errors = mutableListOf<String>()

        if (!kakaoRestKey.isNullOrBlank()) {
            runCatching { searchKakaoAddress(query, kakaoRestKey) }
                .onSuccess { if (it.isNotEmpty()) return GeocodeResult.Success(it) }
                .onFailure { errors += "Kakao/address: ${it.message}".also(::println) }

            for (v in variants) {
                val res = runCatching { searchKakao(v, kakaoRestKey) }
                    .onFailure { errors += "Kakao/keyword($v): ${it.message}".also(::println) }
                    .getOrNull().orEmpty()
                if (res.isNotEmpty()) return GeocodeResult.Success(res)
            }
        }

        if (!googleApiKey.isNullOrBlank()) {
            for (v in variants) {
                val res = runCatching { searchGoogle(v, googleApiKey) }
                    .onFailure { errors += "Google($v): ${it.message}".also(::println) }
                    .getOrNull().orEmpty()
                if (res.isNotEmpty()) return GeocodeResult.Success(res)
            }
        }

        for (v in variants) {
            val res = runCatching { searchNominatim(v) }
                .onFailure { errors += "Nominatim($v): ${it.message}".also(::println) }
                .getOrNull().orEmpty()
            if (res.isNotEmpty()) return GeocodeResult.Success(res)
        }

        for (v in variants) {
            val res = runCatching { searchPhoton(v) }
                .onFailure { errors += "Photon($v): ${it.message}".also(::println) }
                .getOrNull().orEmpty()
            if (res.isNotEmpty()) return GeocodeResult.Success(res)
        }

        return if (errors.isEmpty()) {
            GeocodeResult.Success(emptyList())
        } else {
            GeocodeResult.Failure(errors.joinToString(" / "))
        }
    }

    private suspend fun searchGoogle(query: String, apiKey: String): List<PlaceHit> {
        val res: GoogleTextSearchResponse = http.post("https://places.googleapis.com/v1/places:searchText") {
            contentType(ContentType.Application.Json)
            header("X-Goog-Api-Key", apiKey)
            header("X-Goog-FieldMask", "places.displayName,places.formattedAddress,places.location")
            setBody(GoogleTextSearchRequest(textQuery = query))
        }.body()
        return res.places.mapNotNull { p ->
            val loc = p.location ?: return@mapNotNull null
            PlaceHit(
                provider = "Google",
                name = p.displayName?.text ?: query,
                address = p.formattedAddress.orEmpty(),
                latitude = loc.latitude,
                longitude = loc.longitude,
            )
        }
    }

    /** 지번주소 등 구체적 쿼리를 점진 축약한 검색어 리스트. */
    private fun queryVariants(q: String): List<String> {
        val trimmed = q.trim()
        val out = linkedSetOf(trimmed)
        // Step 1: 번지 꼬리표 제거 ("산 10-33", "123-45", "142")
        val dropLot = trimmed.replace(Regex("""\s*산?\s*\d+(?:-\d+)?\s*$"""), "").trim()
        if (dropLot.isNotBlank() && dropLot != trimmed) out += dropLot
        // Step 2~: 마지막 행정구역 단위(리/동/읍/면)를 한 단계씩 제거
        var current = dropLot.ifBlank { trimmed }
        val tailAdmin = Regex("""\s+\S+(?:리|동|읍|면)\s*$""")
        repeat(3) {
            val reduced = current.replace(tailAdmin, "").trim()
            if (reduced.isBlank() || reduced == current) return@repeat
            out += reduced
            current = reduced
        }
        return out.toList()
    }

    private suspend fun searchKakao(query: String, key: String): List<PlaceHit> {
        val res: KakaoResponse = http.get("https://dapi.kakao.com/v2/local/search/keyword.json") {
            parameter("query", query)
            parameter("size", 5)
            header(HttpHeaders.Authorization, "KakaoAK $key")
        }.body()
        return res.documents.map {
            PlaceHit(
                provider = "Kakao",
                name = it.placeName,
                address = it.roadAddressName.ifBlank { it.addressName },
                latitude = it.y.toDoubleOrNull() ?: 0.0,
                longitude = it.x.toDoubleOrNull() ?: 0.0,
            )
        }
    }

    private suspend fun searchKakaoAddress(query: String, key: String): List<PlaceHit> {
        val res: KakaoResponse = http.get("https://dapi.kakao.com/v2/local/search/address.json") {
            parameter("query", query)
            parameter("size", 5)
            header(HttpHeaders.Authorization, "KakaoAK $key")
        }.body()
        return res.documents.map {
            PlaceHit(
                provider = "Kakao-Addr",
                name = it.roadAddressName.ifBlank { it.addressName },
                address = listOf(it.roadAddressName, it.addressName).filter { s -> s.isNotBlank() }.joinToString(" / "),
                latitude = it.y.toDoubleOrNull() ?: 0.0,
                longitude = it.x.toDoubleOrNull() ?: 0.0,
            )
        }
    }

    private suspend fun searchNominatim(query: String): List<PlaceHit> {
        val items: List<NominatimItem> = http.get("https://nominatim.openstreetmap.org/search") {
            parameter("q", query)
            parameter("format", "json")
            parameter("limit", 5)
            parameter("countrycodes", "kr")
            parameter("accept-language", "ko")
            header(HttpHeaders.UserAgent, "MateDash/1.0 (verification)")
        }.body()
        return items.map {
            PlaceHit(
                provider = "Nominatim",
                name = it.name ?: it.displayName.substringBefore(','),
                address = it.displayName,
                latitude = it.lat.toDoubleOrNull() ?: 0.0,
                longitude = it.lon.toDoubleOrNull() ?: 0.0,
            )
        }
    }

    private suspend fun searchPhoton(query: String): List<PlaceHit> {
        val res: PhotonResponse = http.get("https://photon.komoot.io/api/") {
            parameter("q", query)
            parameter("limit", 5)
            header(HttpHeaders.UserAgent, "MateDash/1.0 (verification)")
        }.body()
        return res.features.mapNotNull { f ->
            val coords = f.geometry.coordinates
            if (coords.size < 2) return@mapNotNull null
            val lon = coords[0]
            val lat = coords[1]
            val p = f.properties
            val addressParts = listOfNotNull(
                p.city,
                p.district,
                p.locality,
                listOfNotNull(p.street, p.housenumber).joinToString(" ").ifBlank { null },
                p.postcode,
            )
            PlaceHit(
                provider = "Photon",
                name = p.name ?: query,
                address = addressParts.joinToString(", "),
                latitude = lat,
                longitude = lon,
            )
        }
    }
}
