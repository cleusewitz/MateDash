package com.soooool.matedash.data.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * iTunes Search API로 곡 정보 → 앨범 아트 URL 조회.
 * 무료 / 인증 불필요 / Rate limit 분당 20회 (충분).
 */
class ItunesSearchClient {
    private val cache = mutableMapOf<String, String>() // "title|artist" -> artwork url

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 6_000
            connectTimeoutMillis = 4_000
        }
    }

    /** 검색 결과의 100x100 URL을 600x600으로 치환해 더 선명하게. 실패 시 빈 문자열 반환. */
    suspend fun findArtworkUrl(title: String, artist: String): String {
        if (title.isBlank()) return ""
        val key = "${title.lowercase()}|${artist.lowercase()}"
        cache[key]?.let { return it }
        return try {
            val term = listOf(title, artist).filter { it.isNotBlank() }
                .joinToString(" ") { it.trim() }
            val encoded = urlEncode(term)
            val url = "https://itunes.apple.com/search?term=$encoded&media=music&entity=song&limit=1"
            val resp: ItunesSearchResponse = httpClient.get(url).body()
            val art = resp.results.firstOrNull()?.artworkUrl100 ?: ""
            val hires = if (art.isNotBlank()) art.replace("100x100", "600x600") else ""
            if (hires.isNotBlank()) cache[key] = hires
            hires
        } catch (e: Exception) {
            println("[MateDash] iTunes search error: ${e.message}")
            ""
        }
    }

    private fun urlEncode(s: String): String {
        val sb = StringBuilder()
        for (b in s.encodeToByteArray()) {
            val c = b.toInt() and 0xFF
            when {
                c in 0x30..0x39 || c in 0x41..0x5A || c in 0x61..0x7A ||
                    c == 0x2D || c == 0x2E || c == 0x5F || c == 0x7E -> sb.append(c.toChar())
                c == 0x20 -> sb.append('+')
                else -> sb.append('%').append(c.toString(16).padStart(2, '0').uppercase())
            }
        }
        return sb.toString()
    }
}

@Serializable
private data class ItunesSearchResponse(
    val resultCount: Int = 0,
    val results: List<ItunesResult> = emptyList(),
)

@Serializable
private data class ItunesResult(
    @SerialName("artworkUrl100") val artworkUrl100: String = "",
    @SerialName("trackName") val trackName: String = "",
    @SerialName("artistName") val artistName: String = "",
)
